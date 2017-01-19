/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.cloud.aws;

import org.eclipse.kura.KuraException;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

public interface DeviceShadowService {

    public void updateReportedShadow(JsonObject reportedShadow) throws KuraException;

    public JsonValue getShadow();

}
