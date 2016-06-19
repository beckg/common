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
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "JSONMap.h"
#include "JSONArray.h"
#include "JSONObject.h"
#include "Logger.h"

//-----------------------------------------------------------------------------------
//
//-----------------------------------------------------------------------------------
JSONMap::JSONMap()
{
}

JSONMap::~JSONMap()
{
}

JSONMap::JSONMap(const JSONMap& other)
{
    m_error = other.m_error;

    for (int i = 0; i < other.m_map.size(); i++)
    {
        m_map.put(other.m_map.getKey(i), other.m_map.get(i));
    }
}

JSONMap& JSONMap::operator=(const JSONMap& other)
{
    m_error = other.m_error;

    for (int i = 0; i < other.m_map.size(); i++)
    {
        m_map.put(other.m_map.getKey(i), other.m_map.get(i));
    }

    return *this;
}

//-----------------------------------------------------------------------------------
//
//-----------------------------------------------------------------------------------
JSONMap& JSONMap::put(const TextString& name, const TextString& value)
{
    m_map.put(name, JSONObject::escape(value));
    return *this;
}

JSONMap& JSONMap::put(const TextString& name, int value)
{
    m_map.put(name, value);
    return *this;
}

//JSONMap& JSONMap::put(const TextString& name, const TextString& name, long value)
//{
//    m_map.put(name, value);
//    return *this;
//}

//JSONMap& JSONMap::put(const TextString& name, double value)
//{
//    m_map.put(name, value);
//    return *this;
//}

//JSONMap& JSONMap::put(const TextString& name, boolean value)
//{
//    m_mapput(name, value ? "true" : "false");
//    return this;
//}

JSONMap& JSONMap::put(const TextString& name, const JSONObject& value)
{
    m_map.put(name, value.toString());
    return *this;
}

JSONMap& JSONMap::put(const TextString& name, const JSONMap& value)
{
    m_map.put(name, value.toString());
    return *this;
}

TextString JSONMap::toString() const
{
   TextString buffer;

    // It is returned as JSONObject.toString() as that is what it really is

    buffer.append("{");
    int first = 1;
    for (int i = 0; i < m_map.size(); i++)
    {
        const TextString& name = m_map.getKey(i);
        if (! first)
            buffer.append(",");
        first = 0;
        buffer.append(JSONObject::escape(name));
        buffer.append(":");
        buffer.append(m_map.get(name));
    }
    buffer.append("}");

    return buffer;
}

void JSONMap::putRaw(const TextString& name, const TextString& value)
{
    m_map.put(name, value);
}

//-------------------------------------------------------------------------------
//
//-------------------------------------------------------------------------------
int JSONMap::size() const
{
    return m_map.size();
}

int JSONMap::isObject(const TextString& name) const
{
    return m_map.get(name).startsWith("{");
}

int JSONMap::isArray(const TextString& name) const
{
    return m_map.get(name).startsWith("[");
}

int JSONMap::isValue(const TextString& name) const
{
    if (isObject(name) || isArray(name))
        return 0;
    return 1;
}

const TextString& JSONMap::getName(int index) const
{
    return m_map.getKey(index);
}

int JSONMap::contains(const TextString& name) const
{
    for (int i = 0; i < m_map.size(); i++)
    {
        if (m_map.getKey(i).equals(name))
            return 1;
    }

    return 0;
}

//-------------------------------------------------------------------------------
//
//-------------------------------------------------------------------------------
TextString JSONMap::getString(const TextString& name) const
{
    TextString value;
    if (getRaw(name, value) < 0)
        abort(); // throw new JSONException("Value does not exist");

    if (value.startsWith("{") || value.startsWith("["))
        abort(); // throw new JSONException("Not a value");

    return JSONObject::unescape(value);
}

TextString JSONMap::getString(const TextString& name, const TextString& defaultValue) const
{
    TextString value;
    if (getRaw(name, value) < 0)
        return defaultValue;

    if (value.startsWith("{") || value.startsWith("["))
        abort(); // throw new JSONException("Not a value");

    return JSONObject::unescape(value);
}

// need defaults

int JSONMap::getInteger(const TextString& name, int& target) const
{
    TextString value;
    if (getRaw(name, value) < 0)
        abort(); // throw new JSONException("Value does not exist");

    if (value.startsWith("{") || value.startsWith("["))
        abort(); // throw new JSONException("Not a value");

    return value.getInteger(target);
}

int JSONMap::getInteger(const TextString& name, int& target, int defaultValue) const
{
    TextString value;
    if (getRaw(name, value) < 0)
        return defaultValue;

    if (value.startsWith("{") || value.startsWith("["))
        abort(); // throw new JSONException("Not a value");

    return value.getInteger(target);
}

//int JSONMap::getLong(const TextString& name, long& value) const
//{
//    return getString(index).getLong(value);
//}

//int JSONMap::getDouble(const TextString& name, double& value) const
//{
//    return getString(index).getDouble(value);
//}

JSONObject& JSONMap::getObject(const TextString& name, JSONObject& target) const
{
    TextString value;
    if (getRaw(name, value) < 0)
        abort(); // throw new JSONException("Value does not exist");

    if (! value.startsWith("{"))
    {
        target.m_error = "Not an object";
        return target;
    }

    target = JSONObject(value);
    return target;
}

JSONMap& JSONMap::getMap(const TextString& name, JSONMap& target) const
{
    TextString value;
    if (getRaw(name, value) < 0)
        abort(); // throw new JSONException("Value does not exist");

    if (! value.startsWith("{"))
    {
        target.m_error = "Not an object";
        return target;
    }

    JSONObject(value).getMap(target);
    return target;
}

JSONArray& JSONMap::getArray(const TextString& name, JSONArray& target) const
{
    TextString value;
    if (getRaw(name, value) < 0)
        abort(); // throw new JSONException("Value does not exist");

    if (! value.startsWith("["))
    {
        target.m_error = "Not an array";
        return target;
    }

    JSONObject object(TextString("{a:").append(value).append("}"));

    object.getArray("a", target);
    return target;
}

const TextString& JSONMap::toString(int index) const
{
    return m_map.get(index);
}

int JSONMap::getRaw(const TextString& name, TextString& target) const
{
    for (int i = 0; i < m_map.size(); i++)
    {
        if (m_map.getKey(i).equals(name))
        {
            target = m_map.get(i);
            return 0;
        }
    }

    return -1;
}

//-------------------------------------------------------------------------------
//
//-------------------------------------------------------------------------------
void JSONMap::dump() const
{
    dump("");
}

void JSONMap::dump(const TextString& text) const
{
     TextString buffer;
     dump(buffer, 1);
     Logger::instance().log(TextString("JSONMap dump(").append(text).append("): \n").append(buffer));
}

void JSONMap::dump(TextString buffer, int level) const
{
    TextString prefix = "";
    for (int i = 0; i < level; i++)
        prefix.append("   ");

    for (int i = 0; i < m_map.size(); i++)
    {
        const TextString& name = m_map.getKey(i);

        if (m_map.get(i).startsWith("{"))
        {
            buffer.append(prefix).append("   ").append("{\n");
            JSONObject object;
            getObject(name, object).dump(buffer, level + 1);
            buffer.append(prefix).append("   ").append("}\n");
        }
        else if (m_map.get(i).startsWith("["))
        {
            buffer.append(prefix).append("   ").append("[\n");
            JSONArray array;
            getArray(name, array).dump(buffer, level + 1);
            buffer.append(prefix).append("   ").append("]\n");
        }
        else
            buffer.append(prefix).append("   ").append(m_map.get(i)).append("\n");
    }
}
