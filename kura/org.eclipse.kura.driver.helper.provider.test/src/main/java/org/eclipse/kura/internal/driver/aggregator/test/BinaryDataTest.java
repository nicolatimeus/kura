package org.eclipse.kura.internal.driver.aggregator.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.eclipse.kura.internal.driver.binary.BinaryDataTypes;
import org.junit.Test;

public class BinaryDataTest {

    @Test
    public void shouldSupportUInt8Read() {
        byte[] b = { (byte) 0xff };
        assertEquals((Short) (short) 255, BinaryDataTypes.UINT8.read(b, 0));
    }

    @Test
    public void shouldSupportInt8Read() {
        byte[] b = { (byte) 0xff };
        assertEquals((Byte) ((byte) (-1)), BinaryDataTypes.INT8.read(b, 0));
    }

    @Test
    public void shouldSupportUInt8Write() {
        byte[] b = { 0 };
        BinaryDataTypes.UINT8.write(b, 0, (short) 255);
        assertEquals((byte) 0xff, b[0]);
    }

    @Test
    public void shouldSupportInt8Write() {
        byte[] b = { 0 };
        BinaryDataTypes.INT8.write(b, 0, (byte) -1);
        assertEquals(-1, b[0]);
    }

    @Test
    public void shouldSupportUInt16LeRead() {
        byte[] b = { (byte) 0xa3, (byte) 0xc4 };
        assertEquals((Integer) 0xc4a3, BinaryDataTypes.UINT16_LE.read(b, 0));
    }

    @Test
    public void shouldSupportUInt16BeRead() {
        byte[] b = { (byte) 0xa3, (byte) 0xc4 };
        assertEquals((Integer) 0xa3c4, BinaryDataTypes.UINT16_BE.read(b, 0));
    }

    @Test
    public void shouldSupportUInt32LeRead() {
        byte[] b = { (byte) 0xa3, (byte) 0xc4, (byte) 0x45, (byte) 0x83 };
        assertEquals((Long) (long) 0x8345c4a3, BinaryDataTypes.UINT32_LE.read(b, 0));
    }

    @Test
    public void shouldSupportUInt32BeRead() {
        byte[] b = { (byte) 0xa3, (byte) 0xc4, (byte) 0x45, (byte) 0x83 };
        assertEquals((Long) (long) 0xa3c44583, BinaryDataTypes.UINT32_BE.read(b, 0));
    }

    @Test
    public void shouldSupportInt16LeRead() {
        byte[] b = { (byte) 0xa3, (byte) 0xc4 };
        assertEquals((Short) (short) 0xc4a3, BinaryDataTypes.INT16_LE.read(b, 0));
    }

    @Test
    public void shouldSupportInt16BeRead() {
        byte[] b = { (byte) 0xa3, (byte) 0xc4 };
        assertEquals((Short) (short) 0xa3c4, BinaryDataTypes.INT16_BE.read(b, 0));
    }

    @Test
    public void shouldSupportInt32LeRead() {
        byte[] b = { (byte) 0xa3, (byte) 0xc4, (byte) 0x45, (byte) 0x83 };
        assertEquals((Integer) 0x8345c4a3, BinaryDataTypes.INT32_LE.read(b, 0));
    }

    @Test
    public void shouldSupportInt32BeRead() {
        byte[] b = { (byte) 0xa3, (byte) 0xc4, (byte) 0x45, (byte) 0x83 };
        assertEquals((Integer) 0xa3c44583, BinaryDataTypes.INT32_BE.read(b, 0));
    }

    @Test
    public void shouldSupportInt64LeRead() {
        byte[] b = { (byte) 0xa3, (byte) 0xc4, (byte) 0x45, (byte) 0x83, (byte) 0xa3, (byte) 0xc4, (byte) 0x45,
                (byte) 0x83 };
        assertEquals((Long) 0x8345c4a38345c4a3L, BinaryDataTypes.INT64_LE.read(b, 0));
    }

    @Test
    public void shouldSupportInt64BeRead() {
        byte[] b = { (byte) 0xa3, (byte) 0xc4, (byte) 0x45, (byte) 0x83, (byte) 0xa3, (byte) 0xc4, (byte) 0x45,
                (byte) 0x83 };
        assertEquals((Long) 0xa3c44583a3c44583L, BinaryDataTypes.INT64_BE.read(b, 0));
    }

    @Test
    public void shouldSupportFloatBeRead() {
        byte[] b = { (byte) 0xa3, (byte) 0xc4, (byte) 0x45, (byte) 0x83 };
        assertEquals((float) -2.1279802e-17, BinaryDataTypes.FLOAT_BE.read(b, 0), 0.0000001e-17);
    }

    @Test
    public void shouldSupportFloatLeRead() {
        byte[] b = { (byte) 0xa3, (byte) 0xc4, (byte) 0x45, (byte) 0x83 };
        assertEquals((float) -5.8118825e-37, BinaryDataTypes.FLOAT_LE.read(b, 0), 0.0000001e-37);
    }

    @Test
    public void shouldSupportDoubleBeRead() {
        byte[] b = { (byte) 0x46, (byte) 0xa3, (byte) 0xb8, (byte) 0xb5, (byte) 0xb5, (byte) 0x05, (byte) 0x6e,
                (byte) 0x17 };
        assertEquals(2e+32, BinaryDataTypes.DOUBLE_BE.read(b, 0), 1);
    }

    @Test
    public void shouldSupportDoubleLeRead() {
        byte[] b = { (byte) 0x17, (byte) 0x6e, (byte) 0x05, (byte) 0xb5, (byte) 0xb5, (byte) 0xb8, (byte) 0xa3,
                (byte) 0x46 };
        assertEquals(2e+32, BinaryDataTypes.DOUBLE_LE.read(b, 0), 1);
    }

    @Test
    public void shouldSupportUInt16LeWrite() {
        byte[] b = { 0, 0 };
        BinaryDataTypes.UINT16_LE.write(b, 0, 0xc4a3);
        assertArrayEquals(new byte[] { (byte) 0xa3, (byte) 0xc4 }, b);
    }

    @Test
    public void shouldSupportUInt16BeWrite() {
        byte[] b = { 0, 0 };
        BinaryDataTypes.UINT16_BE.write(b, 0, 0xc4a3);
        assertArrayEquals(new byte[] { (byte) 0xc4, (byte) 0xa3 }, b);
    }

    @Test
    public void shouldSupportUInt32LeWrite() {
        byte[] b = { 0, 0, 0, 0 };
        BinaryDataTypes.UINT32_LE.write(b, 0, (long) 0x8345c4a3);
        assertArrayEquals(new byte[] { (byte) 0xa3, (byte) 0xc4, (byte) 0x45, (byte) 0x83 }, b);
    }

    @Test
    public void shouldSupportUInt32BeWrite() {
        byte[] b = { 0, 0, 0, 0 };
        BinaryDataTypes.UINT32_BE.write(b, 0, (long) 0x8345c4a3);
        assertArrayEquals(new byte[] { (byte) 0x83, (byte) 0x45, (byte) 0xc4, (byte) 0xa3 }, b);
    }

    @Test
    public void shouldSupportInt16LeWrite() {
        byte[] b = { 0, 0 };
        BinaryDataTypes.INT16_LE.write(b, 0, (short) -15197);
        assertArrayEquals(new byte[] { (byte) 0xa3, (byte) 0xc4 }, b);
    }

    @Test
    public void shouldSupportInt16BeWrite() {
        byte[] b = { 0, 0 };
        BinaryDataTypes.INT16_BE.write(b, 0, (short) -23612);
        assertArrayEquals(new byte[] { (byte) 0xa3, (byte) 0xc4 }, b);
    }

    @Test
    public void shouldSupportInt32LeWrite() {
        byte[] b = { 0, 0, 0, 0 };
        BinaryDataTypes.INT32_LE.write(b, 0, -2092579677);
        assertArrayEquals(new byte[] { (byte) 0xa3, (byte) 0xc4, (byte) 0x45, (byte) 0x83 }, b);
    }

    @Test
    public void shouldSupportInt32BeWrite() {
        byte[] b = { 0, 0, 0, 0 };
        BinaryDataTypes.INT32_BE.write(b, 0, -1547418237);
        assertArrayEquals(new byte[] { (byte) 0xa3, (byte) 0xc4, (byte) 0x45, (byte) 0x83 }, b);
    }

    @Test
    public void shouldSupportInt64LeWrite() {
        byte[] b = { 0, 0, 0, 0, 0, 0, 0, 0 };
        BinaryDataTypes.INT64_LE.write(b, 0, -4106410252820221960L);
        assertArrayEquals(new byte[] { (byte) 0xf8, (byte) 0xef, (byte) 0xa0, (byte) 0xba, (byte) 0xa1, (byte) 0x19,
                (byte) 0x03, (byte) 0xc7 }, b);
    }

    @Test
    public void shouldSupportInt64BeWrite() {
        byte[] b = { 0, 0, 0, 0, 0, 0, 0, 0 };
        BinaryDataTypes.INT64_BE.write(b, 0, -509011509442444345L);
        assertArrayEquals(new byte[] { (byte) 0xf8, (byte) 0xef, (byte) 0xa0, (byte) 0xba, (byte) 0xa1, (byte) 0x19,
                (byte) 0x03, (byte) 0xc7 }, b);
    }

    @Test
    public void shouldSupportFloatBeWrite() {
        byte[] b = { 0, 0, 0, 0 };
        BinaryDataTypes.FLOAT_BE.write(b, 0, (float) 2e+12);
        assertArrayEquals(new byte[] { (byte) 0x53, (byte) 0xe8, (byte) 0xd4, (byte) 0xa5 }, b);
    }

    @Test
    public void shouldSupportFloatLeWrite() {
        byte[] b = { 0, 0, 0, 0 };
        BinaryDataTypes.FLOAT_LE.write(b, 0, (float) 2e+12);
        assertArrayEquals(new byte[] { (byte) 0xa5, (byte) 0xd4, (byte) 0xe8, (byte) 0x53 }, b);
    }

    @Test
    public void shouldSupportDoubleBeWrite() {
        byte[] b = { 0, 0, 0, 0, 0, 0, 0, 0 };
        BinaryDataTypes.DOUBLE_BE.write(b, 0, 2e+32);
        assertArrayEquals(new byte[] { (byte) 0x46, (byte) 0xa3, (byte) 0xb8, (byte) 0xb5, (byte) 0xb5, (byte) 0x05,
                (byte) 0x6e, (byte) 0x17 }, b);
    }

    @Test
    public void shouldSupportDoubleLeWrite() {
        byte[] b = { 0, 0, 0, 0, 0, 0, 0, 0 };
        BinaryDataTypes.DOUBLE_LE.write(b, 0, 2e+32);
        assertArrayEquals(new byte[] { (byte) 0x17, (byte) 0x6e, (byte) 0x05, (byte) 0xb5, (byte) 0xb5, (byte) 0xb8,
                (byte) 0xa3, (byte) 0x46 }, b);
    }

}
