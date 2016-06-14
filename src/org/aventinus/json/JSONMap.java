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
public class JSONMap
{
    //-------------------------------------------------------------------------------
    // Use of toolbox is dicey as it uses JSONObject to initialiase
    //-------------------------------------------------------------------------------
    private static Toolbox toolbox = Toolbox.toolbox();
    private static Logger logger = Logger.logger(JSONMap.class);

    private Map<String, String> map = new HashMap<String, String>();

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    public JSONMap()
    {
    }

    public JSONMap(JSONMap other)
    {
        map.putAll(other.map);
    }

    public JSONMap put(String name, Object value)
    {
        if ((name == null) || (name.length() == 0))
        {
            throw new JSONException("Invalid name [" + name + "]");
        }
        if (value != null)
        {
            throw new JSONException("Object value must be null");
        }

        map.put(name, "null");
        return this;
    }

    public JSONMap put(String name, String value)
    {
        if ((name == null) || (name.length() == 0))
        {
            throw new JSONException("Invalid name [" + name + "]");
        }
        if (value == null)
        {
            throw new JSONException("String value cannot be null");
        }

        map.put(name, JSONObject.escape(value));
        return this;
    }

    public JSONMap put(String name, int value)
    {
        if ((name == null) || (name.length() == 0))
        {
            throw new JSONException("Invalid name [" + name + "]");
        }

        map.put(name, Integer.toString(value));
        return this;
    }

    public JSONMap put(String name, long value)
    {
        if ((name == null) || (name.length() == 0))
        {
            throw new JSONException("Invalid name [" + name + "]");
        }

        map.put(name, Long.toString(value));
        return this;
    }

    public JSONMap put(String name, double value)
    {
        if ((name == null) || (name.length() == 0))
        {
            throw new JSONException("Invalid name [" + name + "]");
        }

        map.put(name, Double.toString(value));
        return this;
    }

    public JSONMap put(String name, boolean value)
    {
        if ((name == null) || (name.length() == 0))
        {
            throw new JSONException("Invalid name [" + name + "]");
        }

        map.put(name, value ? "true" : "false");
        return this;
    }

    public JSONMap put(String name, JSONObject value)
    {
        if ((name == null) || (name.length() == 0))
        {
            throw new JSONException("Invalid name [" + name + "]");
        }

        map.put(name, value.toString());
        return this;
    }

    public JSONMap put(String name, JSONArray value)
    {
        if ((name == null) || (name.length() == 0))
        {
            throw new JSONException("Invalid name [" + name + "]");
        }

        map.put(name, value.toString());
        return this;
    }

    public String toString()
    {
        StringBuilder buffer = new StringBuilder();

        // It is returned as JSONObject.toString() as that is what it really is

        buffer.append("{");
        boolean first = true;
        for (String name: map.values())
        {
            if (! first)
            {
                buffer.append(",");
            }
            first = false;
            buffer.append(JSONObject.escape(name));
            buffer.append(":");
            buffer.append(map.get(name));
        }
        buffer.append("}");

        return buffer.toString();
    }

    protected void putRaw(String name, String value)
    {
        map.put(name, value);
    }

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    public int size()
    {
        return map.size();
    }

    public void clear()
    {
        map.clear();
    }

    public Set<String> keySet()
    {
        return map.keySet();
    }

    public Collection<String> values()
    {
        return map.values();
    }

    public String remove(String name)
    {
        return map.remove(name);
    }

    public boolean isObject(String name)
    {
        String value = map.get(name);
        if (value == null)
        {
            throw new JSONException("Value does not exist");
        }

        return value.startsWith("{");
    }

    public boolean isArray(String name)
    {
        String value = map.get(name);
        if (value == null)
        {
            throw new JSONException("Value does not exist");
        }

        return value.startsWith("[");
    }

    public boolean isValue(String name)
    {
        if (isObject(name) || isArray(name))
        {
            return false;
        }
        return true;
    }

    public boolean containsKey(String name)
    {
        return (map.containsKey(name));
    }

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    public String getString(String name)
    {
        String value = map.get(name);
        if (value == null)
        {
            throw new JSONException("Value does not exist");
        }
        
        if (isObject(name) || isArray(name))
        {
            throw new JSONException("Not a value");
        }

        return JSONObject.unescape(value);
    }

    public String getString(String name, String defaultValue)
    {
        String value = map.get(name);
        if (value == null)
        {
            return defaultValue;
        }
        
        if (isObject(name) || isArray(name))
        {
            throw new JSONException("Not a value");
        }

        return new String(JSONObject.unescape(value));
    }

    public int getInteger(String name)
    {
        String value = map.get(name);
        if (value == null)
        {
            throw new JSONException("Value does not exist");
        }

        return JSONObject.parseInt(JSONObject.unescape(value));
    }

    public int getInteger(String name, int defaultValue)
    {
        String value = map.get(name);
        if (value == null)
        {
            return defaultValue;
        }

        return JSONObject.parseInt(JSONObject.unescape(value));
    }

    public long getLong(String name)
    {
        String value = map.get(name);
        if (value == null)
        {
            throw new JSONException("Value does not exist");
        }

        return JSONObject.parseLong(JSONObject.unescape(value));
    }

    public long getLong(String name, long defaultValue)
    {
        String value = map.get(name);
        if (value == null)
        {
            return defaultValue;
        }

        return JSONObject.parseLong(JSONObject.unescape(value));
    }

    public double getDouble(String name)
    {
        String value = map.get(name);
        if (value == null)
        {
            throw new JSONException("Value does not exist");
        }

        return JSONObject.parseDouble(JSONObject.unescape(value));
    }

    public double getDouble(String name, double defaultValue)
    {
        String value = map.get(name);
        if (value == null)
        {
            return defaultValue;
        }

        return JSONObject.parseDouble(JSONObject.unescape(value));
    }

    public boolean getBoolean(String name)
    {
        String value = map.get(name);
        if (value == null)
        {
            throw new JSONException("Value does not exist");
        }

        return JSONObject.unescape(value).equals("true");
    }

    public boolean getBoolean(String name, boolean defaultValue)
    {
        String value = map.get(name);
        if (value == null)
        {
            return defaultValue;
        }

        return JSONObject.unescape(value).equals("true");
    }

    public JSONObject getObject(String name)
    {
        String value = map.get(name);
        if (value == null)
        {
            throw new JSONException("Value does not exist");
        }

        if (! isObject(name))
        {
            throw new JSONException("Not an object");
        }

        return new JSONObject(value);
    }

    public JSONObject getObject(String name, JSONObject defaultValue)
    {
        String value = map.get(name);
        if (value == null)
        {
            return defaultValue;
        }

        if (! isObject(name))
        {
            throw new JSONException("Not an object");
        }

        return new JSONObject(value);
    }

    public JSONArray getArray(String name)
    {
        String value = map.get(name);
        if (value == null)
        {
            throw new JSONException("Value does not exist");
        }

        if (! isArray(name))
        {
            throw new JSONException("Not an array");
        }

        return new JSONObject(new StringBuilder("{a:").append(value).append("}").toString()).getArray("a");
    }

    public JSONArray getArray(String name, JSONArray defaultValue)
    {
        String value = map.get(name);
        if (value == null)
        {
            return defaultValue;
        }

        if (! isArray(name))
        {
            throw new JSONException("Not an array");
        }

        return new JSONObject(new StringBuilder("{a:").append(value).append("}").toString()).getArray("a");
    }

    public JSONMap getMap(String name)
    {
        String value = map.get(name);
        if (value == null)
        {
            throw new JSONException("Value does not exist");
        }

        if (! isObject(name))
        {
            throw new JSONException("Not an object");
        }

        return new JSONMap(new JSONObject(value).getMap());
    }

    public JSONMap getMap(String name, JSONMap defaultValue)
    {
        String value = map.get(name);
        if (value == null)
        {
            return defaultValue;
        }

        if (! isObject(name))
        {
            throw new JSONException("Not an object");
        }

        return new JSONMap(new JSONObject(value).getMap());
    }

    public JSONList getList(String name)
    {
        String value = map.get(name);
        if (value == null)
        {
            throw new JSONException("Value does not exist");
        }

        if (! isObject(name))
        {
            throw new JSONException("Not an object");
        }

        return new JSONObject(value).getList();
    }

    public JSONList getList(String name, JSONList defaultValue)
    {
        String value = map.get(name);
        if (value == null)
        {
            return defaultValue;
        }

        if (! isObject(name))
        {
            throw new JSONException("Not an object");
        }

        return new JSONObject(value).getList();
    }
    public String toString(String name)
    {
        return map.get(name);
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

        for (String name: map.values())
        {
            if (isObject(name))
            {
                buffer.append(prefix).append("   \"").append(name).append("\":{\n");
                getObject(name).dump(buffer, level + 1);
                buffer.append(prefix).append("   ").append("}\n");
            }
            else if (isArray(name))
            {
                buffer.append(prefix).append("   \"").append(name).append("\":[\n");
                getArray(name).dump(buffer, level + 1);
                buffer.append(prefix).append("   ").append("]\n");
            }
            else
            {
                String value = map.get(name);
                buffer.append(prefix).append("   \"").append(name).append("\":").append(value).append("\n");
            }
        }
    }
}
