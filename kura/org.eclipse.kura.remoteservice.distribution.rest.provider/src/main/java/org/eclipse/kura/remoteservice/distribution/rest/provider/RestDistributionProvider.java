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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.aries.rsa.spi.DistributionProvider;
import org.apache.aries.rsa.spi.Endpoint;
import org.eclipse.kura.remoteservice.distribution.rest.provider.whiteboard.Constants;
import org.eclipse.kura.system.SystemService;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.remoteserviceadmin.EndpointDescription;

import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;

@Component
public class RestDistributionProvider implements DistributionProvider {

    public static final String CONFIG_TYPE = "kura.rest";

    private final ExportHandler exportHandler;
    private final ImportHandler importHandler;

    @Activate
    public RestDistributionProvider(final @Reference SystemService systemService,
            final @Reference(cardinality = ReferenceCardinality.MULTIPLE, //
                    policyOption = ReferencePolicyOption.GREEDY, //
                    target = "(" + Constants.KURA_REMOTESERVICES_EXTENSION_PROP_NAME + "=*)") //
            Collection<MessageBodyReader<?>> messageBodyReaders,
            final @Reference(cardinality = ReferenceCardinality.MULTIPLE, //
                    policyOption = ReferencePolicyOption.GREEDY, //
                    target = "(" + Constants.KURA_REMOTESERVICES_EXTENSION_PROP_NAME + "=*)") //
            Collection<MessageBodyWriter<?>> messageBodyWriters) {

        final List<Object> providers = new ArrayList<>();
        providers.addAll(messageBodyReaders);
        providers.addAll(messageBodyWriters);

        this.exportHandler = new ExportHandler(systemService);
        this.importHandler = new ImportHandler(providers);
    }

    @Override
    public Endpoint exportService(final Object service, final BundleContext serviceContext,
            Map<String, Object> effectiveProperties, final Class[] exportedInterfaces) {

        return exportHandler.exportService(service, serviceContext, effectiveProperties, exportedInterfaces);
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[] { CONFIG_TYPE };
    }

    @Override
    public Object importEndpoint(final ClassLoader cl, final BundleContext consumerContext, final Class[] interfaces,
            final EndpointDescription endpoint) {
        return this.importHandler.importEndpoint(cl, consumerContext, interfaces, endpoint);
    }

}
