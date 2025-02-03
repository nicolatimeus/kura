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
package org.eclipse.kura.example.remoteservice.provider;

import org.eclipse.kura.example.remoteservice.api.TestServiceApi;
import org.eclipse.kura.example.remoteservice.api.model.SumRequest;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(property = { "service.exported.interfaces=*", "kura.service.pid=TestServiceImpl" })
public class TestServiceImpl implements TestServiceApi {

    private static final Logger logger = LoggerFactory.getLogger(TestServiceImpl.class);

    @Override
    public String ping() {
        logger.info("Ping called");

        return "\"pong\"";
    }

    @Override
    public Integer sum(final SumRequest req) {
        final int result = req.getA() + req.getB();

        logger.info("{} + {} = {}", req.getA(), req.getB(), result);

        return result;
    }

}
