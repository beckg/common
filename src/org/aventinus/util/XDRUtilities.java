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
import java.io.*;
import java.net.*;

//-----------------------------------------------------------------------------------------------
// Frightening kind of thing: build RPC XDR requests/responses by hand !
//-----------------------------------------------------------------------------------------------
public class XDRUtilities
{
    //-------------------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------------------
    private static Toolbox toolbox = Toolbox.toolbox();
    private static Logger logger = Logger.logger(XDRUtilities.class);

    public static final int TCP = 6;
    public static final int UDP = 17;

    //-------------------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------------------
    private static abstract class Buffer
    {
        protected byte[] m_buffer;
        protected int m_length;

        protected byte[] getBuffer()
        {
            return m_buffer;
        }

        public int getLength()
        {
            return m_length;
        }

        protected void affirm(int upto)
        {
        }

        public byte peekByte(int offset)
        {
            affirm(offset + 1);

            if (offset + 1 > getLength())
                throw new RuntimeException("Buffer overrun");

            return m_buffer[offset];
        }

        public int peekInteger(int offset)
        {
            affirm(offset + 4);

            if (offset + 4 > getLength())
                throw new RuntimeException("Buffer overrun");

            return XDRUtilities.extractInteger(m_buffer, offset);
        }

        public long peekLongFromInteger(int offset)
        {
            affirm(offset + 4);

            if (offset + 4 > getLength())
                throw new RuntimeException("Buffer overrun");

            // To return an effectively unsigned value

            return ((long)(m_buffer[offset + 0] & 0x000000ff) << 24) | 
                   ((long)(m_buffer[offset + 1] & 0x000000ff) << 16) | 
                   ((long)(m_buffer[offset + 2] & 0x000000ff) << 8) | 
                   (long)(m_buffer[offset + 3] & 0x000000ff);
        }

        public long peekLong(int offset)
        {
            affirm(offset + 8);

            if (offset + 8 > getLength())
                throw new RuntimeException("Buffer overrun");

            return ((long)(m_buffer[offset + 0] & 0x000000ff) << 56) | 
                   ((long)(m_buffer[offset + 1] & 0x000000ff) << 48) | 
                   ((long)(m_buffer[offset + 2] & 0x000000ff) << 40) | 
                   ((long)(m_buffer[offset + 3] & 0x000000ff) << 32) | 
                   ((long)(m_buffer[offset + 4] & 0x000000ff) << 24) | 
                   ((long)(m_buffer[offset + 5] & 0x000000ff) << 16) | 
                   ((long)(m_buffer[offset + 6] & 0x000000ff) << 8) | 
                   (long)(m_buffer[offset + 7] & 0x000000ff);
        }

        public void peekByteArray(int offset, byte[] target, int targetOffset, int length)
        {
            affirm(offset + length);

            if (offset + length > getLength())
                throw new RuntimeException("Buffer overrun");

            System.arraycopy(m_buffer, offset, target, targetOffset, length);
        }

        public String peekString(int offset)
        {
            int length = peekInteger(offset);

            affirm(offset + 4 + length);

            if (offset + 4 + length > getLength())
                throw new RuntimeException("Buffer overrun");

            return new String(m_buffer, offset + 4, length);
        }

        public String peekHex(int offset, int length)
        {
            affirm(offset + length);

            if (offset + length > getLength())
                throw new RuntimeException("Buffer overrun");

            StringBuilder buffer = new StringBuilder();

            for (int i = 0; i < length; i++)
            {
                int value = m_buffer[offset + i] & 0x000000ff;
                if (value < 16)
                    buffer.append("0");
                buffer.append(Integer.toString(value, 16));
            }

            return buffer.toString();
        }

        public void dump(String prefix)
        {
            for (int i = 0; i < getLength() / 4; i++)
            {
                logger.log(prefix + padLeft(Integer.toString(i * 4, 16), 6, '0')
                                  + " " + peekHex(i * 4, 4)
                                  + " " + mapToString(i * 4, 4));
            }
            logger.log("");
        }

        private String mapToString(int offset, int length)
        {
            byte[] result = new byte[length];
            for (int i = 0; i < length; i++)
            {
                byte value = peekByte(offset + i);
                if ((value < 32) || (value > 127))
                    result[i] = 32;
                else
                    result[i] = value;
            }

            return "[" + new String(result) + "]";
        }
    }

    public static void storeInteger(int value, byte[] buffer, int offset)
    {
        buffer[offset + 0] = (byte)(value >> 24 & 0x000000ff);
        buffer[offset + 1] = (byte)(value >> 16 & 0x000000ff);
        buffer[offset + 2] = (byte)(value >>  8 & 0x000000ff);
        buffer[offset + 3] = (byte)(value >>  0 & 0x000000ff);
    }

    public static int extractInteger(byte[] buffer, int offset)
    {
        return ((buffer[offset + 0] & 0x000000ff) << 24) | 
               ((buffer[offset + 1] & 0x000000ff) << 16) | 
               ((buffer[offset + 2] & 0x000000ff) << 8) | 
               (buffer[offset + 3] & 0x000000ff);
    }

    //-------------------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------------------
    public static class Encoder extends Buffer
    {
        private int m_offset = 0;

        public Encoder(int length)
        {
            m_buffer = new byte[length];
            m_length = length;
        }

        public int getLength() // We only return the encoded length
        {
            return m_offset;
        }

        public int getOffset()
        {
            return m_offset;
        }

// should really protect buffer but since we are storing offset...
        public byte[] getBuffer()
        {
            return super.getBuffer();
        }

        public void storeByte(byte value, int offset)
        {
            if (offset + 1 > m_buffer.length)
                throw new RuntimeException("Buffer overrun");

            m_buffer[offset] = value;
        }

        public void storeInteger(int value)
        {
            storeInteger(value, m_offset);
            m_offset += 4;
        }

        public void storeInteger(int value, int offset)
        {
            if (offset + 4 > m_buffer.length)
                throw new RuntimeException("Buffer overrun");

            XDRUtilities.storeInteger(value, m_buffer, offset);
        }

        public void storeLong(long value)
        {
            storeLong(value, m_offset);
            m_offset += 8;
        }

        public void storeLong(long value, int offset)
        {
            if (offset + 8 > m_buffer.length)
                throw new RuntimeException("Buffer overrun");

            m_buffer[offset + 0] = (byte)(value >> 56 & 0x000000ff);
            m_buffer[offset + 1] = (byte)(value >> 48 & 0x000000ff);
            m_buffer[offset + 2] = (byte)(value >> 40 & 0x000000ff);
            m_buffer[offset + 3] = (byte)(value >> 32 & 0x000000ff);
            m_buffer[offset + 4] = (byte)(value >> 24 & 0x000000ff);
            m_buffer[offset + 5] = (byte)(value >> 16 & 0x000000ff);
            m_buffer[offset + 6] = (byte)(value >>  8 & 0x000000ff);
            m_buffer[offset + 7] = (byte)(value >>  0 & 0x000000ff);
        }

        public void storeString(String value)
        {
            int strl = (value.length() + 3) / 4; strl *= 4;

            storeString(value, m_offset);
            m_offset += 4 + strl;
        }

        public void storeString(String value, int offset)
        {
            storeInteger(value.length(), offset);
            System.arraycopy(value.getBytes(), 0, m_buffer, offset + 4, value.length());
        }

        public void storeByteArray(Encoder value)
        {
            storeByteArray(value.m_buffer, value.m_offset, m_offset);
            m_offset += value.m_offset;
        }

        public void storeByteArray(byte[] value)
        {
            storeByteArray(value, value.length, m_offset);
            m_offset += value.length;
        }

        public void storeByteArray(byte[] value, int length, int offset)
        {
            if (offset + value.length > m_buffer.length)
                throw new RuntimeException("Buffer overrun");

            System.arraycopy(value, 0, m_buffer, offset, value.length);
        }
    }

    //-------------------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------------------
    public static class Decoder extends Buffer
    {
        private int m_offset = 0;

        private Socket m_socket;

        public Decoder(byte[] buffer)
        {
            this(buffer, buffer.length);
        }

        public Decoder(byte[] buffer, int length)
        {
            if (length > buffer.length)
                throw new RuntimeException("Buffer overrun");
 
            m_buffer = buffer;
            m_length = length;
        }

/*
        public Decoder(Socket socket)
        {
            m_socket = socket;

            m_buffer = new byte[4096];
            m_length = 0;
        }
*/

        public int getOffset()
        {
            return m_offset;
        }

        public int extractInteger()
        {
            int value = peekInteger(m_offset);
            m_offset += 4;
            return value;
        }

        public long extractLong()
        {
            long value = peekLong(m_offset);
            m_offset += 8;
            return value;
        }

        public byte[] extractByteArray(int length)
        {
            byte[] target = new byte[length];
            peekByteArray(m_offset, target, 0, length);
            m_offset += length;
            return target;
        }

        public void extractByteArray(byte[] target, int targetOffset, int length)
        {
            peekByteArray(m_offset, target, targetOffset, length);
            m_offset += length;
        }

        public String extractString()
        {
            int length = peekInteger(m_offset);
            int strl = (length + 3) / 4; strl *= 4;

            String value = peekString(m_offset);
            m_offset += 4 + strl;
            return value;
        }

/*
        protected void affirm(int upto)
        {
            if (m_length >= upto)
                return;

            try
            {
                if (m_buffer.length < upto)
                {
                    byte[] temp = new byte[upto + 4096];
                    System.arraycopy(m_buffer, 0, temp, 0, m_length);
                    m_buffer = temp;
                }

                long start = System.currentTimeMillis();

                m_channel.configureBlocking(false);        
                while(m_length < upto)
                {
                    ByteBuffer buffer = ByteBuffer.allocate(m_buffer.length - m_length);
//logger.log("rem: " + buffer.remaining());
                    int bytes = m_channel.read(buffer);
                    if (bytes < 0)
                        throw new RuntimeException("Socket closed");
                    else if (bytes == 0)
                    {
                        if (System.currentTimeMillis() - start > 30 * 1000)
                           throw new RuntimeException("Timeout");
                        Utility.sleep(10);
                    }
                    else
                    {
logger.log("read: " + bytes);
                        buffer.flip();
                        buffer.get(m_buffer, m_length, bytes);
                        m_length += bytes;
                    }
                }
            }
            catch (IOException exception)
            {
                throw new RuntimeException(exception);
            }
        }
*/
    }

    //-------------------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------------------
    public static String padLeft(String value, int length, char pad)
    {
        if (value.length() > length)
            throw new RuntimeException("Initial value too long");

        while (value.length() < length)
            value = pad + value;
        return value;
    }

    public static String padRight(String value, int length, char pad)
    {
        if (value.length() > length)
            throw new RuntimeException("Initial value too long");

        while (value.length() < length)
            value = value + pad;
        return value;
    }
}
