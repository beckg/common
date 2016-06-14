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

import java.io.*;
import java.util.*;

//----------------------------------------------------------------------------------------
//
//----------------------------------------------------------------------------------------
public final class StringIntern
{
    //------------------------------------------------------------------------------------
    //
    //------------------------------------------------------------------------------------
    private static HashMap<String,String> strings = new HashMap<String,String>(50000);
    private static int requests = 0;

    private StringIntern() {}

    //------------------------------------------------------------------------------------
    //
    //------------------------------------------------------------------------------------
    public static String intern(String value)
    {
        if (value.length() == 0)
        {
            return "";
        }

        synchronized(strings)
        {
            String intern = strings.get(value);
            if (intern == null)
            {
                intern = new String(value);
                strings.put(intern, intern);
            }

            requests++;
            return intern;
        }
    }

    public static String testAndIntern(String value)
    {
        if (value.length() == 0)
        {
            return "";
        }

        if (value.length() <= 3)
        {
            return intern(value);
        }

        synchronized(strings)
        {
            String intern = strings.get(value);
            if (intern == null)
            {
                return new String(value);
            }

            requests++;
            return intern;
        }
    }

    public static int size()
    {
        synchronized(strings)
        {
            return strings.size();
        }
    }

    public static int requests()
    {
        synchronized(strings)
        {
            return requests;
        }
    }
}
