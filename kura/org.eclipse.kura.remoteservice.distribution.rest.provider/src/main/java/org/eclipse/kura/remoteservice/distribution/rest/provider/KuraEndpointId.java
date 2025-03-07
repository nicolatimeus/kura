package org.eclipse.kura.remoteservice.distribution.rest.provider;

import java.util.Optional;

import com.google.gson.Gson;

public class KuraEndpointId {

    private static final Gson GSON = new Gson();

    private final String baseURL;
    private final String id;

    public KuraEndpointId(final String baseURL, final String id) {
        this.baseURL = baseURL;
        this.id = id;
    }

    public String getBaseURL() {
        return baseURL;
    }

    public String getId() {
        return id;
    }

    public static Optional<KuraEndpointId> fromJson(final String value) {
        try {
            return Optional.of(GSON.fromJson(value, KuraEndpointId.class));
        } catch (final Exception e) {
            return Optional.empty();
        }
    }

    public String toJson() {
        return GSON.toJson(this);
    }
}
