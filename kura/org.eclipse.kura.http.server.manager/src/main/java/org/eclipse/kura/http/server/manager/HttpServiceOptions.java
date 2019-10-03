/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *
 *******************************************************************************/
package org.eclipse.kura.http.server.manager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;

public class HttpServiceOptions {

	private static final Logger logger = LoggerFactory.getLogger(HttpServiceOptions.class);
	
    static final String PROP_HTTP_ENABLED = "http.enabled";
    static final String PROP_HTTP_PORT = "http.port";
    static final String PROP_HTTPS_ENABLED = "https.enabled";
    static final String PROP_HTTPS_PORT = "https.port";
    static final String PROP_HTTPS_KEYSTORE_PATH = "https.keystore.path";
    static final String PROP_HTTPS_KEYSTORE_PASSWORD = "https.keystore.password";

    static final String DEFAULT_HTTPS_KEYSTORE_PASSWORD = "changeit";
    
    static final String PROP_REVOCATION_ENABLED = "https.revocation.check.enabled";
    static final String PROP_REVOCATION_LISTS = "https.client.revocation.urls";

    private static final Property<Boolean> HTTP_ENABLED = new Property<>(PROP_HTTP_ENABLED, true);
    private static final Property<Integer> HTTP_PORT = new Property<>(PROP_HTTP_PORT, 80);
    private static final Property<Boolean> HTTPS_ENABLED = new Property<>(PROP_HTTPS_ENABLED, false);
    private static final Property<Integer> HTTPS_PORT = new Property<>(PROP_HTTPS_PORT, 443);
    private static final Property<String> HTTPS_KEYSTORE_PASSWORD = new Property<>(PROP_HTTPS_KEYSTORE_PASSWORD,
            DEFAULT_HTTPS_KEYSTORE_PASSWORD);
    private static final Property<Boolean> REVOCATION_ENABLED = new Property<>(PROP_REVOCATION_ENABLED, true);
    private static final Property<String> REVOCATION_LISTS = new Property<>(PROP_REVOCATION_LISTS, "[]");

    private final boolean httpEnabled;
    private final int httpPort;
    private final boolean httpsEnabled;
    private final int httpsPort;
    private final String httpsKeystorePath;
    private final char[] httpsKeystorePasswordArray;
    private final boolean isRevocationEnabled;
    private final List<String> certificateRevocationUris;
    
    public HttpServiceOptions(final Map<String, Object> properties, final String kuraHome) {
        Property<String> httpsKeystorePathProp = new Property<>(PROP_HTTPS_KEYSTORE_PATH,
                kuraHome + "/user/security/httpskeystore.ks");
        
        
        this.httpEnabled = HTTP_ENABLED.get(properties);
        this.httpPort = HTTP_PORT.get(properties);
        this.httpsEnabled = HTTPS_ENABLED.get(properties);
        this.httpsPort = HTTPS_PORT.get(properties);
        this.httpsKeystorePath = httpsKeystorePathProp.get(properties);
        this.httpsKeystorePasswordArray = HTTPS_KEYSTORE_PASSWORD.get(properties).toCharArray();
        this.isRevocationEnabled = REVOCATION_ENABLED.get(properties);
        this.certificateRevocationUris = parseRevocationURIs(REVOCATION_LISTS.get(properties));
    }

    public boolean isHttpEnabled() {
        return this.httpEnabled;
    }

    public int getHttpPort() {
        return this.httpPort;
    }

    public boolean isHttpsEnabled() {
        return this.httpsEnabled;
    }

    public int getHttpsPort() {
        return this.httpsPort;
    }

    public String getHttpsKeystorePath() {
        return this.httpsKeystorePath;
    }

    public char[] getHttpsKeystorePassword() {
        return this.httpsKeystorePasswordArray;
    }
    
    public boolean isRevocationEnabled() {
		return isRevocationEnabled;
	} 
    
    public List<String> getRevocationURIs() {
    	return certificateRevocationUris;
    }

    private static List<String> parseRevocationURIs(final String revocationURIs) {
    	try {
    		final List<String> result = new ArrayList<>();
    		
    		for (final JsonValue value : Json.parse(revocationURIs).asArray()) {
    			result.add(value.asString());
    		}
    		
    		return result;
    	} catch (final Exception e) {
    		logger.warn("failed to parse revocation list", e);
    		return Collections.emptyList();
    	}
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.httpEnabled ? 1231 : 1237);
        result = prime * result + this.httpPort;
        result = prime * result + (this.httpsEnabled ? 1231 : 1237);
        result = prime * result + Arrays.hashCode(this.httpsKeystorePasswordArray);
        result = prime * result + (this.httpsKeystorePath == null ? 0 : this.httpsKeystorePath.hashCode());
        result = prime * result + this.httpsPort;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        HttpServiceOptions other = (HttpServiceOptions) obj;
        if (this.httpEnabled != other.httpEnabled) {
            return false;
        }
        if (this.httpPort != other.httpPort) {
            return false;
        }
        if (this.httpsEnabled != other.httpsEnabled) {
            return false;
        }
        if (!Arrays.equals(this.httpsKeystorePasswordArray, other.httpsKeystorePasswordArray)) {
            return false;
        }
        if (this.httpsKeystorePath == null) {
            if (other.httpsKeystorePath != null) {
                return false;
            }
        } else if (!this.httpsKeystorePath.equals(other.httpsKeystorePath)) {
            return false;
        }
        
        boolean result = true;
        if (this.httpsPort != other.httpsPort) {
            result = false;
        }
        return result;
    }

}
