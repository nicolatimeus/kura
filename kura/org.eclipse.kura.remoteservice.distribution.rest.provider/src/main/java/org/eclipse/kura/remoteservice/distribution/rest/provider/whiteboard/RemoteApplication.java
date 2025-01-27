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
package org.eclipse.kura.remoteservice.distribution.rest.provider.whiteboard;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.jakartars.whiteboard.JakartarsWhiteboardConstants;

import jakarta.ws.rs.core.Application;

@Component(property = { JakartarsWhiteboardConstants.JAKARTA_RS_APPLICATION_BASE + "=" + Constants.APPLICATION_BASE,
        JakartarsWhiteboardConstants.JAKARTA_RS_NAME + "=" + Constants.APPLICATION_NAME }, service = {
                Application.class })
public class RemoteApplication extends Application {
}
