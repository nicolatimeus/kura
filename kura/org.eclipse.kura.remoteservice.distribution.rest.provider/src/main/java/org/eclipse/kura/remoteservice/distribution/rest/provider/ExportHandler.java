/*******************************************************************************
 * Copyright (c) 2025 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.remoteservice.distribution.rest.provider;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.apache.aries.rsa.spi.Endpoint;
import org.eclipse.kura.system.SystemService;
import org.objectweb.asm.Opcodes;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.jakartars.whiteboard.JakartarsWhiteboardConstants;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.Path;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.dynamic.DynamicType.Unloaded;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;

public class ExportHandler {

    private static final String WRAPPED_FIELD_NAME = "wrapped";
    private static final Logger logger = LoggerFactory.getLogger(ExportHandler.class);

    private final String hostname;
    private final BundleContext bundleContext;

    public ExportHandler(final SystemService systemService) {
        this.hostname = systemService.getHostname().trim().replace("\n", "");
        this.bundleContext = FrameworkUtil.getBundle(ExportHandler.class).getBundleContext();
    }

    public Endpoint exportService(final Object service, final BundleContext serviceContext,
            Map<String, Object> effectiveProperties, final Class[] exportedInterfaces) {

        final Object kuraServicePid = effectiveProperties.get("kura.service.pid");

        if (!(kuraServicePid instanceof String)) {
            logger.warn("remote service {} does not set kura.service.pid property, ignoring", service);
            return null;
        }

        final String relativePath = "/remote/" + kuraServicePid;

        final Object wrapper;

        try {
            wrapper = buildWrapper(service, relativePath, exportedInterfaces);
        } catch (final Exception e) {
            logger.warn("failed to define wrapper for {}", service, e);
            return null;
        }

        final Map<String, Object> whiteboardProperties = new HashMap<>();
        whiteboardProperties.put(JakartarsWhiteboardConstants.JAKARTA_RS_RESOURCE, true);

        final ServiceRegistration<?> reg = bundleContext.registerService(Object.class, wrapper,
                FrameworkUtil.asDictionary(whiteboardProperties));

        final Map<String, Object> endpointProperties = new HashMap<>(effectiveProperties);
        endpointProperties.put(RemoteConstants.ENDPOINT_ID, "https://" + hostname + "/services" + relativePath);
        endpointProperties.put(RemoteConstants.SERVICE_IMPORTED_CONFIGS,
                new String[] { RestDistributionProvider.CONFIG_TYPE });

        return new EndpointImpl(reg, new EndpointDescription(endpointProperties));
    }

    private Object buildWrapper(final Object service, final String relativePath, final Class[] exportedInterfaces)
            throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException,
            NoSuchFieldException {

        final ByteBuddy byteBuddy = new ByteBuddy(ClassFileVersion.JAVA_V8);
        Builder<?> builder = byteBuddy.subclass(Object.class);

        for (final Class<?> intf : exportedInterfaces) {
            builder = builder.implement(intf);
        }

        for (final Class<?> intf : exportedInterfaces) {
            builder = builder.method(ElementMatchers.isDeclaredBy(intf))
                    .intercept(MethodDelegation.toField(WRAPPED_FIELD_NAME));
        }

        final Unloaded<?> unloaded = builder
                .annotateType(AnnotationDescription.Builder.ofType(Path.class).define("value", relativePath).build())
                .defineField(WRAPPED_FIELD_NAME, service.getClass(), Opcodes.ACC_PUBLIC).make();

        final ClassLoader classLoader = new ClassLoader(service.getClass().getClassLoader()) {

            @Override
            protected Class<?> findClass(final String name) throws ClassNotFoundException {

                if (Path.class.getCanonicalName().equals(name)) {
                    return Path.class;
                }

                throw new ClassNotFoundException("cannot find " + name);
            }
        };

        final Class<?> wrapperClass = unloaded.load(classLoader).getLoaded();

        final Object wrapper = wrapperClass.getConstructor().newInstance();
        wrapperClass.getDeclaredField(WRAPPED_FIELD_NAME).set(wrapper, service);

        return wrapper;
    }

    private static class EndpointImpl implements Endpoint {

        private final ServiceRegistration<?> registration;
        private final EndpointDescription description;

        public EndpointImpl(ServiceRegistration<?> registration, final EndpointDescription description) {
            this.registration = registration;
            this.description = description;
        }

        @Override
        public void close() throws IOException {
            this.registration.unregister();
        }

        @Override
        public EndpointDescription description() {
            return this.description;
        }

    }

}
