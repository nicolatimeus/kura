package org.eclipse.kura.internal.rest.provider;

import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class CompatPublisher implements ServiceTrackerCustomizer<Object, ServiceRegistration<?>> {

    private final BundleContext bundleContext;

    public CompatPublisher(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    public ServiceRegistration<?> addingService(final ServiceReference<Object> reference) {
        final Object service = this.bundleContext.getService(reference);

        if (service == null) {
            return null;
        }

        if (reference.getProperty("osgi.jaxrs.resource") != null
                || reference.getProperty("osgi.jaxrs.extension") != null) {
            return null;
        }

        final Class<?> clazz = service.getClass();

        if (clazz.isAnnotationPresent(Path.class)) {
            return registerResource(service);
        } else if (clazz.isAnnotationPresent(Provider.class)) {
            return registerProvider(service);
        }

        bundleContext.ungetService(reference);

        return null;
    }

    @Override
    public void modifiedService(final ServiceReference<Object> reference, final ServiceRegistration<?> registration) {
        // do nothing

    }

    @Override
    public void removedService(final ServiceReference<Object> reference, final ServiceRegistration<?> registration) {
        registration.unregister();
        bundleContext.ungetService(reference);
    }

    private ServiceRegistration<?> registerResource(final Object object) {
        return bundleContext.registerService(object.getClass().getName(), object,
                RestServiceUtils.resourceProperties());
    }

    private ServiceRegistration<?> registerProvider(final Object object) {
        return bundleContext.registerService(object.getClass().getName(), object,
                RestServiceUtils.extensionProperties());
    }

}
