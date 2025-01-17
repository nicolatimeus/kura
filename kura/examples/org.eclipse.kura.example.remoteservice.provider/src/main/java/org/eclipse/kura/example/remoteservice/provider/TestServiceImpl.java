package org.eclipse.kura.example.remoteservice.provider;

import org.eclipse.kura.example.remoteservice.api.TestService;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(property = { "service.exported.interfaces=*" })
public class TestServiceImpl implements TestService {

    private static final Logger logger = LoggerFactory.getLogger(TestServiceImpl.class);

    @Override
    public void ping() {
        logger.info("Ping called");

    }

    @Override
    public int sum(int a, int b) {
        logger.info("{} + {} = {}", a, b, a + b);

        return a + b;
    }

}
