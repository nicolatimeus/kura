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
import java.util.LinkedList;
import java.util.List;

public abstract class ToplevelBlockTask extends BlockTask {

    private LinkedList<BlockTask> children = new LinkedList<>();
    private boolean isAborted;

    public ToplevelBlockTask(int start, int end, Mode mode) {
        super(start, end, mode);
    }

    public void clear() {
        children.clear();
    }

    public List<BlockTask> getChildren() {
        return children;
    }

    protected void runChildren() throws IOException {
        isAborted = false;
        for (BlockTask child : getChildren()) {
            child.run(this);
        }
    }

    protected void failChildren(String reason) {
        for (BlockTask child : getChildren()) {
            if (isAborted) {
                break;
            }
            child.fail(this, reason);
        }
    }

    protected void succeedChildren() {
        for (BlockTask child : getChildren()) {
            child.succeed(this);
        }
    }

    public void addChild(BlockTask child) {
        children.add(child);
    }

    public void abort(String reason) {
        this.isAborted = true;
        failChildren(reason);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(super.toString());

        if (!children.isEmpty()) {
            builder.append(" children: ");
            for (BlockTask b : getChildren()) {
                builder.append(b.toString());
                builder.append(' ');
            }
        }

        return builder.toString();
    }
}
