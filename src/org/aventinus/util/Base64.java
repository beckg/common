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

//-----------------------------------------------------------------------------------
//
//-----------------------------------------------------------------------------------
public class Base64
{
    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    private static Logger logger = Logger.logger(Base64.class);

    private static final String CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
 
    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    public static void main(String[] args) 
    {
        new Base64().test();
    }

    public void test()
    {
        try
        {
            String[] tests = {""
                              ,"a"
                              ,"ab"
                              ,"abc"
                              ,"abcd"
                              ,"abcde"
                              ,"abcdef"
                              ,"ahgktlveowjxdlfzpekakrkvkdwsapqwipqoir4763651934ls.cm,yx.y,HJUUZJKLKLFDQ,:_+!'fghfeeeee"
                             };

            for (int i = 0; i < tests.length; i++)
            {
                String[] encoding = Base64.encode(tests[i].getBytes());
                String decoding = new String(Base64.decode(encoding));
                for (int j = 0; j < encoding.length; j++)
                {
                    logger.log("test[" + i + "] line[" + encoding[j] + "]");
                }
                logger.log("test[" + i + "] line[" + decoding + "]");
                if (! decoding.equals(tests[i]))
                {
                    throw new IllegalArgumentException("Invalid encode/decode");
                }
            }

            byte[] bytes = new byte[256];
            for (int i = 0; i < 256; i++)
            {
                bytes[i] = (byte)(i & 0xff);
            }
            String[] encoding = Base64.encode(bytes);
            for (int j = 0; j < encoding.length; j++)
            {
                logger.log("test[binary] line[" + encoding[j] + "]");
            }
            bytes = Base64.decode(encoding);
            for (int k = 0; k < 256; k++)
            {
                if (bytes[k] != (byte)k)
                {
                    throw new IllegalArgumentException("Invalid encode/decode");
                }
            }
            logger.log("test[binary] OK");
        }
        catch (Throwable exception)
        {
            logger.fatal(exception);
        }
    }
 
    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    public static String[] encode(byte[] value)
    {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < value.length; i += 3) 
        {
            int j = (value[i] & 0xff) << 16;
            if (i + 1 < value.length)
            {
                j += (value[i + 1] & 0xff) << 8;
            }
            if (i + 2 < value.length)
            {
                j += value[i + 2] & 0xff;
            }

            buffer.append(CHARSET.charAt((j >> 18) & 0x3f));
            buffer.append(CHARSET.charAt((j >> 12) & 0x3f));
            if (i + 1 < value.length)
            {
                buffer.append(CHARSET.charAt((j >> 6) & 0x3f));
            }
            else
            {
                buffer.append("=");
            }
            if (i + 2 < value.length)
            {
                buffer.append(CHARSET.charAt(j & 0x3f));
            }
            else
            {
                buffer.append("=");
            }
        }

        String[] result = new String[(buffer.length() + 76 - 1) / 76];

        for (int i = 0; i < result.length; i++)
        {
            int start = i * 76;
            int end = (i + 1) * 76;
            if (end > buffer.length())
            {
                end = buffer.length();
            }
            result[i] = buffer.substring(start, end);
        }

        return result; 
    }

    public static byte[] decode(String[] value)
    {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < value.length; i++)
        {
            buffer.append(value[i]);
        }
        return decode(buffer.toString());
    }

    public static byte[] decode(String value)
    {
        if (value.length() == 0)
        {
            return new byte[0];
        }

        if (value.length() % 4 != 0)
        {
            throw new IllegalArgumentException("Invalid value length");
        }

        int length = (value.length() / 4) * 3;
        if (value.charAt(value.length() - 1) == '=')
        {
            length--;
        }
        if (value.charAt(value.length() - 2) == '=')
        {
            length--;
        }
        
        byte[] bytes = new byte[length];
        for (int i = 0; i < value.length(); i += 4)
        {
            int char1 = lookup(value.charAt(i));
            int char2 = lookup(value.charAt(i + 1));
            int char3 = 0;
            if (value.charAt(i + 2) != '=')
            {
                char3 = lookup(value.charAt(i + 2));
            }
            int char4 = 0;
            if (value.charAt(i + 3) != '=')
            {
                char4 = lookup(value.charAt(i + 3));
            }

            int decode = (char1 << 18) | (char2 << 12) | (char3 << 6) | char4;

            int offset = (i / 4) * 3;
            bytes[offset] = (byte)((decode >> 16) & 0xff);
            if (value.charAt(i + 2) != '=')
            {
                bytes[offset + 1] = (byte)((decode >> 8) & 0xff);
            }
            if (value.charAt(i + 3) != '=')
            {
                bytes[offset + 2] = (byte)(decode & 0xff);
            }
        }

        return bytes;
    }

    private static int lookup(char value)
    {
        int decode;
        if ((value >= 'A') && (value <= 'Z'))
        {
            decode = value - 65;
        }
        else if ((value >= 'a') && (value <= 'z'))
        {
            decode = 26 + value - 97;
        }
        else if ((value >= '0') && (value <= '9'))
        {
            decode = 26 + 26 + value - 48;
        }
        else if (value == '+')
        {
            decode = 26 + 26 + 10 + value - 43;
        }
        else if (value == '/')
        {
            decode = 26 + 26 + 10 + 1 + value - 47;
        }
        else
        {
            throw new IllegalArgumentException("Invald character[" + value + "]");
        }

        return decode;
    }
}
