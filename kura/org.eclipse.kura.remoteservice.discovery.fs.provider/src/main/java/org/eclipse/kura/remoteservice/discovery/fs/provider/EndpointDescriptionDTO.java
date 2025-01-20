package org.eclipse.kura.remoteservice.discovery.fs.provider;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.eclipse.kura.rest.configuration.api.PropertyDTO;
import org.osgi.service.remoteserviceadmin.EndpointDescription;

public class EndpointDescriptionDTO {

    private final Map<String, PropertyDTO> properties;

    public EndpointDescriptionDTO(final EndpointDescription description) {
        final Map<String, PropertyDTO> result = new HashMap<>();

        for (final Entry<String, Object> e : description.getProperties().entrySet()) {
            if (e.getKey().startsWith("service.exported.") || e.getKey().equals("service.imported")) {
                continue;
            }

            final Optional<PropertyDTO> dto = PropertyDTO.fromConfigurationProperty(e.getValue());

            if (dto.isPresent()) {
                result.put(e.getKey(), dto.get());
            }

        }

        this.properties = result;
    }

    public EndpointDescription getEndpointDescription(final boolean imported) {

        final Map<String, Object> result = new HashMap<>();

        for (final Entry<String, PropertyDTO> e : this.properties.entrySet()) {
            if (e.getKey().startsWith("service.exported.")) {
                continue;
            }

            final Optional<Object> prop = e.getValue().toConfigurationProperty();

            if (prop.isPresent()) {
                result.put(e.getKey(), prop.get());
            }
        }

        if (imported) {
            result.put("service.imported", true);
        }

        return new EndpointDescription(result);
    }

}
