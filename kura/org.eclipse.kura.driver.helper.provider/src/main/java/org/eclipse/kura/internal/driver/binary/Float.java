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

class Float extends BinaryData<java.lang.Float> {

    public Float(Endianness endianness) {
        super(endianness, 4);
    }

    @Override
    public void write(byte[] buf, int offset, java.lang.Float f) {
        int tmp = java.lang.Float.floatToRawIntBits(f);
        if (endianness == Endianness.BigEndian) {
            buf[offset] = (byte) ((tmp >> 24) & 0xff);
            buf[offset + 1] = (byte) ((tmp >> 16) & 0xff);
            buf[offset + 2] = (byte) ((tmp >> 8) & 0xff);
            buf[offset + 3] = (byte) (tmp & 0xff);
        } else {
            buf[offset] = (byte) (tmp & 0xff);
            buf[offset + 1] = (byte) ((tmp >> 8) & 0xff);
            buf[offset + 2] = (byte) ((tmp >> 16) & 0xff);
            buf[offset + 3] = (byte) ((tmp >> 24) & 0xff);
        }
    }

    @Override
    public java.lang.Float read(byte[] buf, int offset) {
        int tmp;
        if (endianness == Endianness.BigEndian) {
            tmp = buf[offset + 3] & 0xff;
            tmp |= (buf[offset + 2] & 0xff) << 8;
            tmp |= (buf[offset + 1] & 0xff) << 16;
            tmp |= (buf[offset] & 0xff) << 24;
        } else {
            tmp = buf[offset] & 0xff;
            tmp |= (buf[offset + 1] & 0xff) << 8;
            tmp |= (buf[offset + 2] & 0xff) << 16;
            tmp |= (buf[offset + 3] & 0xff) << 24;
        }
        return java.lang.Float.intBitsToFloat(tmp);
    }
}
