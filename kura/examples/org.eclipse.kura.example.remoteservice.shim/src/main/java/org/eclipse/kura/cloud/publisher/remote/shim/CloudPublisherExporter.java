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
package org.eclipse.kura.cloud.publisher.remote.shim;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.cloud.publisher.remote.PublisherApi;
import org.eclipse.kura.cloudconnection.publisher.CloudPublisher;
import org.eclipse.kura.configuration.ConfigurationService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true)
public class CloudPublisherExporter {

    static final Logger logger = LoggerFactory.getLogger(CloudPublisherExporter.class);

    private final Map<String, ServiceRegistration<PublisherApi>> registrations = new HashMap<>();
    private final BundleContext bundleContext;

    @Activate
    public CloudPublisherExporter(final ComponentContext context) {
        this.bundleContext = context.getBundleContext();
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, target = "(!("
            + RemoteConstants.SERVICE_IMPORTED + "=*))")
    public void setCloudPublisher(final CloudPublisher publisher, final Map<String, Object> properties) {
        final Object kuraServicePid = properties.get(ConfigurationService.KURA_SERVICE_PID);

        if (!(kuraServicePid instanceof String)) {
            logger.warn("ignoring cloudpublisher with invalid {}", ConfigurationService.KURA_SERVICE_PID);
            return;
        }

        final Map<String, Object> registrationProperties = new HashMap<>();
        registrationProperties.put(ConfigurationService.KURA_SERVICE_PID, kuraServicePid);
        registrationProperties.put(RemoteConstants.SERVICE_EXPORTED_INTERFACES, "*");

        registrations.put((String) kuraServicePid, bundleContext.registerService(PublisherApi.class,
                new CloudPublisherWrapper(publisher), FrameworkUtil.asDictionary(registrationProperties)));

    }

    public void unsetCloudPublisher(final Map<String, Object> properties) {
        final Object kuraServicePid = properties.get(ConfigurationService.KURA_SERVICE_PID);

        if (!(kuraServicePid instanceof String)) {
            logger.warn("ignoring cloudpublisher with invalid {}", ConfigurationService.KURA_SERVICE_PID);
            return;
        }

        final ServiceRegistration<?> reg = registrations.remove(kuraServicePid);

        if (reg != null) {
            reg.unregister();
        }
    }

}
