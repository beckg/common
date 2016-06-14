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
import java.text.*;
import java.io.*;

//-----------------------------------------------------------------------------------
// Instances of this class are invariant (there is not and should not be a set(long) method
//-----------------------------------------------------------------------------------
//-----------------------------------------------------------------------------------
// The aim is to convert dates on the input boundary to long and 
//        to the required format on the outout boundary.
// These are slightly perculiar routines but can remove some of the drugery of
//        working with date times.
// They have a particular performance objective
//-----------------------------------------------------------------------------------
public class DateTime implements Comparable<DateTime>
{
    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    static final long serialVersionUID = -698019209134034L;

    private static final HashMap<Long, DateTime> internCache = new HashMap<Long, DateTime>();
    public static final DateTime ZERO_DATE = new DateTime(0).intern();

    private static SimpleDateFormat dateFormat;
    static
    {
        dateFormat = new SimpleDateFormat("yyyyMMdd.HHmmss.SSS");
        dateFormat.setCalendar(Calendar.getInstance(TimeZone.getTimeZone("GMT")));
    }

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    private long time = 0;

    public DateTime(DateTime other)
    {
        time = other.time;
    }

    public DateTime(long value)
    {
        time = value;
    }

    public DateTime intern()
    {
        Long ltime = Long.valueOf(time);

        synchronized(internCache)
        {
            DateTime other = internCache.get(ltime);

            if (other != null)
            {
                return other;
            }

            internCache.put(ltime, this);
        }

        return this;
    }

    public static DateTime intern(long value)
    {
        Long ltime = Long.valueOf(value);
        DateTime other;

        synchronized(internCache)
        {
            other = internCache.get(ltime);
            if (other != null)
            {
                return other;
            }

            other = new DateTime(value);
            internCache.put(ltime, other);
        }

        return other;
    }

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    public static long now()
    {
        return System.currentTimeMillis();
    }

    public static DateTime asNow()
    {
        return new DateTime(System.currentTimeMillis());
    }

    public long getTime()
    {
        return time;
    }

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    public static long dateToLong(String value)
    {
        synchronized(dateFormat)
        {
            ParsePosition pos = new ParsePosition(0);
            Date date = dateFormat.parse(value, pos);
            if ((date == null) || (pos.getIndex() != value.length()))
            {
                throw new IllegalArgumentException("Invalid date [" + value + "]");
            }
            return date.getTime();
        }
    }

    public static String dateToString(long value)
    {
        synchronized(dateFormat)
        {
            StringBuffer buffer = new StringBuffer();
            dateFormat.format(new Date(value), buffer, new FieldPosition(0));     
            return buffer.toString();
        }
    }

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    public int compareTo(DateTime other)
    {
        return (time < other.time ? -1 : (time == other.time ? 0 : 1));
    }

    public boolean equals(Object object)
    {
        return ((object instanceof DateTime) && (time == ((DateTime)object).time));
    }

    public int hashCode()
    {
        return (int)time;
    }
}
