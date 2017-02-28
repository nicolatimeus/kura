/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Eurotech
 *******************************************************************************/

package org.eclipse.kura.internal.driver.s7plc.task;

import org.eclipse.kura.internal.driver.aggregator.Mode;
import org.eclipse.kura.internal.driver.aggregator.ToplevelBlockTask;
import org.eclipse.kura.internal.driver.aggregator.ToplevelBlockTaskFactory;
import org.eclipse.kura.internal.driver.s7plc.S7PlcDriver;

public class S7PlcToplevelBlockFactory implements ToplevelBlockTaskFactory {

    private S7PlcDriver driver;
    private int areaNo;

    public S7PlcToplevelBlockFactory(S7PlcDriver driver) {
        this.driver = driver;
    }

    public void setAreaNo(int areaNo) {
        this.areaNo = areaNo;
    }

    @Override
    public ToplevelBlockTask build(int start, int end, Mode mode) {
        return new S7PlcToplevelBlockTask(driver, mode, areaNo, start, end);
    }
}