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
import java.io.*;

import org.aventinus.util.*;

//-----------------------------------------------------------------------------------
//##########
// stack context dump
// null ??
//-----------------------------------------------------------------------------------
public class JSONObject 
{
    //-------------------------------------------------------------------------------
    // Use of toolbox is dicey as it uses JSONObject to initialiase
    //-------------------------------------------------------------------------------
    private static Toolbox toolbox = Toolbox.toolbox();
    private static Logger logger = Logger.logger(JSONObject.class);

    public static final long serialVersionUID = 1;

    private static final int MAX_STACK = 40;

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    public interface Encodable
    {
        public JSONObject encode(JSONObject json);
    }

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    private String mInput;
    private StringBuilder mBuffer;
    private int mStart;
    private int mEnd;

    private int[] mStack = null;
    private int mStackIdx = -1;

    private String error = "";

    private static boolean verbose = false;

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    public JSONObject()
    {
        mBuffer = new StringBuilder("{}");
        mInput = null;

        mStart = 0;
        mEnd = -1;
    }

    public JSONObject(byte[] input)
    {
        this(new String(input));
    }

    public JSONObject(String input)
    {
        fromString(input);
    }

    private void fromString(String input)
    {
        mInput = input;

        mStart = 0;
        while(true)
        {
            if (mStart >= mInput.length())
            {
                throw new JSONException("Invalid JSON - no input");
            }

            char inputc = mInput.charAt(mStart);

            if (inputc == '{')
            {
                break;
            }
            if (inputc > ' ')
            {
                throwError("Invalid JSON - invalid leading characters", mStart);
            }

            mStart++;
        }                

        mEnd = parse(mStart, null);
        if (mEnd < 0)
        {
            throw new JSONException("Invalid JSON - invalid message");
        }

        for (int i = mEnd; i < mInput.length(); i++)
        {
            char inputc = mInput.charAt(i);

            if (inputc > ' ')
            {
                throwError("Invalid JSON - invalid trailing characters", mEnd);
            }
        }                
    }

    public JSONObject(JSONObject other)
    {
        mStart = other.mStart;
        mEnd = other.mEnd;

        if (other.mInput == null)
        {
            if (other.mStackIdx >= 0)
            {
                mBuffer = new StringBuilder(other.mBuffer);
            }
            else
            {
                mInput = other.mBuffer.toString();
                mStart = 0;
                mEnd = mInput.length();
            }
        }
        else
        {
            mInput = other.mInput;
        }

        error = other.error;
        mStack = null;
        mStackIdx = -1;

        if (other.mStackIdx >= 0)
        {
            mStackIdx = other.mStackIdx;
            mStack = new int[MAX_STACK];
            System.arraycopy(other.mStack, 0, mStack, 0, MAX_STACK);
        }
    }

    public static void verbose(boolean value)
    {
        verbose = value;
    }

//    public void readExternal(ObjectInput input) throws ClassNotFoundException, IOException
//    {
//        fromString((String)input.readObject());
//    }

//    public void writeExternal(ObjectOutput output) throws IOException
//    {
//        output.writeObject(toString());
//    }

    public static JSONObject fromFile(String filename)
    {
        BufferedReader reader = null;
        try
        {
            if (filename.startsWith("file:"))
            {
                filename = filename.substring(5);
            }

            StringBuilder buffer = new StringBuilder();
            reader = new BufferedReader(new FileReader(filename));
            while(true)
            {
                String line = reader.readLine();
                if (line == null)
                {
                    break;
                }
                buffer.append(line).append("\n");
            }

            return new JSONObject(buffer.toString());
        }
        catch (Exception exception)
        {
            throw new JSONException(exception);
        }
        finally
        {
            Toolbox.close(reader);
        }
    }

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    private static final int CONTAINS = 2;
    private static final int GET_VALUE = 3;
    private static final int GET_OBJECT = 4;
    private static final int GET_ARRAY = 5;
    private static final int GET_NAMES = 6;
    private static final int GET_MAP = 7;
    private static final int GET_LIST = 11;
    private static final int REMOVE_VALUE = 8;
    private static final int REMOVE_ARRAY = 9;
    private static final int REMOVE_OBJECT = 10;

    private static final class Request
    {
        private Request() {}

        private int mRequest;
        private boolean mResponse = false;

        private String mName = null;
        private String mValue = null;

        private JSONArray mArray = null;
        private JSONMap mMap = null;
        private JSONList mList = null;
        private ArrayList<String> mNames = null;
    }

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    public JSONObject encode(Encodable encoder)
    {
        return encoder.encode(this);
    }

    public JSONObject add(String name, String value)
    {
        return addRaw(name, escape(value));
    }

    public JSONObject add(String name, int value)
    {
        return addRaw(name, String.valueOf(value));
    }

    public JSONObject add(String name, long value)
    {
        return addRaw(name, String.valueOf(value));
    }

    public JSONObject add(String name, double value)
    {
        return addRaw(name, String.valueOf(value));
    }

    public JSONObject add(String name, boolean value)
    {
        return addRaw(name, value ? "true" : "false");
    }

    private JSONObject addRaw(String name, String value)
    {
        convert();

        if (error.length() > 0)
        {
            throw new JSONException("Invalid state");
        }

        if (mStackIdx < 0)
        {
            mBuffer.setLength(mBuffer.length() - 1); // remove outer }

            if (mBuffer.length() > 1)
            {
                mBuffer.append(",");
            }
            mBuffer.append(escape(name)).append(":").append(value);

            mBuffer.append("}"); // replace out }
        }
        else if (mStackIdx == 0)
        {
            error = "Invalid state";
            throw new JSONException("Invalid state");
        }
        else
        {
            if (mStack[mStackIdx] == STACK_OBJECT)
            {
                mStack[++mStackIdx] = STACK_VALUE;
            }
            else if (mStack[mStackIdx] == STACK_ARRAY)
            {
                mStack[++mStackIdx] = STACK_VALUE;
            }
            else if (mStack[mStackIdx] == STACK_VALUE)
            {
               mBuffer.append(",");
            }
            else
            {
                error = "Invalid state";
                throw new JSONException("Invalid state");
            }

            mBuffer.append(escape(name)).append(":").append(value);
        }

        return this;
    }

    public JSONObject add(String name, JSONObject value)
    {
        convert();

        if (error.length() > 0)
        {
            throw new JSONException("Invalid state");
        }

        if (mStackIdx < 0)
        {
            mBuffer.setLength(mBuffer.length() - 1); // remove outer }

            if (mBuffer.length() > 1)
            {
                mBuffer.append(",");
            }
            mBuffer.append(escape(name)).append(":").append(value.toString());

            mBuffer.append("}"); // replace out }
        }
        else if (mStackIdx == 0)
        {
            error = "Invalid state";
            throw new JSONException("Invalid state");
        }
        else
        {
            if (mStack[mStackIdx] == STACK_OBJECT)
            {
                mStack[++mStackIdx] = STACK_VALUE;
            }
            else if ((mStack[mStackIdx] == STACK_VALUE) && (mStack[mStackIdx - 1] == STACK_OBJECT))
            {
                mBuffer.append(",");
            }
            else
            {
                error = "Invalid state";
                throw new JSONException("Invalid state");
            }

            mBuffer.append(escape(name)).append(":").append(value.toString());
        }

        return this;
    }

    public JSONObject add(JSONObject value)
    {
        convert();

        if (error.length() > 0)
        {
            throw new JSONException("Invalid state - in error[" + error + "]");
        }

        if (mStackIdx <= 0)
        {
            error = "Invalid state";
            throw new JSONException("Invalid state - missing startArray()");
        }
        else
        {
            if (mStack[mStackIdx] == STACK_ARRAY)
            {
                mStack[++mStackIdx] = STACK_VALUE;
            }
            else if ((mStack[mStackIdx] == STACK_VALUE) && (mStack[mStackIdx - 1] == STACK_ARRAY))
            {
                mBuffer.append(",");
            }
            else
            {
                error = "Invalid state";
                throw new JSONException("Invalid state");
            }

            mBuffer.append(value.toString());
        }

        return this;
    }

    public JSONObject add(String name, JSONArray value)
    {
        convert();

        if (error.length() > 0)
        {
            throw new JSONException("Invalid state");
        }

        if (mStackIdx < 0)
        {
            mBuffer.setLength(mBuffer.length() - 1); // remove outer }

            if (mBuffer.length() > 1)
            {
                mBuffer.append(",");
            }
            mBuffer.append(escape(name)).append(":").append(value.toString());

            mBuffer.append("}"); // replace out }
        }
        else if (mStackIdx == 0)
        {
            error = "Invalid state";
            throw new JSONException("Invalid state");
        }
        else
        {
            if (mStack[mStackIdx] == STACK_OBJECT)
            {
                mStack[++mStackIdx] = STACK_VALUE;
            }
            else if ((mStack[mStackIdx] == STACK_VALUE) && (mStack[mStackIdx - 1] == STACK_OBJECT))
            {
                mBuffer.append(",");
            }
            else
            {
                error = "Invalid state";
                throw new JSONException("Invalid state");
            }

            mBuffer.append(escape(name)).append(":").append(value.toString());
        }

        return this;
    }

    public JSONObject add(JSONArray value)
    {
        convert();

        if (error.length() > 0)
        {
            throw new JSONException("Invalid state");
        }

        if (mStackIdx <= 0)
        {
            error = "Invalid state";
            throw new JSONException("Invalid state");
        }
        else
        {
            if (mStack[mStackIdx] == STACK_ARRAY)
            {
                mStack[++mStackIdx] = STACK_ARRAY;
            }
            else if ((mStack[mStackIdx] == STACK_VALUE) || (mStack[mStackIdx - 1] == STACK_ARRAY))
            {
                mBuffer.append(",");
            }
            else
            {
                error = "Invalid state";
                throw new JSONException("Invalid state");
            }

            mBuffer.append(value.toString());
        }

        return this;
    }

    public String removeValue(String name)
    {
        convert();

        if (error.length() > 0)
        {
            throw new JSONException("Invalid state");
        }

        if (name.length() == 0)
        {
            throw new JSONException("Invalid name");
        }

        if (mStackIdx >= 0)
        {
            throw new JSONException("Object is open pending additions");
        }

        Request request = new Request();
        request.mRequest = REMOVE_VALUE;
        request.mName = name;

        parse(mStart, request);

        if (! request.mResponse)
        {
            throw new JSONException("Did not find Value [" + name + "]");
        }

        return unescape(request.mValue);
    }

    public JSONObject removeObject(String name)
    {
        convert();

        if (error.length() > 0)
        {
            throw new JSONException("Invalid state");
        }

        if (name.length() == 0)
        {
            throw new JSONException("Invalid name");
        }

        if (mStackIdx >= 0)
        {
            throw new JSONException("Object is open pending additions");
        }

        Request request = new Request();
        request.mRequest = REMOVE_OBJECT;
        request.mName = name;

        parse(mStart, request);

        if (! request.mResponse)
        {
            throw new JSONException("Did not find Object [" + name + "]");
        }

        return new JSONObject(request.mValue);
    }

    public JSONArray removeArray(String name)
    {
        convert();

        if (error.length() > 0)
        {
            throw new JSONException("Invalid state");
        }

        if (name.length() == 0)
        {
            throw new JSONException("Invalid name");
        }

        if (mStackIdx >= 0)
        {
            throw new JSONException("Object is open pending additions");
        }

        Request request = new Request();
        request.mRequest = REMOVE_ARRAY;
        request.mName = name;

        parse(mStart, request);

        if (! request.mResponse)
        {
            throw new JSONException("Did not find Array [" + name + "]");
        }

        return new JSONObject("{a:" + request.mValue + "}").getArray("a");
    }

    public int length()
    {
        if (mInput == null)
        {
            return mBuffer.length();
        }
        else
        {
            return mEnd - mStart;
        }
    }

    public String toString()
    {
        if (error.length() > 0)
        {
            throw new JSONException("Invalid state");
        }

        if (mStackIdx >= 0)
        {
            throw new JSONException("Object is open pending additions");
        }

        if (mInput == null)
        {
            return mBuffer.toString();
        }
        else
        {
            if ((mStart == 0) && (mEnd == mInput.length()))
            {
                return mInput;
            }
            return mInput.substring(mStart, mEnd);
        }
    }

    private void convert()
    {
        if (mInput != null)
        {
            // We need to detect the empty object by eliminating whitespace
            //   between the braces
            for (mStart = mStart + 1; mStart < mEnd; mStart++)
            {
                if (mInput.charAt(mStart) > ' ')
                {
                    break;
                }
            }

            mBuffer = new StringBuilder("{");
            mBuffer.append(mInput.substring(mStart, mEnd));

            mInput = null;
            mStart = 0;
            mEnd = -1;
        }
    }

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    private static final int STACK_KEY = 1;
    private static final int STACK_OBJECT = 2;
    private static final int STACK_ARRAY = 3;
    private static final int STACK_VALUE = 4;

    public JSONObject startObject()
    {
        convert();

        if (mStackIdx <= 0)
        {
            error = "Invalid startObject()";
            throw new JSONException("Invalid startObject()");
        }
        else
        {
            if (mStack[mStackIdx] == STACK_KEY)
            {
                mStack[mStackIdx] = STACK_OBJECT;
            }
            else if (mStack[mStackIdx] == STACK_ARRAY)
            {
                mStack[++mStackIdx] = STACK_OBJECT;
            }
            else if ((mStack[mStackIdx] == STACK_VALUE) && (mStack[mStackIdx - 1] == STACK_ARRAY))
            {
                mBuffer.append(",");               
                mStack[mStackIdx] = STACK_OBJECT;
            }
            else
            {
                error = "Invalid startObject()";
                throw new JSONException("Invalid startObject()");
            }

            mBuffer.append("{");
        }

        return this;
    }

    public JSONObject endObject()
    {
        if (mStackIdx <= 0)
        {
            error = "Invalid startArray()";
            throw new JSONException("Invalid endObject()");
        }
        else
        {
            if (mStack[mStackIdx] == STACK_OBJECT)
            {
                mStack[mStackIdx] = STACK_VALUE;
            }
            else if ((mStackIdx > 1) && (mStack[mStackIdx] == STACK_VALUE) && (mStack[mStackIdx - 1] == STACK_OBJECT))
            {
                mStackIdx--;
                mStack[mStackIdx] = STACK_VALUE;
            }
            else
            {
                error = "Invalid endObject()";
                throw new JSONException("Invalid endObject()");
            }

            mBuffer.append("}");

            if (mStackIdx == 1)
            {
                mBuffer.append("}"); // replace outer }
                mStackIdx = -1;
            }
        }

        return this;
    }

    public JSONObject startArray()
    {
        if (mStackIdx <= 0)
        {
            error = "Invalid startArray()";
            throw new JSONException("Invalid startArray()");
        }
        else
        {
            if (mStack[mStackIdx] == STACK_KEY)
            {
                mStack[mStackIdx] = STACK_ARRAY;
            }
            else if (mStack[mStackIdx] == STACK_ARRAY)
            {
                mStack[++mStackIdx] = STACK_ARRAY;
            }
            else if ((mStack[mStackIdx] == STACK_VALUE) && (mStack[mStackIdx - 1] == STACK_ARRAY))
            {
                mStack[mStackIdx] = STACK_ARRAY;
                mBuffer.append(",");
            }
            else
            {
                error = "Invalid startArray()";
                throw new JSONException("Invalid startArray()");
            }

            mBuffer.append("[");
        }

        return this;
    }

    public JSONObject endArray()
    {     
        if (mStackIdx <= 0)
        {
            error = "Invalid endArray()";
            throw new JSONException("Invalid endArray()");
        }
        else
        {
            if (mStack[mStackIdx] == STACK_ARRAY)
            {
                mStack[mStackIdx] = STACK_VALUE;
            }
            else if ((mStack[mStackIdx] == STACK_VALUE) && (mStack[mStackIdx - 1] == STACK_ARRAY))
            {
                mStackIdx--;
                mStack[mStackIdx] = STACK_VALUE;
            }
            else
            {
                error = "Invalid endArray()";
                throw new JSONException("Invalid endArray()");
            }
     
            mBuffer.append("]");

            if (mStackIdx == 1)
            {
                mBuffer.append("}"); // replace outer }
                mStackIdx = -1;
            }
        }

        return this;
    }

    public JSONObject key(String key)
    {
        convert();

        if (key.length() == 0)
        {
            error = "Invalid key";
            throw new JSONException("Invalid key");
        }

        if (mStack == null)
        {
            mStack = new int[MAX_STACK];
            mStackIdx = -1;
        }

        if (mStackIdx < 0)
        {
            mStack[++mStackIdx] = STACK_OBJECT;
            mStack[++mStackIdx] = STACK_KEY;
            mBuffer.setLength(mBuffer.length() - 1); // remove outer }
            if (mBuffer.length() > 1)
            {
                mBuffer.append(",");
            }

            mBuffer.append(escape(key)).append(":");
        }
        else if (mStackIdx == 0)
        {
            error = "Invalid key()";
            throw new JSONException("Invalid key()");
        }
        else
        {
            if (mStack[mStackIdx] == STACK_OBJECT)
            {
                mStack[++mStackIdx] = STACK_KEY;
            }
            else if (mStack[mStackIdx] == STACK_VALUE)
            {
                if (mStack[mStackIdx - 1] == STACK_ARRAY)
                {
                    error = "Invalid key()";
                    throw new JSONException("Invalid key()");
                }

                mStack[mStackIdx] = STACK_KEY;
                mBuffer.append(",");
            }
            else
            {
                error = "Invalid key()";
                throw new JSONException("Invalid key()");
            }

            mBuffer.append(escape(key)).append(":");
        }

        return this;
    }

    public JSONObject value(String value)
    {
        return rawValue(escape(value));
    }

    public JSONObject value(int value)
    {
        return rawValue(String.valueOf(value));
    }

    public JSONObject value(long value)
    {
        return rawValue(String.valueOf(value));
    }

    public JSONObject value(double value)
    {
        return rawValue(String.valueOf(value));
    }

    public JSONObject value(boolean value)
    {
        return rawValue(value ? "true" : "false");
    }

    private JSONObject rawValue(String value)
    {
        if (value.length() == 0)
        {
            error = "Invalid value";
            throw new JSONException("Invalid value");
        }

        if (mStackIdx <= 0)
        {
            error = "Invalid value()";
            throw new JSONException("Invalid value()");
        }
        else
        {
            if (mStack[mStackIdx] == STACK_KEY)
            {
                mStack[mStackIdx] = STACK_VALUE;
            }
            else if (mStack[mStackIdx] == STACK_ARRAY)
            {
                mStack[++mStackIdx] = STACK_VALUE;
            }
            else if ((mStack[mStackIdx] == STACK_VALUE) && (mStack[mStackIdx - 1] == STACK_ARRAY))
            {
                mBuffer.append(",");
            }
            else
            {
                error = "Invalid value()";
                throw new JSONException("Invalid value()");
            }

            mBuffer.append(value);

            if (mStackIdx == 1)
            {
                mBuffer.append("}"); // replace outer }
                mStackIdx = -1;
            }
        }

        return this;
    }

    private void dumpStack()
    {
        logger.log(new StringBuilder().append("buffer=").append(mBuffer));
        for (int i = 0; i < mStackIdx; i++)
        {
            logger.log(new StringBuilder().append("stack[").append(i).append("]=").append(mStack[i]));
        }
    }

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    public boolean containsKey(String name)
    {
        if (name.length() == 0)
        {
            throw new JSONException("Invalid name");
        }

        if (mStackIdx >= 0)
        {
            throw new JSONException("Object is open pending additions");
        }

        Request request = new Request();
        request.mRequest = CONTAINS;
        request.mName = name;

        parse(mStart, request);

        if (! request.mResponse)
        {
            return false;
        }

        return true;
    }

    public boolean isObject(String name)
    {
        String value = (String)getValue(name, GET_VALUE);
        if ((value == null) || (! value.startsWith("{")))
        {
            return false;
        }
        return true;
    }

    public boolean isArray(String name)
    {
        String value = (String)getValue(name, GET_VALUE);
        if ((value == null) || (! value.startsWith("[")))
        {
            return false;
        }
        return true;
    }

    boolean isValue(String name)
    {
        String value = (String)getValue(name, GET_VALUE);
        if ((value == null) || value.startsWith("{") || value.startsWith("["))
        {
            return false;
        }
        return true;
    }

    public String getString(String name)
    {
        return getString(name, null);
    }

    public String getString(String name, String defaultValue)
    {
        String value = (String)getValue(name, GET_VALUE);
        if (value == null)
        {
            if (defaultValue == null)
            {
                throw new JSONException("Did not find Value [" + name + "]");
            }
            value = defaultValue;
        }

        return new String(unescape(value));
    }

    public int getInteger(String name)
    {
        String value = (String)getValue(name, GET_VALUE);
        if (value == null)
        {
            throw new JSONException("Did not find Value [" + name + "]");
        }

        return parseInt(unescape(value));
    }

    public int getInteger(String name, int defaultValue)
    {
        String value = (String)getValue(name, GET_VALUE);
        if (value == null)
        {
           return defaultValue;
        }

        return parseInt(unescape(value));
    }

    public long getLong(String name)
    {
        String value = (String)getValue(name, GET_VALUE);
        if (value == null)
        {
            throw new JSONException("Did not find Value [" + name + "]");
        }

        return parseLong(unescape(value));
    }

    public long getLong(String name, long defaultValue)
    {
        String value = (String)getValue(name, GET_VALUE);
        if (value == null)
        {
           return defaultValue;
        }

        return parseLong(unescape(value));
    }

    public double getDouble(String name)
    {
        String value = (String)getValue(name, GET_VALUE);
        if (value == null)
        {
            throw new JSONException("Did not find Value [" + name + "]");
        }

        return parseDouble(unescape(value));
    }

    public double getDouble(String name, double defaultValue)
    {
        String value = (String)getValue(name, GET_VALUE);
        if (value == null)
        {
           return defaultValue;
        }

        return parseDouble(unescape(value));
    }

    public boolean getBoolean(String name)
    {
        String value = (String)getValue(name, GET_VALUE);
        if (value == null)
        {
            throw new JSONException("Did not find Value [" + name + "]");
        }

        return unescape(value).equals("true");
    }

    public boolean getBoolean(String name, boolean defaultValue)
    {
        String value = (String)getValue(name, GET_VALUE);
        if (value == null)
        {
           return defaultValue;
        }

        return unescape(value).equals("true");
    }

    public JSONObject getObject(String name)
    {
        return getObject(name, null);
    }

    public JSONObject getObject(String name, JSONObject defaultValue)
    {
        JSONObject object = (JSONObject)getValue(name, GET_OBJECT);
        if (object == null)
        {
            if (defaultValue == null)
            {
                throw new JSONException("Did not find Object [" + name + "]");
            }

            return defaultValue;
        }

        return object;
    }

    public JSONArray getArray(String name)
    {
        return getArray(name, null);
    }

    public JSONArray getArray(String name, JSONArray defaultValue)
    {
        JSONArray array = (JSONArray)getValue(name, GET_ARRAY);
        if (array == null)
        {
            if (defaultValue == null)
            {
                throw new JSONException("Did not find Array [" + name + "]");
            }

            return defaultValue;
        }

        return array;
    }

    private Object getValue(String name, int type)
    {
        if (name.length() == 0)
        {
            throw new JSONException("Invalid name");
        }

        if (mStackIdx >= 0)
        {
            throw new JSONException("Object is open pending additions");
        }

        Request request = new Request();
        request.mRequest = type;
        request.mName = name;

        parse(mStart, request);

        if (! request.mResponse)
        {
            return null;
        }

        if (request.mRequest == GET_VALUE)
        {
            return request.mValue;
        }
        else if (request.mRequest == GET_OBJECT)
        {
            return new JSONObject(new String(request.mValue));
        }
        else if (request.mRequest == GET_ARRAY)
        {
            return request.mArray;
        }
        throw new JSONException("Invalid request");
    }

    public JSONMap getMap()
    {
        if (mStackIdx >= 0)
        {
            throw new JSONException("Object is open pending additions");
        }

        Request request = new Request();
        request.mRequest = GET_MAP;
        request.mMap = new JSONMap();

        parse(mStart, request);

        return request.mMap;
    }

    public JSONList getList()
    {
        if (mStackIdx >= 0)
        {
            throw new JSONException("Object is open pending additions");
        }

        Request request = new Request();
        request.mRequest = GET_LIST;
        request.mList = new JSONList();

        parse(mStart, request);

        return request.mList;
    }

    public List<String> getNames()
    {
        if (mStackIdx >= 0)
        {
            throw new JSONException("Object is open pending additions");
        }

        Request request = new Request();
        request.mRequest = GET_NAMES;
        request.mNames = new ArrayList<String>();

        parse(mStart, request);
 
        return request.mNames;
    }

    // Really just a debugging aid - the user can get the same effect by re-escaping the String
    protected String toString(String name)
    {
        String value = (String)getValue(name, GET_VALUE);
        if (value == null)
        {
            throw new JSONException("Did not find Value [" + name + "]");
        }

        return value;
    }

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    protected static int parseInt(String value)
    {
        try
        {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException exception)
        {
            throw new JSONException("Invalid number", exception);
        }
    }

    protected static long parseLong(String value)
    {
        try
        {
            return Long.parseLong(value);
        }
        catch (NumberFormatException exception)
        {
            throw new JSONException("Invalid number", exception);
        }
    }

    protected static double parseDouble(String value)
    {
        try
        {
            return Double.parseDouble(value);
        }
        catch (NumberFormatException exception)
        {
            throw new JSONException("Invalid number", exception);
        }
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
         dump(buffer, 0);
         logger.log(new StringBuilder("JSONObject dump(").append(text).append("): \n").append(buffer));
    }

    public void dump(StringBuilder buffer, int level)
    {
        if (mStackIdx >= 0)
        {
            throw new JSONException("Object is open pending additions");
        }

        String prefix = "";
        for (int i = 0; i < level; i++)
        {
            prefix = prefix + "   ";
        }

        if (level == 0)
        {
            buffer.append(prefix).append("{\n");
        }

        JSONMap map = getMap();
        for (String name: getNames())
        {
            if (map.isObject(name))
            {
                buffer.append(prefix).append("   \"").append(name).append("\":{\n");
                map.getObject(name).dump(buffer, level + 1);
                buffer.append(prefix).append("   ").append("}\n");
            }
            else if (map.isArray(name))
            {
                buffer.append(prefix).append("   \"").append(name).append("\":[\n");
                map.getArray(name).dump(buffer, level + 1);
                buffer.append(prefix).append("   ").append("]\n");
            }
            else
            {
                buffer.append(prefix).append("   ").append("\"").append(name).append("\":").append(map.toString(name)).append("\n");
            }
        }

        if (level == 0)
        {
            buffer.append(prefix).append("}\n");
        }
    }

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    private int scanLimit()
    {
        if (mInput == null)
        {
            return mBuffer.length();
        }
        return mInput.length();
    }

    private char charAt(int index)
    {
        if (mInput == null)
        {
            return mBuffer.charAt(index);
        }
        return mInput.charAt(index);
    }

    private String substring(int start, int end)
    {
        if (mInput == null)
        {
            return mBuffer.substring(start, end);
        }
        return mInput.substring(start, end);
    }

    private boolean match(Request request, int start, int end)
    {
        if (request.mName == null)
        {
            return false;
        }
        if (request.mName.length() == 0)
        {
            return false;
        }
        if (start < 0)
        {
            return false;
        }

        if ((charAt(start) == '"') && (charAt(end - 1) == '"'))
        {
            start++;
            end--;
        }

        if (request.mName.length() != end - start)
        {
            return false;
        }

        for (int i = 0; i < end - start; i++)
        {
            if (request.mName.charAt(i) != charAt(start + i))
            {
                return false;
            }
        }

        return true;
    }

    private void throwError(String text, int current)
    {
        int start = 0;
        int end;
        if (mInput == null)
        {
            end = mBuffer.length();
        }
        else
        {
            end = mInput.length();
        }

        if (end - current > 40)
        {
            end = current + 40;
        }
        if (current - start > 40)
        {
            start = current - 40;
        }

        if (mInput == null)
        {
            error = text + " [" + mBuffer.substring(start, current) + "<=!=>" + mBuffer.substring(current, end) + "]";
        }
        else
        {
            error = text + " [" + mInput.substring(start, current) + "<=!=>" + mInput.substring(current, end) + "]";
        }

        throw new JSONException(error);
    }

    private int parse2(int start, Request request)
    {
        if (verbose)
        {
            logger.log("up");
        }

        int resp = parse(start, request);

        if (verbose)
        {
            logger.log("down");
        }

        return resp;
    }

    private int parse(int start, Request request)
    {
        char inquote = ' ';

        int nameStart = -1;
        int nameEnd = -1;
        boolean colon = false;
        int valueStart = -1;
        int valueEnd = -1;
        boolean seenComma = false;

        // The trick here is that we delay processing a fully formed name:value or value until
        //     we seen the next semantic object

        int current = start + 1;
        while (true)
        {
            if (verbose)
            {
                logger.log(new StringBuilder()
                           .append("start[").append(charAt(start))
                           .append("] cur[").append(current)
                           .append("] inputc[").append((current >= scanLimit() ? "eos" : charAt(current)))
                           .append("] nStart[").append(nameStart)
                           .append("] nEnd[").append(nameEnd)
                           .append("] :[").append(colon)
                           .append("] vStart[").append(valueStart)
                           .append("] vEnd[").append(valueEnd)
                           .append("] seenComma[").append(seenComma)
                           .append("] req[").append((request != null))
                           .append("]"));
            }

            // We need at least one more character to differentiate between real end-of-string
            //    and an escape sequence. We are guaranteed for there to be one
            //    as the input must be terminated by '}'

            if ((inquote != ' ') && (current >= scanLimit() - 1))
            {
                throwError("Invalid JSON - quoting", current);
            }
            else if (current >= scanLimit())
            {
                throwError("Invalid JSON - early end", current);
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
                        {
                            nameEnd = current + 1;
                        }
                        else
                        {
                            valueEnd = current + 1;                   
                        }
                    }
                    else
                    {
                        valueEnd = current + 1;
                    }
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
                {
                    nameEnd = current;
                }
                if ((valueStart >= 0) && (valueEnd < 0))
                {
                    valueEnd = current;
                }
                current += 1;
                continue;
            }

            if (inputc < ' ')
            {
                throwError("Invalid JSON - invalid character", current);
            }

            if ((charAt(current) == '/') && (current < scanLimit() - 1) && (charAt(current + 1) == '/'))                
            {
                if (((nameStart < 0) || (nameEnd > 0)) && ((valueStart < 0) || (valueEnd > 0)))
                {
                    int eol = -1;
                    for (int i = current; i < scanLimit(); i++)
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
                    if ((seenComma) || (nameStart < 0))
                    {
                        throwError("Invalid JSON - missing name:value", current);
                    }
                }
                else // [
                {
                    if ((seenComma) || (valueStart < 0))
                    {
                        throwError("Invalid JSON - missing value", current);
                    }
                }
                if (valueEnd < 0)
                {
                    if (current - valueStart <= 0)
                    {
                        throwError("Invalid JSON - unexpected delimiter", current);
                    }
                    valueEnd = current;
                }
                seenComma = true;
                current += 1;
                continue;
            }

            if (inputc == ':')
            {
                if (charAt(start) == '[')
                {
                    throwError("Invalid JSON - invalid :", current);
                }
                if (colon)
                {
                    throwError("Invalid JSON - invalid :", current);
                }
                if (nameStart < 0)
                {
                    throwError("Invalid JSON - no name", current);
                }
                if (nameEnd < 0)
                {
                    nameEnd = current;
                }
                colon = true;
                current += 1;
                continue;
            }

            // We have one of:
            //      } ] { [ " non-white-space
            // ... handle the formation of (name:)value

            boolean nws = true;
            if ((inputc == '{') || (inputc == '}') || (inputc == '[') || (inputc == ']') || (inputc == '"'))
            {
                nws = false;
            }

            if (nws || (inputc == '"'))
            {
            }
            else
            {
                if ((valueStart >= 0) && (valueEnd < 0))
                {
                    valueEnd = current;
                }
            }

            if (valueEnd >= 0)
            {
                // Cleanup prior value

                if (((inputc == '}') || (inputc == ']')) && seenComma)
                {
                    throwError("Invalid JSON - missing name:value or value", current);
                }

                if (request != null)
                {
                    if (request.mRequest == GET_ARRAY)
                    {
                        if (request.mArray != null)
                        {
                            request.mArray.addRaw(substring(valueStart, valueEnd));
                        }
                    }
                    else if (charAt(start) == '{')
                    {
                        if (request.mRequest == GET_MAP)
                        {
                            request.mMap.putRaw(unescape(substring(nameStart, nameEnd)), substring(valueStart, valueEnd));
                        }
                        else if (request.mRequest == GET_LIST)
                        {
                            request.mList.addRaw(unescape(substring(nameStart, nameEnd)), substring(valueStart, valueEnd));
                        }
                        else if (request.mRequest == GET_NAMES)
                        {
                            request.mNames.add(unescape(substring(nameStart, nameEnd)));
                        }
                        else if (match(request, nameStart, nameEnd))
                        {
                            if (request.mRequest != CONTAINS)
                            {
                                request.mValue = substring(valueStart, valueEnd);
                            }
                            if ((request.mRequest == REMOVE_VALUE) || (request.mRequest == REMOVE_OBJECT) || (request.mRequest == REMOVE_ARRAY))
                            {
                                fixupRemove(nameStart, valueEnd);
                            }
                            request.mResponse = true;
                            return -1;
                        }
                    }
                }

                nameStart = -1;
                nameEnd = -1;
                colon = false;
                valueStart = -1;
                valueEnd = -1;
                seenComma = false;

                if (nws || (inputc == '"'))
                {
                    if (charAt(start) == '{')
                    {
                        nameStart = current;
                    }
                    else
                    {
                        valueStart = current;
                    }
                    if (inputc == '"')
                    {
                        inquote = '"';
                    }
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
                    throwError("Invalid JSON - unexpected quote", current);
                }
                if ((inputc == '[') || (inputc == '{'))
                {
                    throwError("Invalid JSON - unexpected start of new value - missing whitespace ?", current);
                }
                if (current - valueStart <= 0)
                {
                    throwError("Invalid JSON - unexpected delimiter", current);
                }
                valueEnd = current;
            }
            else if (nameEnd >= 0)
            {
                if (! colon)
                {
                    throwError("Invalid JSON - no name :", current);
                }
                if ((inputc == '}') || (inputc == ']'))
                {
                    throwError("Invalid JSON - unexpected delimiter - missing value", current);
                }

                if (nws || (inputc == '"'))
                {
                    valueStart = current;
                    if (inputc == '"')
                    {
                        inquote = '"';
                    }
                    current++;
                    continue;
                }
            }
            else if (nameStart > 0)
            {
                // only colon terminates name - colon & inquote have already been handled
                if (! nws)
                {
                    throwError("Invalid JSON - unexpected delimiter - missing colon ?", current);
                }
                current++;
                continue;
            }
            else if (nws || (inputc == '"'))
            {
                if (charAt(start) == '{')
                {
                    nameStart = current;
                }
                else
                {
                    valueStart = current;
                }
                if (inputc == '"')
                {
                    inquote = '"';
                }
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
                    throwError("Invalid JSON - invalid nesting", current);
                }
                return current + 1;
            }

            if (inputc == ']')
            {
                if (charAt(start) != '[')
                {
                    throwError("Invalid JSON - invalid nesting", current);
                }
                return current + 1;
            }

            if (inputc == '{')
            {
                if (charAt(start) == '{')
                {
                    if (nameEnd < 0)
                    {
                        throwError("Invalid JSON - missing name", current);
                    }
                }

                valueStart = current;
                current = parse2(current, null);
                if (current < 0)
                {
                    return -1;
                }
                valueEnd = current;
                continue;
            }

            if (inputc == '[')
            {
                if (charAt(start) == '{')
                {
                    if (nameEnd < 0)
                    {
                        throwError("Invalid JSON - missing name", current);
                    }
                }

                valueStart = current;

                if (request != null)
                {
                    if (request.mRequest == GET_ARRAY)
                    {
                        if (request.mArray == null)
                        {
                            if (match(request, nameStart, nameEnd))
                            {
                                request.mArray = new JSONArray();
                                current = parse2(current, request);
                                if (current < 0)
                                {
                                    return -1;
                                }
                                request.mResponse = true;
                                return -1;
                            }
                        }
                    }
                }

                current = parse2(current, null);
                if (current < 0)
                {
                    return -1;
                }
                valueEnd = current;
                continue;
            }

            throwError("Parsing logic error", current);
        }
    }

    //-------------------------------------------------------------------------------
    // We need to search left and then right looking to see if there is a comma to delete
    // It will be at the same nesting level as we are
    //-------------------------------------------------------------------------------
    private void fixupRemove(int start, int end)
    {
        int i;
        for (i = start - 1; ; i--)
        {
            if (charAt(i) > ' ')
            {
                break;
            }
        }
        if (charAt(i) == ',')
        {
            mBuffer.delete(i, end);
        }
        else 
        {
            for (i = end; ; i++)
            {
                if (charAt(i) > ' ')
                {
                    break;
                }
            }
            if (charAt(i) == ',')
            {
                mBuffer.delete(start, i + 1);
            }
            else 
            {
                mBuffer.delete(start, end);
            }
        }
    }

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    public static String unescape(String value)
    {
       if (value == null)
       {
           return value;
       }

       if (value.length() == 0)
       {
           return value;
       }

       if (value.charAt(0) != '"')
       {
           return value;
       }

       StringBuilder buffer = null;

       int offset = 1;
       for (int i = 1; i < value.length() - 1; )
       {
           char escape = 0;

           if ((value.charAt(i) == '\\') && (value.charAt(i + 1) == '"'))
           {
               escape = '"';
           }
           else if ((value.charAt(i) == '\\') && (value.charAt(i + 1) == '\\'))
           {
               escape = '\\';
           }
           else if ((value.charAt(i) == '\\') && (value.charAt(i + 1) == 't'))
           {
               escape = '\t';
           }
           else if ((value.charAt(i) == '\\') && (value.charAt(i + 1) == 'n'))
           {
               escape = '\n';
           }
           else if ((value.charAt(i) == '\\') && (value.charAt(i + 1) == 'r'))
           {
               escape = '\r';
           }

           if (escape == 0)
           {
               i++;
               continue;
           }

           if (buffer == null)
           {
               buffer = new StringBuilder(value.length());
           }

           buffer.append(value.substring(offset, i));
           buffer.append(escape);
           i = i + 2;
           offset = i;
       }
       if (offset == 1)
       {
           return value.substring(1, value.length() - 1);
       }

       if (offset < value.length() - 1)
       {
           buffer.append(value.substring(offset, value.length() - 1));
       }

       return buffer.toString();
    }

    public static String escape(String value)
    {
       if (value == null)
       {
           return null;
       }

       StringBuilder buffer = new StringBuilder(value.length() + 10);

       buffer.append('"');

       int offset = 0;
       for (int i = 0; i < value.length(); i++)
       {
           String escape = null;

           if (value.charAt(i) == '"')
           {
               escape = "\\\"";
           }
           else if (value.charAt(i) == '\\')
           {
               escape = "\\\\";
           }
           else if (value.charAt(i) == '\t')
           {
               escape = "\\t";
           }
           else if (value.charAt(i) == '\n')
           {
               escape = "\\n";
           }
           else if (value.charAt(i) == '\r')
           {
               escape = "\\r";
           }
           else if (value.charAt(i) < ' ')
           {
               throw new JSONException("Invalid character");
           }

           if (escape == null)
           {
               continue;
           }

           buffer.append(value.substring(offset, i));
           buffer.append(escape);
           offset = i + 1;
       }

       if (offset == 0)
       {
           buffer.append(value);
       }
       else if (offset < value.length())      
       {
           buffer.append(value.substring(offset, value.length()));      
       }

       buffer.append('"');

       return buffer.toString();
    }
}
