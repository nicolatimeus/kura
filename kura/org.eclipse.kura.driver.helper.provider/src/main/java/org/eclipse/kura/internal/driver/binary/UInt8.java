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

class UInt8 extends BinaryData<Short> {

    public UInt8() {
        super(Endianness.BigEndian, 1);
    }

    @Override
    public void write(byte[] buf, int offset, Short value) {
        buf[offset] = (byte) (value & 0xff);
    }

    @Override
    public Short read(byte[] buf, int offset) {
        return (short) (buf[offset] & 0xff);
    }
}
