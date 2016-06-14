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

import org.aventinus.util.*;

//-----------------------------------------------------------------------------------
//
//-----------------------------------------------------------------------------------
public class GenericRetriever extends RetrieverAdapter
{
    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    private static Toolbox toolbox = Toolbox.toolbox();
    private static Logger logger = Logger.logger(Database.class);

    private ArrayList<ArrayList<Pair<String, Object>>> cache;

    public GenericRetriever()
    {
        cache = new ArrayList<ArrayList<Pair<String, Object>>>();
    }

    public GenericRetriever(ArrayList<ArrayList<Pair<String, Object>>> cache)
    {
        this.cache = cache;
    }

    public void processRow()
    {
        ArrayList<Pair<String, Object>> columns = new ArrayList<Pair<String, Object>>();

        for (int i = 0; i < getColumns(); i++)
        {
            columns.add(new Pair<String, Object>(getName(i + 1), getObject(i + 1)));
        }

        cache.add(columns);
    }

    public ArrayList<ArrayList<Pair<String, Object>>> getValues()
    {
        return cache;
    }
}
