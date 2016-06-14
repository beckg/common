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
package org.aventinus.json;

import java.util.*;

import org.aventinus.util.*;

//-----------------------------------------------------------------------------------
//
//-----------------------------------------------------------------------------------
public class JSONArray
{
    //-------------------------------------------------------------------------------
    // Use of toolbox is dicey as it uses JSONObject to initialiase
    //-------------------------------------------------------------------------------
    private static Toolbox toolbox = Toolbox.toolbox();
    private static Logger logger = Logger.logger(JSONArray.class);

    private String error = "";

    private ArrayList<String> mList = new ArrayList<String>();

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    public JSONArray()
    {
    }

    public JSONArray(JSONArray other)
    {
        error = other.error;
        mList.addAll(other.mList);
    }

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    public JSONArray add(String value)
    {
        if (value == null)
        {
            throw new JSONException("String value cannot be null");
        }
        mList.add(JSONObject.escape(value));
        return this;
    }

    public JSONArray add(int value)
    {
        mList.add(Integer.toString(value));
        return this;
    }

    public JSONArray add(long value)
    {
        mList.add(Long.toString(value));
        return this;
    }

    public JSONArray add(double value)
    {
        mList.add(Double.toString(value));
        return this;
    }

    public JSONArray add(boolean value)
    {
        mList.add(value ? "true" : "false");
        return this;
    }

    public JSONArray add(JSONObject value)
    {
        mList.add(value.toString());
        return this;
    }

    public JSONArray add(JSONArray value)
    {
        mList.add(value.toString());
        return this;
    }

    public void remove(int index)
    {
        mList.remove(index);
    }

    public void appendLF()
    {
        for (int i = 0; i < mList.size(); i++)
        {
            mList.set(i, mList.get(i) + "\n");
        }
    }

    public String toString()
    {
        StringBuilder buffer = new StringBuilder();

        buffer.append("[");
        boolean first = true;
        for (String value: mList)
        {
            if (! first)
            {
                buffer.append(",");
            }
            first = false;
            buffer.append(value);
        }
        buffer.append("]");

        return buffer.toString();
    }

    protected void addRaw(String value)
    {
        mList.add(value);
    }

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    public int size()
    {
        return mList.size();
    }

    public boolean isObject(int index)
    {
        return mList.get(index).startsWith("{");
    }

    public boolean isArray(int index)
    {
        return mList.get(index).startsWith("[");
    }

    boolean isValue(int index)
    {
        if (isObject(index) || isArray(index))
        {
            return false;
        }
        return true;
    }

    // We do not have default methods as the value must exist in the array !

    public String getString(int index)
    {
        if (isObject(index) || isArray(index))
        {
            throw new JSONException("Not a value");
        }

        return JSONObject.unescape(mList.get(index));
    }

    public int getInteger(int index)
    {
        return JSONObject.parseInt(getString(index));
    }

    public long getLong(int index)
    {
        return JSONObject.parseLong(getString(index));
    }

    public double getDouble(int index)
    {
        return JSONObject.parseDouble(getString(index));
    }

    public boolean getBoolean(int index)
    {
        return getString(index).equals("true");
    }

    public JSONObject getObject(int index)
    {
        if (! isObject(index))
        {
            throw new JSONException("Not an object");
        }

        return new JSONObject(mList.get(index));
    }

    public JSONMap getMap(int index)
    {
        if (! isObject(index))
        {
            throw new JSONException("Not an object");
        }

        return new JSONObject(mList.get(index)).getMap();
    }

    public JSONArray getArray(int index)
    {
        if (! isArray(index))
        {
            throw new JSONException("Not an array");
        }

        return new JSONObject(new StringBuilder("{a:").append(mList.get(index)).append("}").toString()).getArray("a");
    }

    public String toString(int index)
    {
        return mList.get(index);
    }

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    public void dump()
    {
        dump("");
    }

    public void dump(String text)
    {
         StringBuilder buffer = new StringBuilder();
         dump(buffer, 1);
         logger.log(new StringBuilder("JSONArray dump(").append(text).append("): \n").append(buffer));
    }

    public void dump(StringBuilder buffer, int level)
    {
        String prefix = "";
        for (int i = 0; i < level; i++)
        {
            prefix = prefix + "   ";
        }

        for (int i = 0; i < mList.size(); i++)
        {
            String value = mList.get(i);
            if (isObject(i))
            {
                buffer.append(prefix).append("   ").append("{\n");
                getObject(i).dump(buffer, level + 1);
                buffer.append(prefix).append("   ").append("}\n");
            }
            else if (isArray(i))
            {
                buffer.append(prefix).append("   ").append("[\n");
                getArray(i).dump(buffer, level + 1);
                buffer.append(prefix).append("   ").append("]\n");
            }
            else
            {
                buffer.append(prefix).append("   ").append(value).append("\n");
            }
        }
    }
}
