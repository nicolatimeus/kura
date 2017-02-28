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

package org.eclipse.kura.internal.driver.binary;

public abstract class BinaryData<T> {

    protected final Endianness endianness;
    protected final int size;

    public BinaryData(Endianness endianness, int size) {
        this.endianness = endianness;
        this.size = size;
    }

    public Endianness getEndianness() {
        return endianness;
    }

    public int getSize() {
        return size;
    }

    public abstract void write(byte[] buf, int offset, T value);

    public abstract T read(byte[] buf, int offset);

}
