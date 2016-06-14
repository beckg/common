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

#include "JSONArray.h"
#include "JSONObject.h"
#include "Logger.h"

//-----------------------------------------------------------------------------------
//
//-----------------------------------------------------------------------------------
JSONArray::JSONArray()
{
}

JSONArray::~JSONArray()
{
}

JSONArray::JSONArray(const JSONArray& other)
{
    m_error = other.m_error;

    for (int i = 0; i < other.m_list.size(); i++)
    {
        m_list.add(new TextString(other.m_list.get(i)));
    }
}

JSONArray& JSONArray::operator=(const JSONArray& other)
{
    m_error = other.m_error;

    for (int i = 0; i < other.m_list.size(); i++)
    {
        m_list.add(new TextString(other.m_list.get(i)));
    }

    return *this;
}

//-----------------------------------------------------------------------------------
//
//-----------------------------------------------------------------------------------
JSONArray& JSONArray::add(const TextString& value)
{
    m_list.add(new TextString(JSONObject::escape(value)));
    return *this;
}


JSONArray& JSONArray::add(int value)
{
    m_list.add(new TextString(value));
    return *this;
}

//JSONArray& JSONArray::add(long value)
//{
//    m_list.add(new TextString(value));
//    return *this;
//}

//JSONArray& JSONArray::add(double value)
//{
//    m_list.add(new TextString(value));
//    return *this;
//}

//JSONArray& JSONArray::add(boolean value)
//{
//    m_list.add(value ? "true" : "false");
//    return this;
//}

JSONArray& JSONArray::add(const JSONObject& value)
{
    m_list.add(new TextString(value.toString()));
    return *this;
}

JSONArray& JSONArray::add(const JSONArray& value)
{
    m_list.add(new TextString(value.toString()));

    return *this;
}

TextString JSONArray::toString() const
{
    TextString buffer;

    buffer.append("[");
    int first = 1;
    for (int i = 0; i < m_list.size(); i++)
    {
        if (! first)
            buffer.append(",");
        first = 0;
        buffer.append(m_list.get(i));
    }
    buffer.append("]");

    return buffer;
}

void JSONArray::addRaw(const TextString& value)
{
    m_list.add(new TextString(value));
}

//-------------------------------------------------------------------------------
//
//-------------------------------------------------------------------------------
int JSONArray::size() const
{
    return m_list.size();
}

int JSONArray::isObject(int index) const
{
    return m_list.get(index).startsWith("{");
}

int JSONArray::isArray(int index) const
{
    return m_list.get(index).startsWith("[");
}

int JSONArray::isValue(int index) const
{
    if (isObject(index) || isArray(index))
        return 0;
    return 1;
}

// We do not have default methods as the value must exist in the array !

TextString JSONArray::getString(int index) const
{
    if (isObject(index) || isArray(index))
        abort(); // throw new JSONException("Not a value");

    const TextString& value = m_list.get(index);

    return JSONObject::unescape(value);
}

int JSONArray::getInteger(int index, int& value) const
{
    return getString(index).getInteger(value);
}

//int JSONArray::getLong(int index, long& value) const
//{
//    return getString(index).getLong(value);
//}

//int JSONArray::getDouble(int index, double& value) const
//{
//    return getString(index).getDouble(value);
//}

JSONObject& JSONArray::getObject(int index, JSONObject& target) const
{
    if (! isObject(index))
    {
        target.m_error = "Not an object";
        return target;
    }

    target = m_list.get(index);

    return target;
}

JSONArray& JSONArray::getArray(int index, JSONArray& target) const
{
    if (! isArray(index))
    {
        target.m_error = "Not an array";
        return target;
    }

    JSONObject object(TextString("{a:").append(m_list.get(index)).append("}"));

    object.getArray("a", target);

    return target;
}

//-------------------------------------------------------------------------------
//
//-------------------------------------------------------------------------------
const TextString& JSONArray::toString(int index) const
{
    return m_list.get(index);
}

void JSONArray::normalise(JSONArray& target) const
{
    for (int entry = 0; entry < size(); entry++)
    {
        if (isObject(entry))
        {
            JSONObject object;
            JSONObject temp;
            getObject(entry, object).normalise(temp);
            target.add(temp);
        }
        else if (isArray(entry))
        {
            JSONArray array;
            JSONArray temp;
            getArray(entry, array).normalise(temp);
            target.add(temp);
        }
        else
        {
            target.addRaw(toString(entry));
        }
    }
}

//-------------------------------------------------------------------------------
//
//-------------------------------------------------------------------------------
void JSONArray::dump() const
{
    dump("");
}

void JSONArray::dump(const TextString& text) const
{
     TextString buffer;
     dump(buffer, 1);
     Logger::instance().log(TextString("JSONArray dump(").append(text).append("): \n").append(buffer));
}

void JSONArray::dump(TextString buffer, int level) const
{
    TextString prefix = "";
    for (int i = 0; i < level; i++)
        prefix.append("   ");

    for (int i = 0; i < m_list.size(); i++)
    {
        if (isObject(i))
        {
            buffer.append(prefix).append("   ").append("{\n");
            JSONObject object;
            getObject(i, object).dump(buffer, level + 1);
            buffer.append(prefix).append("   ").append("}\n");
        }
        else if (isArray(i))
        {
            buffer.append(prefix).append("   ").append("[\n");
            JSONArray array;
            getArray(i, array).dump(buffer, level + 1);
            buffer.append(prefix).append("   ").append("]\n");
        }
        else
            buffer.append(prefix).append("   ").append(m_list.get(i)).append("\n");
    }
}
