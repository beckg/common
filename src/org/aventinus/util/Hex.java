//-----------------------------------------------------------------------------------
// Copyright (c) 2009-2013, Gordon Beck (gordon.beck@aventinus.org). All rights reserved.
//
//    This file is part of a suite of tools. 
//
//    The tools are free software: you can redistribute it and/or modify 
//    it under the terms of the GNU General Public License as published by 
//    the Free Software Foundation, either version 3 of the License, or 
//    (at your option) any later version. 
// 
//    The tools are distributed in the hope that they will be useful, 
//    but WITHOUT ANY WARRANTY; without even the implied warranty of 
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
//    GNU General Public License for more details. 
// 
//    You should have received a copy of the GNU General Public License 
//    along with these tools.  If not, see <http://www.gnu.org/licenses/>.
//-----------------------------------------------------------------------------------
package org.aventinus.util;

import java.util.*;

//-----------------------------------------------------------------------------------
//
//-----------------------------------------------------------------------------------
public final class Hex
{
    private Hex() {}

    //-------------------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------------------
    public static String toHex(byte value)
    {
        byte[] data = new byte[1];
        data[0] = value;
        return toHex(data);
    }

    public static String toHex(short value)
    {
        byte[] data = new byte[2];
        data[0] = (byte)(value >> 8);
        data[1] = (byte)value;
        return toHex(data);
    }

    public static String toHex(int value)
    {
        byte[] data = new byte[4];
        data[0] = (byte)(value >> 24);
        data[1] = (byte)(value >> 16);
        data[2] = (byte)(value >> 8);
        data[3] = (byte)value;
        return toHex(data);
    }

    public static String toHex(long value)
    {
        byte[] data = new byte[8];
        data[0] = (byte)(value >> 56);
        data[1] = (byte)(value >> 48);
        data[2] = (byte)(value >> 40);
        data[3] = (byte)(value >> 32);
        data[4] = (byte)(value >> 24);
        data[5] = (byte)(value >> 16);
        data[6] = (byte)(value >> 8);
        data[7] = (byte)value;
        return toHex(data);
    }

    public static String toHex(byte[] data)
    {
        return toHex(data, 0, data.length);
    }

    public static String toHex(byte[] data, int offset, int length)
    {
        StringBuilder hex = new StringBuilder();

        if (length > data.length - offset)
            length = data.length - offset;
        for (int i = 0; i < length; i++)
        { 
            int msb = ((int)data[offset + i] & 0x00F0) / 16; 
            int lsb = data[offset + i] & 0x000F;
            hex.append("0123456789ABCDEF".charAt(msb));
            hex.append("0123456789ABCDEF".charAt(lsb)); 
        }

        return hex.toString();
    }

    public static String toHexLines(byte[] data)
    {
        return toHexLines(data, 0, data.length);
    }

    public static String toHexLines(byte[] data, int offset, int length)
    {
        StringBuilder buffer = new StringBuilder();
        for (int j = 0; j < (length + 31) / 32; j++)
        {
            int len = Math.min(32, length - j * 32); 
            buffer.append("\n").append(Hex.toHex(j * 32).substring(4))
                  .append(" ").append(Hex.toHex(data, offset + j * 32, len));
        }
        return buffer.toString();
    }
}
