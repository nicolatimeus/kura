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

package org.eclipse.kura.internal.driver.aggregator;

import java.io.IOException;

public abstract class BlockTask {

    private int start;
    private int end;
    private Mode mode;

    public BlockTask(int start, int end, Mode mode) {
        this.start = start;
        this.end = end;
        this.setMode(mode);
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public abstract void run(ToplevelBlockTask parent) throws IOException;

    public abstract void fail(ToplevelBlockTask parent, String reason);

    public abstract void succeed(ToplevelBlockTask parent);

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getClass().getName()).append(" ").append("[ ").append(start).append(", ").append(end)
                .append("]");

        return builder.toString();
    }
}
