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

public final class BinaryDataTypes {

    public static final BinaryData<Short> UINT8 = new UInt8();
    public static final BinaryData<Byte> INT8 = new Int8();

    public static final BinaryData<Integer> UINT16_LE = new UInt16(Endianness.LittleEndian);
    public static final BinaryData<Integer> UINT16_BE = new UInt16(Endianness.BigEndian);

    public static final BinaryData<Short> INT16_LE = new Int16(Endianness.LittleEndian);
    public static final BinaryData<Short> INT16_BE = new Int16(Endianness.BigEndian);

    public static final BinaryData<Long> UINT32_LE = new UInt32(Endianness.LittleEndian);
    public static final BinaryData<Long> UINT32_BE = new UInt32(Endianness.BigEndian);

    public static final BinaryData<Integer> INT32_LE = new Int32(Endianness.LittleEndian);
    public static final BinaryData<Integer> INT32_BE = new Int32(Endianness.BigEndian);

    public static final BinaryData<Long> INT64_LE = new Int64(Endianness.LittleEndian);
    public static final BinaryData<Long> INT64_BE = new Int64(Endianness.BigEndian);

    public static final BinaryData<java.lang.Float> FLOAT_LE = new Float(Endianness.LittleEndian);
    public static final BinaryData<java.lang.Float> FLOAT_BE = new Float(Endianness.BigEndian);

    public static final BinaryData<java.lang.Double> DOUBLE_LE = new Double(Endianness.LittleEndian);
    public static final BinaryData<java.lang.Double> DOUBLE_BE = new Double(Endianness.BigEndian);

    private BinaryDataTypes() {
    }
}
