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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.aries.rsa.spi.Endpoint;
import org.eclipse.kura.remoteservice.distribution.rest.provider.whiteboard.Constants;
import org.eclipse.kura.system.SystemService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.jakartars.whiteboard.JakartarsWhiteboardConstants;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.dynamic.DynamicType.Unloaded;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.Pipe;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.matcher.ElementMatchers;

public class ExportHandler {

    private static final Logger logger = LoggerFactory.getLogger(ExportHandler.class);

    private final String hostname;
    private final BundleContext bundleContext;

    private final Map<Class<?>, DispatcherRegistration> dispatchers = new HashMap<>();

    public ExportHandler(final SystemService systemService) {
        this.hostname = systemService.getHostname().trim().replace("\n", "");
        this.bundleContext = FrameworkUtil.getBundle(ExportHandler.class).getBundleContext();
    }

    public synchronized Endpoint exportService(final Object service, final BundleContext serviceContext,
            Map<String, Object> effectiveProperties, final Class<?>[] exportedInterfaces) {

        Object id = effectiveProperties.get("kura.service.pid");

        if (!(id instanceof String)) {
            id = "$" + effectiveProperties.get(RemoteConstants.ENDPOINT_SERVICE_ID);
        }

        final String pid = (String) id;

        final List<Class<?>> interfaceList = Arrays.asList(exportedInterfaces);

        final Map<String, Object> endpointProperties = new HashMap<>(effectiveProperties);

        final String baseURL = "https://" + hostname + "/services/" + Constants.APPLICATION_BASE;
        final KuraEndpointId kuraId = new KuraEndpointId(baseURL, pid);

        endpointProperties.put(RemoteConstants.ENDPOINT_ID, kuraId.toJson());
        endpointProperties.put(RemoteConstants.SERVICE_IMPORTED_CONFIGS,
                new String[] { RestDistributionProvider.CONFIG_TYPE });

        final EndpointImpl result = new EndpointImpl(interfaceList, pid, new EndpointDescription(endpointProperties));

        for (final Class<?> serviceInterface : interfaceList) {
            try {
                getOrCreateDispatcher(serviceInterface).dispatcher.addService(pid, service);
            } catch (final Exception e) {
                logger.warn("failed to create or update dispatcher for interface: {} pid: {}", serviceInterface, pid,
                        e);
                result.close();
                return null;
            }
        }

        return result;
    }

    public DispatcherRegistration getOrCreateDispatcher(final Class<?> serviceInterface) {

        return this.dispatchers.compute(serviceInterface, (t, u) -> {
            if (u != null) {
                return u;
            }

            return buildInterfaceDispatcher(t);
        });

    }

    public static class Interceptor {

        private Interceptor() {
        }

        @RuntimeType
        public static Object intercept(@Pipe Function<Object, Object> pipe, @This Dispatcher dispatcher) {

            return pipe.apply(dispatcher.getDispatchTarget());
        }
    }

    public DispatcherRegistration buildInterfaceDispatcher(final Class<?> serviceInterface) {

        final ByteBuddy byteBuddy = new ByteBuddy(ClassFileVersion.JAVA_V8);
        final Unloaded<?> unloaded = byteBuddy.subclass(Dispatcher.class).implement(serviceInterface)
                .method(ElementMatchers.isDeclaredBy(serviceInterface))
                .intercept(MethodDelegation.withDefaultConfiguration().withBinders(Pipe.Binder.install(Function.class))
                        .to(Interceptor.class))
                .annotateType(AnnotationDescription.Builder.ofType(Path.class)
                        .define("value", serviceInterface.getName().replaceAll("[.]", "/") + "/{__pid}").build())
                .make();

        final ClassLoader classLoader = new ClassLoader(serviceInterface.getClassLoader()) {

            @Override
            protected Class<?> findClass(final String name) throws ClassNotFoundException {

                if (Path.class.getCanonicalName().equals(name)) {
                    return Path.class;
                }

                if (Dispatcher.class.getName().equals(name)) {
                    return Dispatcher.class;
                }

                if (Interceptor.class.getName().equals(name)) {
                    return Interceptor.class;
                }

                throw new ClassNotFoundException("cannot find " + name);
            }
        };

        final Class<?> wrapperClass = unloaded.load(classLoader).getLoaded();

        Dispatcher dispatcher;
        try {
            dispatcher = (Dispatcher) wrapperClass.getConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to crate dispatcher", e);
        }

        final Map<String, Object> whiteboardProperties = new HashMap<>();
        whiteboardProperties.put(JakartarsWhiteboardConstants.JAKARTA_RS_RESOURCE, true);
        whiteboardProperties.put(JakartarsWhiteboardConstants.JAKARTA_RS_APPLICATION_SELECT,
                "(" + JakartarsWhiteboardConstants.JAKARTA_RS_NAME + "=" + Constants.APPLICATION_NAME + ")");

        final ServiceRegistration<?> reg = bundleContext.registerService(Object.class, dispatcher,
                FrameworkUtil.asDictionary(whiteboardProperties));

        return new DispatcherRegistration(dispatcher, reg);
    }

    private static class DispatcherRegistration {

        private final Dispatcher dispatcher;
        private final ServiceRegistration<?> reg;

        public DispatcherRegistration(Dispatcher dispatcer, ServiceRegistration<?> reg) {
            this.dispatcher = dispatcer;
            this.reg = reg;
        }

    }

    public static class Dispatcher {

        @Context
        private UriInfo info;

        private final Map<String, Object> services = new HashMap<>();

        public void addService(final String pid, final Object o) {
            this.services.put(pid, o);
        }

        public void removeService(final String pid) {
            this.services.remove(pid);
        }

        public int serviceCount() {
            return services.size();
        }

        public Object getDispatchTarget() {
            final MultivaluedMap<String, String> params = info.getPathParameters();

            final List<String> p = params.get("__pid");

            if (p == null || p.isEmpty()) {
                throw new WebApplicationException(400);
            }

            final Object target = this.services.get(p.iterator().next());

            if (target == null) {
                throw new WebApplicationException(404);
            }

            return target;
        }

    }

    private synchronized void removeEndpoint(final EndpointImpl endpoint) {
        for (final Class<?> intf : endpoint.interfaces) {
            final DispatcherRegistration reg = this.dispatchers.get(intf);

            if (reg != null) {
                reg.dispatcher.removeService(endpoint.pid);

                if (reg.dispatcher.serviceCount() == 0) {
                    reg.reg.unregister();
                    this.dispatchers.remove(intf);
                }
            }
        }
    }

    private class EndpointImpl implements Endpoint {

        private final List<Class<?>> interfaces;
        private final String pid;
        private final EndpointDescription description;

        public EndpointImpl(List<Class<?>> interfaces, String pid, EndpointDescription description) {
            super();
            this.interfaces = interfaces;
            this.pid = pid;
            this.description = description;
        }

        @Override
        public void close() {
            removeEndpoint(this);
        }

        @Override
        public EndpointDescription description() {
            return this.description;
        }

    }

}
