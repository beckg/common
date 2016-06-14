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
public class JSONList
{
    //-------------------------------------------------------------------------------
    // Use of toolbox is dicey as it uses JSONObject to initialiase
    //-------------------------------------------------------------------------------
    private static Toolbox toolbox = Toolbox.toolbox();
    private static Logger logger = Logger.logger(JSONList.class);

    private List<String> names = new ArrayList<String>();
    private List<String> values = new ArrayList<String>();

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    public JSONList()
    {
    }

    public JSONList(JSONList other)
    {
        names.addAll(other.names);
        values.addAll(other.values);
    }

    public JSONList add(String name, String value)
    {
        if ((name == null) || (name.length() == 0))
        {
            throw new JSONException("Invalid name [" + name + "]");
        }
        if (value == null)
        {
            throw new JSONException("String value cannot be null");
        }

        names.add(name);
        values.add(JSONObject.escape(value));
        return this;
    }

    public JSONList add(String name, int value)
    {
        if ((name == null) || (name.length() == 0))
        {
            throw new JSONException("Invalid name [" + name + "]");
        }

        names.add(name);
        values.add(Integer.toString(value));
        return this;
    }

    public JSONList add(String name, long value)
    {
        if ((name == null) || (name.length() == 0))
        {
            throw new JSONException("Invalid name [" + name + "]");
        }

        names.add(name);
        values.add(Long.toString(value));
        return this;
    }

    public JSONList add(String name, double value)
    {
        if ((name == null) || (name.length() == 0))
        {
            throw new JSONException("Invalid name [" + name + "]");
        }

        names.add(name);
        values.add(Double.toString(value));
        return this;
    }

    public JSONList add(String name, boolean value)
    {
        if ((name == null) || (name.length() == 0))
        {
            throw new JSONException("Invalid name [" + name + "]");
        }

        names.add(name);
        values.add(value ? "true" : "false");
        return this;
    }

    public JSONList add(String name, JSONObject value)
    {
        if ((name == null) || (name.length() == 0))
        {
            throw new JSONException("Invalid name [" + name + "]");
        }

        names.add(name);
        values.add(value.toString());
        return this;
    }

    public JSONList add(String name, JSONArray value)
    {
        if ((name == null) || (name.length() == 0))
        {
            throw new JSONException("Invalid name [" + name + "]");
        }

        names.add(name);
        values.add(value.toString());
        return this;
    }

    public String toString()
    {
        StringBuilder buffer = new StringBuilder();

        // It is returned as JSONObject.toString() as that is what it really is

        buffer.append("{");
        boolean first = true;
        for (int index = 0; index < names.size(); index++)
        {
            if (! first)
            {
                buffer.append(",");
            }
            first = false;
            buffer.append(JSONObject.escape(names.get(index)));
            buffer.append(":");
            buffer.append(values.get(index)); 
        }
        buffer.append("}");

        return buffer.toString();
    }

    protected void addRaw(String name, String value)
    {
        names.add(name);
        values.add(value);
    }

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    public int size()
    {
        return names.size();
    }

    public void clear()
    {
        names.clear();
        values.clear();
    }

    public void remove(int index)
    {
        names.remove(index);
        values.remove(index);
    }

    public String getName(int index)
    {
        return names.get(index);
    }

    public boolean isObject(int index)
    {
        String value = values.get(index);
//        if (value == null)
//        {
//            throw new JSONException("Value does not exist");
//        }

        return value.startsWith("{");
    }

    public boolean isArray(int index)
    {
        String value = values.get(index);
//        if (value == null)
//        {
//            throw new JSONException("Value does not exist");
//        }

        return value.startsWith("[");
    }

    public boolean isValue(int index)
    {
        if (isObject(index) || isArray(index))
        {
            return false;
        }
        return true;
    }

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    private String getValue(int index)
    {
        if ((index < 0) || (index >= values.size()))
        {
            throw new JSONException("Invalid index");
        }

        return values.get(index);
    }

    public String getString(int index)
    {
        String value = getValue(index);
        
        if (isObject(index) || isArray(index))
        {
            throw new JSONException("Not a value");
        }

        return JSONObject.unescape(value);
    }

    public int getInteger(int index)
    {
        String value = getValue(index);

        return JSONObject.parseInt(JSONObject.unescape(value));
    }

    public long getLong(int index)
    {
        String value = getValue(index);

        return JSONObject.parseLong(JSONObject.unescape(value));
    }

    public double getDouble(int index)
    {
        String value = getValue(index);

        return JSONObject.parseDouble(JSONObject.unescape(value));
    }

    public boolean getBoolean(int index)
    {
        String value = getValue(index);

        return JSONObject.unescape(value).equals("true");
    }

    public JSONObject getObject(int index)
    {
        String value = getValue(index);

        if (! isObject(index))
        {
            throw new JSONException("Not an object");
        }

        return new JSONObject(value);
    }

    public JSONArray getArray(int index)
    {
        String value = getValue(index);

        if (! isArray(index))
        {
            throw new JSONException("Not an array");
        }

        return new JSONObject(new StringBuilder("{a:").append(value).append("}").toString()).getArray("a");
    }

    public JSONMap getMap(int index)
    {
        String value = getValue(index);

        if (! isObject(index))
        {
            throw new JSONException("Not an object");
        }

        return new JSONMap(new JSONObject(value).getMap());
    }

    public JSONList getList(int index)
    {
        String value = getValue(index);

        if (! isObject(index))
        {
            throw new JSONException("Not an object");
        }

        return new JSONObject(value).getList();
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
         logger.log(new StringBuilder("JSONMap dump(").append(text).append("): \n").append(buffer));
    }

    public void dump(StringBuilder buffer, int level)
    {
        String prefix = "";
        for (int i = 0; i < level; i++)
        {
            prefix = prefix + "   ";
        }

        for (int index = 0; index < names.size(); index++)
        {
            if (isObject(index))
            {
                buffer.append(prefix).append("   \"").append(names.get(index)).append("\":{\n");
                getObject(index).dump(buffer, level + 1);
                buffer.append(prefix).append("   ").append("}\n");
            }
            else if (isArray(index))
            {
                buffer.append(prefix).append("   \"").append(names.get(index)).append("\":[\n");
                getArray(index).dump(buffer, level + 1);
                buffer.append(prefix).append("   ").append("]\n");
            }
            else
            {
                String value = values.get(index);
                buffer.append(prefix).append("   \"").append(names.get(index)).append("\":").append(value).append("\n");
            }
        }
    }
}
