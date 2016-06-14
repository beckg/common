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
package org.aventinus.util;

import java.util.*;
import java.math.*;
import java.io.*;

import org.aventinus.json.*;
import org.aventinus.database.*;

//-----------------------------------------------------------------------------------
//
//-----------------------------------------------------------------------------------
public final class Toolbox
{
    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    private static boolean verbose = true;

    static
    {
        if ((System.getProperty("verbose") != null) && System.getProperty("verbose").equals("N"))
        {
            verbose = false;
        }
    }

    private static Logger logger = Logger.logger(Toolbox.class);

    private static Toolbox instance;

    private Database database = null;

    private JSONObject config;
    private Map<String, String> configRawValues = new HashMap<String, String>();

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    public static synchronized Toolbox instance()
    {
        if (instance == null)
        {
            instance = new Toolbox();
            instance.initConfiguration();
//            instance.initConfiguration(System.getProperty("properties.url"));
        }
        return instance;
    }

    public static synchronized Toolbox toolbox()
    {
        return instance();
    }

    private Toolbox()
    {        
    }

    public static void verbose(boolean value)
    {
        verbose = value;
    }

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    public synchronized Database database()
    {
        if (database == null)
        {
            database = new Database();
        }

        return database;
    }

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    public static void sleep(long period)
    {
        try
        {
             Thread.sleep(period);
        }
        catch (InterruptedException exception)
        {
            // ignore
        }
    }

    public static void wait(Object object, long timeout)
    {
        try
        {
            object.wait(timeout);
        }
        catch (InterruptedException exception)
        {
            // ignore
        } 
    }

    public static void close(Closeable stream)
    {
        try
        {
            if (stream != null)
            {
                stream.close();
            }
        }
        catch (IOException exception)
        {
            // We use this for catch or finally closes and hence ignore the error
        }
    }

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    public static void dumpThreads()
    {
        Map<Thread,StackTraceElement[]> traces = Thread.getAllStackTraces();
        for (Thread thread: traces.keySet())
        {
            logger.info("Thread:" + thread);
            for (StackTraceElement trace: traces.get(thread))
                logger.info("... " + trace);
        }
    }

    public static double normalise(double value, int scale)
    {
        return new BigDecimal(value).setScale(scale, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    //-------------------------------------------------------------------------------
    // Escapes embedded 's by doubling then up.
    // It also avoids a problem with Oracle and empty strings becoming nulls
    //-------------------------------------------------------------------------------
    public static String sqlEscape(String value)
    {
        if (value.length() == 0)
        {
            return "' '";
        }

        for (int i = 0; i < value.length(); )
        {
            if (value.charAt(i) == '\'')
            {
                value = value.substring(0, i) + "'" + value.substring(i);
                i = i + 2;
            }
            else
            {
                i = i + 1;
            }
        }

        return "'" + value + "'";
    }

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    private void initConfiguration()
    {
        if (System.getProperty("configuration") == null)
        {
            config = new JSONObject();
        }
        else
        {
            config = readConfig();
            if (config == null)
            {
                System.exit(0);
            }
        }

        Logger.init();

        if (config.containsKey("system"))
        {
            JSONMap map = config.getObject("system").getMap();
            for (String name: map.keySet())
            {
                String value = map.getString(name);
                System.setProperty(name, value);

                if ((name.toUpperCase().indexOf("PASSWORD") >= 0) || (name.toUpperCase().indexOf("PASSWD") >= 0))
                {
                    value = "********";
                }
                logger.info("System.setProperty[" + name + "][" + value + "]");
            }
        }

/*
            if (verbose)
            {
                TreeSet<String> tree = new TreeSet<String>();

                for (Enumeration<Object> e = System.getProperties().keys(); e.hasMoreElements() ;) 
                {
                    String key = (String)e.nextElement();
                    Object value = System.getProperties().getProperty(key);
                    if ((key.toUpperCase().indexOf("PASSWORD") >= 0) || (key.toUpperCase().indexOf("PASSWD") >= 0))
                    {
                        value = "********";
                    }
                    tree.add(key + "=[" + value + "]");
                }

                for (Iterator<String> iterator = tree.iterator(); iterator.hasNext(); )
                {
                    logger.log("SystemProperty: " + iterator.next()); 
                }
            }     
*/
    }

    private JSONObject readConfig()
    {
        BufferedReader reader = null;
        try
        {
            String url = System.getProperty("configuration");

// add http option
            if (! url.startsWith("file:"))
            {
                throw new IllegalArgumentException("Invalid configuration option");
            }
            url = url.substring(5);

            StringBuilder buffer = new StringBuilder();
            reader = new BufferedReader(new FileReader(url));
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
            logger.info("", exception);
            return null;
        }
        finally
        {
            close(reader);
        }
    }

    public void setRawString(String name, String value)
    {
        synchronized(config)
        {
            configRawValues.put(name, value);
        }
    }

    private String getRawString(String name)
    {
        synchronized(config)
        {
            if (configRawValues.containsKey(name))
            {
                return configRawValues.get(name);
            }

            JSONObject source = config;

            String[] elements = name.split("\\.");

            for (int i = 0; i < elements.length - 1; i++)
            {
                if (! source.containsKey(elements[i]))
                {
                    return System.getProperty(name);
                }

                source = source.getObject(elements[i]);
            }

            String tail = elements[elements.length - 1];

            String value;

            if (! source.containsKey(tail))
            {
                value = System.getProperty(name); // will eventually find null !
            }
            else if (source.isObject(tail))
            {
                value = source.getObject(tail).toString();
            }
            else if (source.isArray(tail))
            {
                value = source.getArray(tail).toString();
            }
            else
            {
                value = source.getString(tail);
            }

            configRawValues.put(name, value); // we are caching null values
            return value;
        }
    }

    public boolean hasProperty(String name)
    {
        return (getRawString(name) != null);
    }

    public String getStringProperty(String name)
    {
        return getStringProperty(name, null);
    }

    public String getStringProperty(String name, String defaultValue)
    {
        String value = getRawString(name);
        if (value == null)
        {
            if (defaultValue == null)
            {
                throw new IllegalArgumentException("Invalid configuration item [" + name + "]");
            }
            return defaultValue;
        }

        return value;
    }

    public int getIntegerProperty(String name)
    {
        return getIntegerProperty(name, null);
    }

    public int getIntegerProperty(String name, Integer defaultValue)
    {
        String value = getRawString(name);
        if (value == null)
        {
            if (defaultValue == null)
            {
                throw new IllegalArgumentException("Invalid configuration item [" + name + "]");
            }
            return defaultValue;
        }

        try
        {
            if (value.trim().startsWith("0x"))
                return Integer.parseInt(value.trim().substring(2), 16);
            return Integer.parseInt(value.trim());
        }
        catch (NumberFormatException exception)
        {
            throw new IllegalArgumentException("Invalid configuration item [" + name + "]", exception);
        }
    }

    public long getLongProperty(String name)
    {
        return getLongProperty(name, (Long)null);
    }

    public long getLongProperty(String name, Integer defaultValue)
    {
        return getLongProperty(name, (long)defaultValue);
    }

    public long getLongProperty(String name, Long defaultValue)
    {
        String value = getRawString(name);
        if (value == null)
        {
            if (defaultValue == null)
            {
                throw new IllegalArgumentException("Invalid configuration item [" + name + "]");
            }
            return defaultValue;
        }

        try
        {
            return Long.parseLong(value.trim());
        }
        catch (NumberFormatException exception)
        {
            throw new IllegalArgumentException("Invalid configuration item [" + name + "]", exception);
        }
    }

    public double getDoubleProperty(String name)
    {
        return getDoubleProperty(name, (Double)null);
    }

    public double getDoubleProperty(String name, Double defaultValue)
    {
        String value = getRawString(name);
        if (value == null)
        {
            if (defaultValue == null)
            {
                throw new IllegalArgumentException("Invalid configuration item [" + name + "]");
            }
            return defaultValue;
        }

        try
        {
            return Double.parseDouble(value.trim());
        }
        catch (NumberFormatException exception)
        {
            throw new IllegalArgumentException("Invalid configuration item [" + name + "]", exception);
        }
    }

    public JSONObject getObjectProperty(String name)
    {
        return getObjectProperty(name, null);
    }

    public JSONObject getObjectProperty(String name, JSONObject defaultValue)
    {
        String[] elements = name.split("\\.");

        synchronized(config)
        {
            JSONObject source = config;

            for (int i = 0; i < elements.length; i++)
            {
                if (! source.containsKey(elements[i]))
                {
                    String raw = System.getProperty(name);
                    if (raw != null)
                    {
                        return new JSONObject(raw);
                    }

                    if (defaultValue == null)
                    {
                        throw new IllegalArgumentException("Invalid configuration item [" + name + "]");
                    }
                    return defaultValue;
                }

                if (i == elements.length - 1)
                {
                    return source.getObject(elements[i]);
                }

                source = source.getObject(elements[i]);
            }

            throw new RuntimeException("??");
        }
    }

    public JSONArray getArrayProperty(String name)
    {
        return getArrayProperty(name, null);
    }

    public JSONArray getArrayProperty(String name, JSONArray defaultValue)
    {
        String[] elements = name.split("\\.");

        synchronized(config)
        {
            JSONObject source = config;

            for (int i = 0; i < elements.length; i++)
            {
                if (! source.containsKey(elements[i]))
                {
                    String raw = System.getProperty(name);
                    if (raw != null)
                    {
                        return new JSONObject("{a:" + raw + "}").getArray("a");
                    }

                    if (defaultValue == null)
                    {
                        throw new IllegalArgumentException("Invalid configuration item [" + name + "]");
                    }
                    return defaultValue;
                }

                if (i == elements.length - 1)
                {
                    return source.getArray(elements[i]);
                }

                source = source.getObject(elements[i]);
            }

            throw new RuntimeException("??");
        }
    }
}
