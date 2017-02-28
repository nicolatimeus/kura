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

class Int8 extends BinaryData<Byte> {

    public Int8() {
        super(Endianness.BigEndian, 1);
    }

    @Override
    public void write(byte[] buf, int offset, Byte value) {
        buf[offset] = value;
    }

    @Override
    public Byte read(byte[] buf, int offset) {
        return buf[offset];
    }
}
