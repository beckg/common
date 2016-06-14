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
package org.aventinus.database;

import java.util.*;

//-----------------------------------------------------------------------------------
//
//-----------------------------------------------------------------------------------
public class IntegerRetriever extends RetrieverAdapter
{
    private ArrayList<Integer> values;

    public IntegerRetriever()
    {
        values = new ArrayList<Integer>();
    }

    public IntegerRetriever(ArrayList<Integer> values)
    {
        this.values = values;
    }

    public void processRow()
    {
        values.add(getInteger(1));
    }

    public int getValue()
    {
        if (values.size() == 0)
        {
            return 0;
        }

        return values.get(values.size() - 1).intValue();
    }

    public ArrayList<Integer> getValues()
    {
        return values;
    }
}
