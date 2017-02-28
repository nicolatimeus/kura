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

class UInt16 extends BinaryData<Integer> {

    public UInt16(Endianness endianness) {
        super(endianness, 2);
    }

    @Override
    public void write(byte[] buf, int offset, Integer value) {
        if (endianness == Endianness.BigEndian) {
            buf[offset] = (byte) ((value >> 8) & 0xff);
            buf[offset + 1] = (byte) (value & 0xff);
        } else {
            buf[offset] = (byte) (value & 0xff);
            buf[offset + 1] = (byte) ((value >> 8) & 0xff);
        }
    }

    @Override
    public Integer read(byte[] buf, int offset) {
        int result;
        if (endianness == Endianness.BigEndian) {
            result = buf[offset + 1] & 0xff;
            result |= (buf[offset] & 0xff) << 8;
        } else {
            result = buf[offset] & 0xff;
            result |= (buf[offset + 1] & 0xff) << 8;
        }
        return result;
    }
}
