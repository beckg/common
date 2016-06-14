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

import org.aventinus.json.*;

public class Test
{
    private static Toolbox toolbox = Toolbox.toolbox();
    private static Logger logger = Logger.logger(Test.class);

    public static void main(String[] argv)
    {
        new Test().run();
    }
    
    private Test()
    {
    }

    private void run()
    {
        try
        {
            test8();
        }
        catch (Exception exception)
        {
            logger.fatal(exception);
        }          
    }

    private void test8()
    {
        try
        {
            File file = new File("C:/Documents and Settings/gordon/Desktop/Dilbert.gif");
            logger.info("bytes=" + file.length());

            InputStream input = new FileInputStream(file);
            byte[] bytes = new byte[(int)file.length()];
            input.read(bytes);
            input.close();

            for (int i = 0; i < bytes.length - 1; i += 2)
            {
                 byte temp0 = bytes[i];
                 byte temp1 = bytes[i + 1];

                 bytes[i] = (byte)((temp0 & 0xf0) | (temp1 & 0x0f));
                 bytes[i + 1] = (byte)((temp0 & 0x0f) | (temp1 & 0xf0));
            }

            OutputStream output = new FileOutputStream("C:/Documents and Settings/gordon/Desktop/Dilbert.zip");
            output.write(bytes);
            output.close();
        }
        catch (Throwable exception)
        {
            logger.error("", exception);
        }
    }
}
