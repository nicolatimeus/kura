package org.eclipse.kura.example.remoteservice.api;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface TestService {

    public void ping();

    public int sum(int a, int b);
}
