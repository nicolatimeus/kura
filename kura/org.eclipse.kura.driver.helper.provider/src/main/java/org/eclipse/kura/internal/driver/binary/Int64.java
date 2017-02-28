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

class Int64 extends BinaryData<Long> {

    public Int64(Endianness endianness) {
        super(endianness, 8);
    }

    @Override
    public void write(byte[] buf, int offset, Long value) {
        if (endianness == Endianness.BigEndian) {
            buf[offset] = (byte) ((value >> 56) & 0xff);
            buf[offset + 1] = (byte) ((value >> 48) & 0xff);
            buf[offset + 2] = (byte) ((value >> 40) & 0xff);
            buf[offset + 3] = (byte) ((value >> 32) & 0xff);
            buf[offset + 4] = (byte) ((value >> 24) & 0xff);
            buf[offset + 5] = (byte) ((value >> 16) & 0xff);
            buf[offset + 6] = (byte) ((value >> 8) & 0xff);
            buf[offset + 7] = (byte) (value & 0xff);
        } else {
            buf[offset] = (byte) (value & 0xff);
            buf[offset + 1] = (byte) ((value >> 8) & 0xff);
            buf[offset + 2] = (byte) ((value >> 16) & 0xff);
            buf[offset + 3] = (byte) ((value >> 24) & 0xff);
            buf[offset + 4] = (byte) ((value >> 32) & 0xff);
            buf[offset + 5] = (byte) ((value >> 40) & 0xff);
            buf[offset + 6] = (byte) ((value >> 48) & 0xff);
            buf[offset + 7] = (byte) ((value >> 56) & 0xff);
        }
    }

    @Override
    public Long read(byte[] buf, int offset) {
        long result;
        if (endianness == Endianness.BigEndian) {
            result = buf[offset + 7] & 0xffL;
            result |= (buf[offset + 6] & 0xffL) << 8;
            result |= (buf[offset + 5] & 0xffL) << 16;
            result |= (buf[offset + 4] & 0xffL) << 24;
            result |= (buf[offset + 3] & 0xffL) << 32;
            result |= (buf[offset + 2] & 0xffL) << 40;
            result |= (buf[offset + 1] & 0xffL) << 48;
            result |= (buf[offset] & 0xffL) << 56;
        } else {
            result = buf[offset] & 0xffL;
            result |= (buf[offset + 1] & 0xffL) << 8;
            result |= (buf[offset + 2] & 0xffL) << 16;
            result |= (buf[offset + 3] & 0xffL) << 24;
            result |= (buf[offset + 4] & 0xffL) << 32;
            result |= (buf[offset + 5] & 0xffL) << 40;
            result |= (buf[offset + 6] & 0xffL) << 48;
            result |= (buf[offset + 7] & 0xffL) << 56;
        }
        return result;
    }
}