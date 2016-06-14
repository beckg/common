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

#include "Logger.h"
#include "JSONArray.h"
#include "JSONMap.h"
#include "JSONObject.h"

//-----------------------------------------------------------------------------------
//
//-----------------------------------------------------------------------------------
static const int STACK_KEY = 1;
static const int STACK_OBJECT = 2;
static const int STACK_ARRAY = 3;
static const int STACK_VALUE = 4;

int JSONObject::m_verbose = 0;

#define MAX_STACK 40

//-----------------------------------------------------------------------------------
//
//-----------------------------------------------------------------------------------
JSONObject::JSONObject()
{
    m_buffer = "{}";

    m_start = 0;
    m_end = -1;

    m_stack = 0;
    m_stackIdx = -1;
}

JSONObject::JSONObject(const TextString& input)
{
    m_buffer = input;

    m_start = 0;
    m_end = -1;

    m_stack = 0;
    m_stackIdx = -1;

    m_start = 0;
    while(1)
    {
        if (m_start >= m_buffer.length())
        {
            m_error = "Invalid JSON - no input";
            return;
        }

        char inputc = m_buffer.charAt(m_start);

        if (inputc == '{')
            break;
        if (inputc > ' ')
        {
            m_error = "Invalid JSON - invalid leading characters";
            return;
        }
        m_start++;
    }                

    m_end = parse(m_start, (JSONRequest*)0);
    if (m_end < 0)
        return;

    for (int i = m_end; i < m_buffer.length(); i++)
    {
        char inputc = m_buffer.charAt(i);

        if (inputc > ' ')
        {
            m_error = "Invalid JSON - invalid trailing characters";
            return;
        }
    }
}

JSONObject::~JSONObject()
{
    if (m_stack != 0)
        delete m_stack;
}

JSONObject::JSONObject(const JSONObject& other)
{
    m_buffer = other.m_buffer;

    m_start = other.m_start;
    m_end = other.m_end;

    m_error = other.m_error;
    m_stack = 0;
    m_stackIdx = -1;

    if (other.m_stackIdx >= 0)
    {
        m_stackIdx = other.m_stackIdx;
        m_stack = new int[MAX_STACK];
        for (int i = 0; i < MAX_STACK; i++)
            m_stack[i] = other.m_stack[i];
    }
}

JSONObject& JSONObject::operator=(const JSONObject& other)
{
    m_buffer = other.m_buffer;

    m_start = other.m_start;
    m_end = other.m_end;

    m_error = other.m_error;
    m_stack = 0;
    m_stackIdx = -1;

    if (other.m_stackIdx >= 0)
    {
        m_stackIdx = other.m_stackIdx;
        if (m_stack != 0)
            delete m_stack;
        m_stack = new int[MAX_STACK];
        for (int i = 0; i < MAX_STACK; i++)
            m_stack[i] = other.m_stack[i];
    }

    return *this;
}

//-----------------------------------------------------------------------------------
//
//-----------------------------------------------------------------------------------
static int CONTAINS = 2;
static int GET_VALUE = 3;
static int GET_OBJECT = 4;
static int GET_ARRAY = 5;
static int GET_NAMES = 6;
static int GET_MAP = 7;
static int REMOVE_VALUE = 8;
static int REMOVE_ARRAY = 9;
static int REMOVE_OBJECT = 10;

class JSONRequest
{
   public:
       JSONRequest()
       {
           m_request = 0;
           m_response = 0;

           m_array = 0;
           m_map = 0;
           m_names = 0;
       }
       ~JSONRequest()
       {
           if (m_array != 0)
               delete m_array;
           if (m_map != 0)
               delete m_map;
           if (m_names != 0)
               delete m_names;
       }

       int m_request;
       int m_response;

       TextString m_name;
       TextString m_value;

       JSONArray* m_array;
       JSONMap* m_map;
       List<TextString>* m_names;

    private:
        JSONRequest(const JSONRequest& other);
        JSONRequest& operator=(const JSONRequest& other);
};

//-----------------------------------------------------------------------------------
//
//-----------------------------------------------------------------------------------
JSONObject& JSONObject::add(const TextString& name, const TextString& value)
{
    return addRaw(name,  JSONObject::escape(value));
}

JSONObject& JSONObject::add(const TextString& name, int value)
{
    return addRaw(name,  TextString(value));
}

JSONObject& JSONObject::add(const TextString& name, long value)
{
    return addRaw(name,  TextString(value));
}

//JSONObject& JSONObject::add(const TextString& name, double value)
//{
//    return addRaw(name,  TextString(value));
//}

JSONObject& JSONObject::add(const TextString& name, const JSONObject& value)
{
    if (m_error.length() > 0)
        return *this;

    if (m_stackIdx < 0)
    {
        m_buffer.setLength(m_buffer.length() - 1); // remove outer }

        if (m_buffer.length() > 1)
            m_buffer.append(",");
        m_buffer.append(escape(name)).append(":").append(value.toString());

        m_buffer.append("}"); // replace out }
    }
    else if (m_stackIdx == 0)
    {
        m_error = "Invalid state";
        return *this;
    }
    else
    {

        if (m_stack[m_stackIdx] == STACK_OBJECT)
            m_stack[++m_stackIdx] = STACK_VALUE;
        else if (m_stack[m_stackIdx] == STACK_ARRAY)
            m_stack[++m_stackIdx] = STACK_VALUE;
        else if (m_stack[m_stackIdx] == STACK_VALUE)
        {
            m_buffer.append(",");
        }
        else
        {
            m_error = "Invalid state";
            return *this;
        }

        m_buffer.append(escape(name)).append(":").append(value.toString());
    }

    return *this;
}

JSONObject& JSONObject::add(const TextString& name, const JSONArray& value)
{
    if (m_error.length() > 0)
        return *this;

    if (m_stackIdx < 0)
    {
        m_buffer.setLength(m_buffer.length() - 1); // remove outer }

        if (m_buffer.length() > 1)
            m_buffer.append(",");
        m_buffer.append(escape(name)).append(":").append(value.toString());

        m_buffer.append("}"); // replace out }
    }
    else if (m_stackIdx == 0)
    {
        m_error = "Invalid state";
        return *this;
    }
    else
    {
        if (m_stack[m_stackIdx] == STACK_OBJECT)
            m_stack[++m_stackIdx] = STACK_ARRAY;
        else if (m_stack[m_stackIdx] == STACK_VALUE)
        {
            m_buffer.append(",");
        }
        else
        {
            m_error = "Invalid state";
            return *this;
        }

        m_buffer.append(escape(name)).append(":").append(value.toString());
    }

    return *this;
}

JSONObject& JSONObject::addRaw(const TextString& name, const TextString& value)
{
    if (m_error.length() > 0)
        return *this;

    if (m_stackIdx < 0)
    {
        m_buffer.setLength(m_buffer.length() - 1); // remove outer }

        if (m_buffer.length() > 1)
            m_buffer.append(",");
        m_buffer.append(escape(name)).append(":").append(value);

        m_buffer.append("}"); // replace out }
    }
    else if (m_stackIdx == 0)
    {
        m_error = "Invalid state";
        return *this;
    }
    else
    {
        if (m_stack[m_stackIdx] == STACK_OBJECT)
            m_stack[++m_stackIdx] = STACK_VALUE;
        else if (m_stack[m_stackIdx] == STACK_ARRAY)
            m_stack[++m_stackIdx] = STACK_VALUE;
        else if (m_stack[m_stackIdx] == STACK_VALUE)
            m_buffer.append(",");
        else
        {
            m_error = "Invalid state";
            return *this;
        }

        m_buffer.append(escape(name)).append(":").append(value);
    }

    return *this;
}

void JSONObject::removeValue(const TextString& name, TextString& target)
{
    if (m_error.length() > 0)
        return;

    if (name.length() == 0)
    {
        m_error = "Invalid name";
        return;
    }

    if (m_stackIdx >= 0)
    {
        m_error = "Object is open pending additions";
        return;
    }

    JSONRequest request;
    request.m_request = REMOVE_VALUE;
    request.m_name = name;

    parse(m_start, &request);

    if (! request.m_response)
    {
        m_error = "Did not find value";
        return;
    }

    target = JSONObject::unescape(request.m_value);
}

void JSONObject::removeObject(const TextString& name, JSONObject& target)
{
    if (m_error.length() > 0)
        return;

    if (name.length() == 0)
    {
        m_error = "Invalid name";
        target.m_error = "Invalid name";
        return;
    }

    if (m_stackIdx >= 0)
    {
        m_error = "Object is open pending additions";
        target.m_error = "Object is open pending additions";
        return;
    }

    JSONRequest request;
    request.m_request = REMOVE_OBJECT;
    request.m_name = name;

    parse(m_start, &request);

    if (! request.m_response)
    {
        m_error = "Did not find object";
        target.m_error = "Did not find object";
        return;
    }

    target = request.m_value;
}

void JSONObject::removeArray(const TextString& name, JSONArray& target)
{
    if (m_error.length() > 0)
        return;

    if (name.length() == 0)
    {
        m_error = "Invalid name";
        target.m_error = "Invalid name";
        return;
    }

    if (m_stackIdx >= 0)
    {
        m_error = "Object is open pending additions";
        target.m_error = "Object is open pending additions";
        return;
    }

    JSONRequest request;
    request.m_request = REMOVE_ARRAY;
    request.m_name = name;

    parse(m_start, &request);

    if (! request.m_response)
    {
        m_error = "Did not find array";
        target.m_error = "Did not find array";
        return;
    }

    JSONObject object(TextString("{a:").append(request.m_value).append("}"));
    object.getArray("a", target);
}

//-----------------------------------------------------------------------------------
//
//-----------------------------------------------------------------------------------
JSONObject& JSONObject::startObject()
{
    if (m_stackIdx <= 0)
    {
        m_error = "Invalid startObject()";
        return *this;
    }
    else
    {
        if (m_stack[m_stackIdx] == STACK_KEY)
            m_stack[m_stackIdx] = STACK_OBJECT;
        else if (m_stack[m_stackIdx] == STACK_ARRAY)
            m_stack[++m_stackIdx] = STACK_OBJECT;
        else if ((m_stack[m_stackIdx] == STACK_VALUE) && (m_stack[m_stackIdx - 1] == STACK_ARRAY))
        {
            m_buffer.append(",");               
            m_stack[m_stackIdx] = STACK_OBJECT;
        }
        else
        {
            m_error = "Invalid startObject()";
            return *this;
        }

        m_buffer.append("{");
    }

    return *this;
}

JSONObject& JSONObject::endObject()
{
    if (m_stackIdx <= 0)
    {
        m_error = "Invalid endObject()";
        return *this;
    }
    else
    {
        if (m_stack[m_stackIdx] == STACK_OBJECT)
            m_stack[m_stackIdx] = STACK_VALUE;
        else if ((m_stackIdx > 1) && (m_stack[m_stackIdx] == STACK_VALUE) && (m_stack[m_stackIdx - 1] == STACK_OBJECT))
        {
            m_stackIdx--;
            m_stack[m_stackIdx] = STACK_VALUE;
        }
        else
        {
            m_error = "Invalid endObject()";
            return *this;
        }

        m_buffer.append("}");

        if (m_stackIdx == 1)
        {
            m_buffer.append("}"); // replace outer }
            m_stackIdx = -1;
        }
    }

    return *this;
}

JSONObject& JSONObject::startArray()
{
    if (m_stackIdx <= 0)
    {
        m_error = "Invalid startArray()";
        return *this;
    }
    else
    {
        if (m_stack[m_stackIdx] == STACK_KEY)
            m_stack[m_stackIdx] = STACK_ARRAY;
        else if (m_stack[m_stackIdx] == STACK_ARRAY)
            m_stack[++m_stackIdx] = STACK_ARRAY;
        else if ((m_stack[m_stackIdx] == STACK_VALUE) && (m_stack[m_stackIdx - 1] == STACK_ARRAY))
        {
            m_stack[m_stackIdx] = STACK_ARRAY;
            m_buffer.append(",");
        }
        else
        {
            m_error = "Invalid startArray()";
            return *this;
        }

        m_buffer.append("[");
    }

    return *this;
}

JSONObject& JSONObject::endArray()
{     
    if (m_stackIdx <= 0)
    {
        m_error = "Invalid endArray()";
        return *this;
    }
    else
    {
        if (m_stack[m_stackIdx] == STACK_ARRAY)
            m_stack[m_stackIdx] = STACK_VALUE;
        else if ((m_stack[m_stackIdx] == STACK_VALUE) && (m_stack[m_stackIdx - 1] == STACK_ARRAY))
        {
            m_stackIdx--;
            m_stack[m_stackIdx] = STACK_VALUE;
        }
        else
        {
            m_error = "Invalid endArray()";
            return *this;
        }
 
        m_buffer.append("]");

        if (m_stackIdx == 1)
        {
            m_buffer.append("}"); // replace outer }
            m_stackIdx = -1;
        }
    }

    return *this;
}

JSONObject& JSONObject::key(const TextString& key)
{
    if (key.length() == 0)
    {
        m_error = "Invalid key";
        return *this;
    }

    if (m_stack == 0)
    {
        m_stack = new int[MAX_STACK];
        if (m_stack == 0)
            abort();
        m_stackIdx = -1;
    }

    if (m_stackIdx < 0)
    {
        m_stack[++m_stackIdx] = STACK_OBJECT;
        m_stack[++m_stackIdx] = STACK_KEY;
        m_buffer.setLength(m_buffer.length() - 1); // remove outer }
        if (m_buffer.length() > 1)
            m_buffer.append(",");

        m_buffer.append(key).append(":");
    }
    else if (m_stackIdx == 0)
    {
        m_error = "Invalid key()";
        return *this;
    }
    else
    {
        if (m_stack[m_stackIdx] == STACK_OBJECT)
            m_stack[++m_stackIdx] = STACK_KEY;
        else if (m_stack[m_stackIdx] == STACK_VALUE)
        {
            if (m_stack[m_stackIdx - 1] == STACK_ARRAY)
            {
                m_error = "Invalid key()";
                return *this;
            }

            m_stack[m_stackIdx] = STACK_KEY;
            m_buffer.append(",");
        }
        else
        {
            m_error = "Invalid key()";
            return *this;
        }

        m_buffer.append(key).append(":");
    }

    return *this;
}

JSONObject& JSONObject::value(const TextString& value)
{
    return rawValue(JSONObject::escape(value));
}

JSONObject& JSONObject::value(int value)
{
    return rawValue(TextString(value));
}

JSONObject& JSONObject::value(long value)
{
    return rawValue(TextString(value));
}


//JSONObject& JSONObject::value(double value)
//{
//    return rawValue(TextString(value));
//}

JSONObject& JSONObject::rawValue(const TextString& value)
{
    if (value.length() == 0)
    {
        m_error = "Invalid value";
        return *this;
    }

    if (m_stackIdx <= 0)
    {
        m_error = "Invalid value()";
        return *this;
    }
    else
    {
        if (m_stack[m_stackIdx] == STACK_KEY)
            m_stack[m_stackIdx] = STACK_VALUE;
        else if (m_stack[m_stackIdx] == STACK_ARRAY)
        {
            m_stack[++m_stackIdx] = STACK_VALUE;
        }
        else if ((m_stack[m_stackIdx] == STACK_VALUE) && (m_stack[m_stackIdx - 1] == STACK_ARRAY))
        {
            m_buffer.append(",");
        }
        else
        {
            m_error = "Invalid value()";
            return *this;
        }

        m_buffer.append(value);

        if (m_stackIdx == 1)
        {
            m_buffer.append("}"); // replace outer }
            m_stackIdx = -1;
        }
    }

    return *this;
}

void JSONObject::dumpStack() const
{
    Logger::instance().log(TextString("buffer=").append(m_buffer));
    for (int i = 0; i < m_stackIdx; i++)
        Logger::instance().log(TextString("stack[").append(i).append("]=").append(m_stack[i]));
}

//-----------------------------------------------------------------------------------
//
//-----------------------------------------------------------------------------------
int JSONObject::contains(const TextString& name) const
{
    if (name.length() == 0)
        return 0;

    if (m_stackIdx >= 0)
        return 0;

    JSONRequest request;
    request.m_request = CONTAINS;
    request.m_name = name;

    ((JSONObject*)this)->parse(m_start, &request); // I know that this request is const !

    if (! request.m_response)
        return 0;
    return 1;
}

TextString JSONObject::getString(const TextString& name) const
{
    TextString value;
    if (getValue(name, GET_VALUE, value) < 0)
        abort();

    return unescape(value);
}

TextString JSONObject::getString(const TextString& name, const TextString& defaultValue) const
{
    TextString value;
    if (getValue(name, GET_VALUE, value) < 0)
        return defaultValue;

    return unescape(value);
}

int JSONObject::getInteger(const TextString& name, int& target) const
{
    TextString value;
    if (getValue(name, GET_VALUE, value) < 0)
        return -1;

    return unescape(value).getInteger(target);
}

int JSONObject::getInteger(const TextString& name, int& target, int defaultValue) const
{
    TextString value;
    if (getValue(name, GET_VALUE, value) < 0)
    {
        target = defaultValue;
        return 0;
    }

    return unescape(value).getInteger(target);
}

int JSONObject::getLong(const TextString& name, long& target) const
{
    TextString value;
    if (getValue(name, GET_VALUE, value) < 0)
        return -1;

    return unescape(value).getLong(target);
}

int JSONObject::getLong(const TextString& name, long& target, long defaultValue) const
{
    TextString value;
    if (getValue(name, GET_VALUE, value) < 0)
    {
        target = defaultValue;
        return 0;
    }

    return unescape(value).getLong(target);
}

//int JSONObject::getDouble(const TextString& name, double& target) const
//{
//    JSONValue value;
//    return getValue(name, value).getDouble(target);
//}

JSONObject& JSONObject::getObject(const TextString& name, JSONObject& target) const
{   
    TextString value;
    if (getValue(name, GET_OBJECT, value) < 0)
        abort();

    target = JSONObject(value);
    return target;
}

JSONArray& JSONObject::getArray(const TextString& name, JSONArray& target) const
{
    if (name.length() == 0)
    {
        target.m_error = "Invalid name";
        return target;
    }

    if (m_stackIdx >= 0)
    {
        target.m_error = "Object is open pending additions";
        return target;
    }

    JSONRequest request;
    request.m_request = GET_ARRAY;
    request.m_name = name;

    ((JSONObject*)this)->parse(m_start, &request); // I know that this request is const !

    if (! request.m_response)
    {
        target.m_error = "Did not find array";
        return target;
    }

    if (request.m_array == 0)
    {
        target.m_error = "Not an array";
        return target;
    }

    target = *request.m_array;
    return target;
}

// This one will also find arrays but it cannot return them !

int JSONObject::getValue(const TextString& name, int type, TextString& target) const
{
    if (name.length() == 0)
    {
//        target.m_error = "Invalid name";
        return -1;
    }

    if (m_stackIdx >= 0)
    {
//        target.m_error = "Object was open pending additions";
        return -1;
    }

    JSONRequest request;
    request.m_request = type;
    request.m_name = name;

    ((JSONObject*)this)->parse(m_start, &request); // I know that this request is const !

    if (! request.m_response)
    {
//        target.m_error = "Did not find value";
        return -1;
    }

    target = request.m_value;
    return 0;
}

JSONMap& JSONObject::getMap(JSONMap& target) const
{
    if (m_stackIdx >= 0)
    {
//        target.m_error = "Object was open pending additions";
        return target;
    }

    JSONRequest request;
    request.m_request = GET_MAP;
    request.m_map = &target;

    ((JSONObject*)this)->parse(m_start, &request); // I know that this request is const !

    request.m_map = 0;

    return target;
}

List<TextString>& JSONObject::getNames(List<TextString>& target) const
{
    if (m_stackIdx >= 0)
    {
//        target.m_error = "Object was open pending additions";
        return target;
    }

    JSONRequest request;
    request.m_request = GET_NAMES;
    request.m_names = &target;

    ((JSONObject*)this)->parse(m_start, &request); // I know that this request is const !

    request.m_names = 0;

    return target;
}

//-----------------------------------------------------------------------------------
//
//-----------------------------------------------------------------------------------
const TextString& JSONObject::toString() const
{
    // The abort()s mean that I cannot use this to create (invalid) messages

    if (m_error.length() > 0)
        abort();

    if (m_stackIdx >= 0)
        abort();

    return m_buffer;
}

void JSONObject::normalise(JSONObject& target) const
{
    JSONMap map;
    getMap(map);

    for (int entry = 0; entry < map.size(); entry++)
    {
        TextString name = map.getName(entry);

        if (map.isObject(name))
        {
            JSONObject object;
            JSONObject temp;
            map.getObject(name, object).normalise(temp);
            target.add(name, temp);
        }
        else if (map.isArray(name))
        {
            JSONArray array;
            JSONArray temp;
            map.getArray(name, array).normalise(temp);
            target.add(name, temp);
        }
        else
        {
            target.addRaw(name, map.toString(entry));
        }
    }
}

//-----------------------------------------------------------------------------------
//
//-----------------------------------------------------------------------------------
void JSONObject::dump() const
{
    dump("");
}

void JSONObject::dump(const TextString& text) const
{
     TextString buffer;
     dump(buffer, 0);
     Logger::instance().log(TextString("JSONObject dump(").append(text).append("): \n").append(buffer));
}

void JSONObject::dump(TextString& buffer, int level) const
{
    if (m_stackIdx >= 0)
    {
        buffer.append("Object is open pending additions").append("\n");
        return;
    }

    TextString prefix = "";
    for (int i = 0; i < level; i++)
        prefix.append("   ");

    if (level == 0)
        buffer.append(prefix).append("{\n");

    JSONMap map; 
    getMap(map);
    List<TextString> names;
    getNames(names);
    for (int i = 0; i < names.size(); i++)
    {
        TextString name = names.get(i);

        if (map.isObject(name))
        {
            buffer.append(prefix).append("   \"").append(name).append("\":{\n");
            JSONObject target;
            map.getObject(name, target).dump(buffer, level + 1);
            buffer.append(prefix).append("   ").append("}\n");
        }
        else if (map.isArray(name))
        {
            buffer.append(prefix).append("   \"").append(name).append("\":[\n");
            JSONArray target;
            map.getArray(name, target).dump(buffer, level + 1);
            buffer.append(prefix).append("   ").append("]\n");
        }
        else
            buffer.append(prefix).append("   ").append("\"").append(name).append("\":")
                                 .append(map.toString(i)).append("\n");
    }

    if (level == 0)
        buffer.append(prefix).append("}\n");
}

//-----------------------------------------------------------------------------------
//
//-----------------------------------------------------------------------------------
int JSONObject::length() const
{
    return m_buffer.length();
}

char JSONObject::charAt(int index) const
{
    return m_buffer.charAt(index);
}

TextString JSONObject::substring(int start, int end) const
{
    return m_buffer.substring(start, end);
}

int JSONObject::match(const JSONRequest& request, int start, int end) const
{
    if (request.m_name.length() == 0)
        return 1;

    if ((charAt(start) == '"') && (charAt(end - 1) == '"'))
    {
        start++;
        end--;
    }

    if (request.m_name.length() != end - start)
        return 0;

    for (int i = 0; i < end - start; i++)
    {
        if (request.m_name.charAt(i) != charAt(start + i))
            return 0;
    }

    return 1;
}

void JSONObject::setError(const TextString& text, int current)
{
    int start = 0;
    int end = m_buffer.length();

    if (end - current > 40)
        end = current + 40;
    if (current - start > 40)
        start = current - 40;

    m_error = text; 
    m_error.append(" [").append(m_buffer.substring(start, current))
           .append("] ! [").append(m_buffer.substring(current, end)).append("]");
}

int JSONObject::parse2(int start, JSONRequest* request)
{
    if (m_verbose)
        Logger::instance().log("up");

    int resp = parse(start, request);

    if (m_verbose)
        Logger::instance().log("down");

    return resp;
}

int JSONObject::parse(int start, JSONRequest* request)
{
    char inquote = ' ';

    int nameStart = -1;
    int nameEnd = -1;
    int colon = 0;
    int valueStart = -1;
    int valueEnd = -1;
    int seenComma = 0;

    int current = start + 1;
    while (1)
    {
        if (m_verbose)
        {
            Logger::instance().log(TextString("start[").append(m_buffer.substring(start, start + 1))
                                   .append("] cur[").append(current)
                                   .append("] inputc[").append(current >= length() ? "eos" : m_buffer.substring(current, current + 1))
                                   .append("] nStart[").append(nameStart)
                                   .append("] nEnd[").append(nameEnd)
                                   .append("] :[").append(colon)
                                   .append("] vStart[").append(valueStart)
                                   .append("] vEnd[").append(valueEnd)
                                   .append("] seenComma[").append(seenComma)
                                   .append("] req[").append((request != 0))
                                   .append("]"));
        }

        // We need at least one more character to differentiate between real end-of-string
        //    and an escape sequence. We are guaranteed for there to be one
        //    as the input must be terminated by '}' or ']'

        if ((inquote != ' ') && (current >= length() - 1))
        {
            setError("Invalid JSON - quoting", current);
            return -1;
        }
        else if (current >= length())
        {
            setError("Invalid JSON - early end", current);
            return -1;
        }

        char inputc = charAt(current);

        if (inquote != ' ')
        {
            if ((inputc == '\\') && (charAt(current + 1) == '\\'))
            {
                current += 2;
                continue;
            }

            if ((inputc == '\\') && (charAt(current + 1) == '"'))
            {
                current += 2;
                continue;
            }

            if (inputc == inquote)
            {
                if (charAt(start) == '{')
                {
                    if (nameEnd < 0)
                        nameEnd = current + 1;
                    else
                        valueEnd = current + 1;                   
                }
                else
                    valueEnd = current + 1;
                inquote = ' ';
                current += 1;
                continue;
            }

            current += 1;
            continue;
        }

        if ((inputc == ' ') || (inputc == '\n') || (inputc == '\r') || (inputc == '\t'))
        {
            if ((nameStart >= 0) && (nameEnd < 0))
                nameEnd = current;
            if ((valueStart >= 0) && (valueEnd < 0))
                valueEnd = current;
            current += 1;
            continue;
        }

        if (inputc < ' ')
        {
            setError("Invalid JSON - invalid character", current);
            return -1;
        }

        if ((charAt(current) == '/') && (current < length() - 1) && (charAt(current + 1) == '/'))                
        {
            if (((nameStart < 0) || (nameEnd > 0)) && ((valueStart < 0) || (valueEnd > 0)))
            {
                int eol = -1;
                for (int i = current; i < length(); i++)
                {
                    if ((charAt(i) == '\n') || (charAt(i) == '\r'))
                    {
                        eol = i;
                        break;
                    }
                }

                if (eol >= 0)
                {
                    current = eol; // We want to process /r or /n as whitespace
                    continue;
                }
            }
        }

        if (inputc == ',')
        {
            if (charAt(start) == '{')
            {
                if (nameStart < 0)
                {
                    setError("Invalid JSON - missing name:value", current);
                    return -1;
                }
            }
            if (valueStart < 0)
            {
                setError("Invalid JSON - missing value", current);
                return -1;
            }
            if (valueEnd < 0)
            {
                if (current - valueStart <= 0)
                {
                    setError("Invalid JSON - unexpected delimiter", current);
                    return -1;
                }
                valueEnd = current;
            }
            current += 1;
            continue;
        }

        if (inputc == ',')
        {
            if (charAt(start) == '{')
            {
                if ((seenComma) || (nameStart < 0))
                {
                    setError("Invalid JSON - missing name:value", current);
                    return -1;
                }
            }
            else // [
            {
                if ((seenComma) || (valueStart < 0))
                {
                    setError("Invalid JSON - missing value", current);
                    return -1;
                }
            }
            if (valueEnd < 0)
            {
                if (current - valueStart <= 0)
                {
                    setError("Invalid JSON - unexpected delimiter", current);
                    return -1;
                }
                valueEnd = current;
            }
            seenComma = 1;
            current += 1;
            continue;
        }

        if (inputc == ':')
        {
            if (charAt(start) == '[')
            {
                setError("Invalid JSON - invalid :", current);
                return -1;
            }
            if (colon)
            {
                setError("Invalid JSON - invalid :", current);
                return -1;
            }
            if (nameStart < 0)
            {
                setError("Invalid JSON - no name :", current);
                return -1;
            }
            if (nameEnd < 0)
                nameEnd = current;
            colon = 1;
            current += 1;
            continue;
        }


        // We have one of:
        //      } ] { [ " non-white-space
        // ... handle the formation of (name:)value

        int nws = 1;
        if ((inputc == '{') || (inputc == '}') || (inputc == '[') || (inputc == ']') || (inputc == '"'))
            nws = 0;

        if (nws || (inputc == '"'))
        {
        }
        else
        {
            if ((valueStart >= 0) && (valueEnd < 0))

                valueEnd = current;
        }

        if (valueEnd >= 0)
        {
            // Cleanup prior value

            if (((inputc == '}') || (inputc == ']')) && seenComma)
            {
                setError("Invalid JSON - missing name:value or value", current);
                return -1;
            }

            if (request != 0)
            {
                if (request->m_request == GET_ARRAY)
                {
                    if (request->m_array != 0)
                        request->m_array->addRaw(substring(valueStart, valueEnd));
                }
                else if (charAt(start) == '{')
                {
                    if (request->m_request == GET_MAP)
                        request->m_map->putRaw(unescape(substring(nameStart, nameEnd)), substring(valueStart, valueEnd));
                    else if (request->m_request == GET_NAMES)
                        request->m_names->add(unescape(substring(nameStart, nameEnd)));
                    else if (match(*request, nameStart, nameEnd))
                    {
                        if (request->m_request != CONTAINS)
                            request->m_value = substring(valueStart, valueEnd);
                        if ((request->m_request == REMOVE_VALUE) || (request->m_request == REMOVE_OBJECT) || (request->m_request == REMOVE_ARRAY))
                            fixupRemove(nameStart, valueEnd);
                        request->m_response = 1;
                        return -1;
                    }
                }
            }

            nameStart = -1;
            nameEnd = -1;
            colon = 0;
            valueStart = -1;
            valueEnd = -1;
            seenComma = 0;

            if (nws || (inputc == '"'))
            {
                if (charAt(start) == '{')
                    nameStart = current;
                else
                    valueStart = current;
                if (inputc == '"')
                    inquote = '"';
                current++;
                continue;
            }

            // otherwise we drop through to handle nesting
        }
        else if (valueStart > 0)
        {
            if (nws)
            {
                current++;
                continue;
            }
            if (inputc == '"')
            {
                setError("Invalid JSON - unexpected quote", current);
                return -1;
            }
            if ((inputc == '[') || (inputc == '{'))
            {
                setError("Invalid JSON - unexpected start of new value - missing whitespace ?", current);
                return -1;
            }
            if (current - valueStart <= 0)
            {
                setError("Invalid JSON - unexpected delimiter", current);
                return -1;
            }
            valueEnd = current;
        }
        else if (nameEnd >= 0)
        {
            if (! colon)
            {
                setError("Invalid JSON - no name :", current);
                return -1;
            }
            if ((inputc == '}') || (inputc == ']'))
            {
                setError("Invalid JSON - unexpected delimiter - missing value", current);
                return -1;
            }

            if (nws || (inputc == '"'))
            {
                valueStart = current;
                if (inputc == '"')
                    inquote = '"';
                current++;
                continue;
            }
        }
        else if (nameStart > 0)
        {
            // only colon terminates name - colon & inquote have already been handled
            if (! nws)
            {
                setError("Invalid JSON - unexpected delimiter - missing colon ?", current);
                return -1;
            }
            current++;
            continue;
        }
        else if (nws || (inputc == '"'))
        {
            if (charAt(start) == '{')
                nameStart = current;
            else
                valueStart = current;
            if (inputc == '"')
                inquote = '"';
            current++;
            continue;
        }

        // We have one of:
        //      } ] { [ 
        // ... handle nesting

        if (inputc == '}')
        {
            if (charAt(start) != '{')
            {
                setError("Invalid JSON - invalid nesting", current);
                return -1;
            }
            return current + 1;
        }

        if (inputc == ']')
        {
            if (charAt(start) != '[')
            {
                setError("Invalid JSON - invalid nesting", current);
                return -1;
            }
            return current + 1;
        }

        if (inputc == '{')
        {
            if (charAt(start) == '{')
            {
                if (nameEnd < 0)
                {
                    setError("Invalid JSON - missing name", current);
                    return -1;
                }
            }

            valueStart = current;
            current = parse2(current, 0);
            if (current < 0)
                return -1;
            valueEnd = current;
            continue;
        }

        if (inputc == '[')
        {
            if (charAt(start) == '{')
            {
                if (nameEnd < 0)
                {
                    setError("Invalid JSON - missing name", current);
                    return -1;
                }
            }

            valueStart = current;

            if (request != 0)
            {
                if (request->m_request == GET_ARRAY)
                {
                    if (request->m_array == 0)
                    {
                        if (match(*request, nameStart, nameEnd))
                        {
                            request->m_array = new JSONArray();
                            current = parse2(current, request);
                            if (current < 0)
                                return -1;
                            request->m_response = 1;
                            return -1;
                        }
                    }
                }
            }

            current = parse2(current, 0);
            if (current < 0)
                return -1;
            valueEnd = current;
            continue;
        }

        setError("Parsing logic error", current);
        return -1;
    }
}

//-----------------------------------------------------------------------------------
// We need to search left and then right lookup to see if there is a comma to delete
// It will be at the same nesting level as we are
// This is symmetric with the empty flag
//-----------------------------------------------------------------------------------
void JSONObject::fixupRemove(int start, int end)
{
    int i;
    for (i = start - 1; ; i--)
    {
        if (charAt(i) > ' ')
            break;
    }
    if (charAt(i) == ',')
        m_buffer.replace(i, end, "");
    else 
    {
        for (i = end + 1; ; i++)
        {
            if (charAt(i) > ' ')
            break;
        }
        if (charAt(i) == ',')
            m_buffer.replace(start, i + 1, "");
        else 
            m_buffer.replace(start, end, "");
    }
}

//-----------------------------------------------------------------------------------
//
//-----------------------------------------------------------------------------------
TextString JSONObject::unescape(const TextString& value)
{
   if (value.charAt(0) != '"')
       return value;

   TextString buffer;
   buffer.setCapacity(value.length());

   int offset = 1;
   for (int i = 1; i < value.length() - 1; )
   {
       char* escape = 0;

       if ((value.charAt(i) == '\\') && (value.charAt(i + 1) == '"'))
           escape = "\"";
       else if ((value.charAt(i) == '\\') && (value.charAt(i + 1) == '\\'))
           escape = "\\";
       else if ((value.charAt(i) == '\\') && (value.charAt(i + 1) == 't'))
           escape = "\t";
       else if ((value.charAt(i) == '\\') && (value.charAt(i + 1) == 'n'))
           escape = "\n";
       else if ((value.charAt(i) == '\\') && (value.charAt(i + 1) == 'r'))
           escape = "\r";

       if (escape == 0)
       {
           i++;
           continue;
       }

       buffer.append(value, offset, i - offset);
       buffer.append(escape);
       i = i + 2;
       offset = i;
   }
   if (offset < value.length() - 1)
       buffer.append(value, offset, value.length() - 1 - offset);      

   return buffer;
}

TextString JSONObject::escape(const TextString& value)
{
   TextString buffer;
   buffer.setCapacity(value.length() + 10);

   buffer.append("\"");

   int offset = 0;
   for (int i = 0; i < value.length(); i++)
   {
       char* escape = 0;

       if (value.charAt(i) == '"')
           escape = "\\\"";
       else if (value.charAt(i) == '\\')
           escape = "\\\\";
       else if (value.charAt(i) == '\t')
           escape = "\\t";
       else if (value.charAt(i) == '\n')
           escape = "\\n";
       else if (value.charAt(i) == '\r')
           escape = "\\r";
       else if (value.charAt(i) < 0)
           continue;
       else if (value.charAt(i) < ' ')
           abort(); // throw new JSONException("Invalid character");

       if (escape == 0)
           continue;

       buffer.append(value, offset, i - offset);
       buffer.append(escape);
       offset = i + 1;
   }
   if (offset < value.length())
       buffer.append(value, offset, value.length() - offset);

   buffer.append("\"");

   return buffer;
}
