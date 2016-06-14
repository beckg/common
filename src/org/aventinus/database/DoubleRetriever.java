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
public class DoubleRetriever extends RetrieverAdapter
{
    private ArrayList<Double> values;

    public DoubleRetriever()
    {
        values = new ArrayList<Double>();
    }

    public DoubleRetriever(ArrayList<Double> values)
    {
        this.values = values;
    }

    public void processRow()
    {
        values.add(getDouble(1));
    }

    public double getValue()
    {
        if (values.size() == 0)
        {
            return 0;
        }

        return values.get(values.size() - 1).doubleValue();
    }

    public ArrayList<Double> getValues()
    {
        return values;
    }
}
