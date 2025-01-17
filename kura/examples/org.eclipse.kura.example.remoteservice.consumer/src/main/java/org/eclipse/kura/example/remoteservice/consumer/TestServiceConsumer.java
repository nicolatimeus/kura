package org.eclipse.kura.example.remoteservice.consumer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.example.remoteservice.api.TestService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class TestServiceConsumer {

    private static final Logger logger = LoggerFactory.getLogger(TestServiceConsumer.class);

    private final TestService testService;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> future;

    @Activate
    public TestServiceConsumer(final @Reference TestService testService) {
        this.testService = testService;

        logger.info("TestService consumer activated");

        testService.sum(10, 20);

        this.future = executor.scheduleWithFixedDelay(this::doPing, 0, 10, TimeUnit.SECONDS);
    }

    private void doPing() {
        try {
            logger.info("sending ping...");
            testService.ping();
            logger.info("sending ping...done");
        } catch (final Exception e) {
            logger.warn("failed to send ping", e);
        }
    }

    @Deactivate
    public void deactivate() {
        logger.info("TestService consumer deactivating...");

        try {
            future.cancel(false);
            executor.shutdown();
            executor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        logger.info("TestService consumer deactivating...done");
    }

}
