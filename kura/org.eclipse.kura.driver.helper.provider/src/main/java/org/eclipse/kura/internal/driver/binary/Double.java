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

class Double extends BinaryData<java.lang.Double> {

    public Double(Endianness endianness) {
        super(endianness, 8);
    }

    @Override
    public void write(byte[] buf, int offset, java.lang.Double d) {
        long tmp = java.lang.Double.doubleToRawLongBits(d);
        if (endianness == Endianness.BigEndian) {
            buf[offset] = (byte) ((tmp >> 56) & 0xff);
            buf[offset + 1] = (byte) ((tmp >> 48) & 0xff);
            buf[offset + 2] = (byte) ((tmp >> 40) & 0xff);
            buf[offset + 3] = (byte) ((tmp >> 32) & 0xff);
            buf[offset + 4] = (byte) ((tmp >> 24) & 0xff);
            buf[offset + 5] = (byte) ((tmp >> 16) & 0xff);
            buf[offset + 6] = (byte) ((tmp >> 8) & 0xff);
            buf[offset + 7] = (byte) (tmp & 0xff);
        } else {
            buf[offset] = (byte) (tmp & 0xff);
            buf[offset + 1] = (byte) ((tmp >> 8) & 0xff);
            buf[offset + 2] = (byte) ((tmp >> 16) & 0xff);
            buf[offset + 3] = (byte) ((tmp >> 24) & 0xff);
            buf[offset + 4] = (byte) ((tmp >> 32) & 0xff);
            buf[offset + 5] = (byte) ((tmp >> 40) & 0xff);
            buf[offset + 6] = (byte) ((tmp >> 48) & 0xff);
            buf[offset + 7] = (byte) ((tmp >> 56) & 0xff);
        }
    }

    @Override
    public java.lang.Double read(byte[] buf, int offset) {
        long tmp;
        if (endianness == Endianness.BigEndian) {
            tmp = buf[offset + 7] & 0xffL;
            tmp |= (buf[offset + 6] & 0xffL) << 8;
            tmp |= (buf[offset + 5] & 0xffL) << 16;
            tmp |= (buf[offset + 4] & 0xffL) << 24;
            tmp |= (buf[offset + 3] & 0xffL) << 32;
            tmp |= (buf[offset + 2] & 0xffL) << 40;
            tmp |= (buf[offset + 1] & 0xffL) << 48;
            tmp |= (buf[offset] & 0xffL) << 56;
        } else {
            tmp = buf[offset] & 0xffL;
            tmp |= (buf[offset + 1] & 0xffL) << 8;
            tmp |= (buf[offset + 2] & 0xffL) << 16;
            tmp |= (buf[offset + 3] & 0xffL) << 24;
            tmp |= (buf[offset + 4] & 0xffL) << 32;
            tmp |= (buf[offset + 5] & 0xffL) << 40;
            tmp |= (buf[offset + 6] & 0xffL) << 48;
            tmp |= (buf[offset + 7] & 0xffL) << 56;
        }
        return java.lang.Double.longBitsToDouble(tmp);
    }
}