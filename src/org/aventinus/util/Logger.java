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
import java.text.*;
import java.io.*;
import java.lang.reflect.*;

//-----------------------------------------------------------------------------------
//
//-----------------------------------------------------------------------------------
public final class Logger
{
    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    private static SimpleDateFormat logFormat = new SimpleDateFormat("yyyyMMdd.HHmmss.SSS");

    private static final int DEBUG = 2;
    private static final int INFO = 3;
    private static final int ERROR = 4;

    private static Method getLogger = null;
    private static Method debug1 = null;
    private static Method debug2 = null;
    private static Method info1 = null;
    private static Method info2 = null;
    private static Method error1 = null;
    private static Method error2 = null;

    private Object targetLogger = null;

    private boolean formatPrefix = true;
    private static boolean formatPrefixAll = true;

    private static volatile boolean debug = false;

    private static PrintStream output = null;
    private static long rolltime = -1;

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    private Logger(Class<?> klass)
    {
        try
        {
            if (getLogger != null)
            {
                targetLogger = getLogger.invoke(null, klass);
            }
        }
        catch (IllegalAccessException exception)
        {
        }
        catch (InvocationTargetException exception)
        {
        }
    }

    public static Logger logger(Class<?> klass)
    {
        return new Logger(klass);
    }

    static
    {
        try
        {
            Class<?> loggerFactory = Class.forName("org.slf4j.LoggerFactory");
            getLogger = loggerFactory.getMethod("getLogger", Class.class);

            Class<?> targetInterface = Class.forName("org.slf4j.Logger");
            debug1 = targetInterface.getMethod("debug", String.class);
            debug2 = targetInterface.getMethod("debug", String.class, Throwable.class);
            info1 = targetInterface.getMethod("info", String.class);
            info2 = targetInterface.getMethod("info", String.class, Throwable.class);
            error1 = targetInterface.getMethod("error", String.class);
            error2 = targetInterface.getMethod("error", String.class, Throwable.class);
        }
        catch (ClassNotFoundException exception)
        {
            getLogger = null;
        }
        catch (NoSuchMethodException exception)
        {
            getLogger = null;
        }
    }

    public static void init()
    {
        if (getLogger != null)
            return;

        rolltime = 0;
        open();
    }

    public static void setFormatPrefixAll(boolean formatPrefixAll)
    {
        Logger.formatPrefixAll = formatPrefixAll;
    }

    public void setFormatPrefix(boolean formatPrefix)
    {
        this.formatPrefix = formatPrefix;
    }

    private static void open()
    {
        if (rolltime < 0)
            return;

        if (System.currentTimeMillis() < rolltime)
            return;

        if (! Toolbox.toolbox().hasProperty("logging"))
        {
            rolltime = Long.MAX_VALUE;
            return;
        }

        if (output != null)
            Toolbox.close(output);

        try
        {
            StringBuffer buffer = new StringBuffer();
            logFormat.format(new Date(), buffer, new FieldPosition(0));
            String filename = Toolbox.toolbox().getStringProperty("logging.directory", ".") 
                            + "/" + Toolbox.toolbox().getStringProperty("logging.prefix")
                            + "." + buffer.toString() + ".log";
            output = new PrintStream(new FileOutputStream(filename));
        }
        catch (IOException exception)
        {
            if (rolltime <= 0)
                System.exit(0);
            if (Toolbox.toolbox().getStringProperty("logging.exitOnError", "N").equals("Y"))
                System.exit(0);
            rolltime = Long.MAX_VALUE;
            output = null;
            return;
        }
        
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.add(Calendar.SECOND, 1);

        rolltime = calendar.getTime().getTime();
    }

    private class StackTrace extends Exception
    {
        public static final long serialVersionUID = 1;

        public StackTrace() 
        {
            super();
        }

        public StackTrace(String message) 
        {
            super(message);
        }

        public StackTrace(Throwable exception) 
        {
            super();

            initCause(exception);
        }

        public StackTrace(String message, Throwable exception) 
        {
            super(message);
            initCause(exception);
        }
    }   

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    public static void debug(boolean debug)
    {
        Logger.debug = debug;
    }

    public static boolean debug()
    {
        return debug;
    }

    public void log(StringBuilder text, Object... values)
    {
        log(INFO, text.toString(), null, values);
    }

    public void log(String text, Object... values)
    {
        log(INFO, text, null, values);
    }

    public void log(Throwable exception)
    {
        log(INFO, null, exception);
    }

    public void debug(StringBuilder text, Object... values)
    {
        if (debug)
            log(DEBUG, text.toString(), null, values);
    }

    public void debug(String text, Object... values)
    {
        if (debug)
            log(DEBUG, text, null, values); 
    }

    public void info(StringBuilder text, Object... values)
    {
        log(INFO, text.toString(), null, values);
    }

    public void info(String text, Object... values)
    {
        log(INFO, text, null, values);
    }

    public void info(Throwable exception)
    {
        log(INFO, null, exception);
    }

    public void error(StringBuilder text, Object... values)
    {
        log(ERROR, text.toString(), null, values);
    }

    public void error(String text, Object... values)
    {
        log(ERROR, text, null, values);
    }

    public void error(Throwable exception)
    {
        log(ERROR, null, exception);
    }

    public void fatal()
    {
        log(ERROR, null, new StackTrace());
        System.exit(-1);
    }

    public void fatal(String text, Object... values)
    {
         log(ERROR, text, null, values);
         System.exit(-1);
    }

    public void fatal(Throwable exception)
    {
        log(ERROR, null, exception);
        System.exit(-1);
    }

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    private void log(int level, String text, Throwable throwable, Object... values)
    {
        if ((throwable == null) && (values.length > 0) && (values[0] instanceof Throwable))
        {
            throwable = (Throwable)values[0];
            Object[] tvalues = new Object[values.length - 1];
            for (int i = 0; i < values.length - 1; i++)
                tvalues[i] = values[i + 1];
            values = tvalues;
        }

        StringBuffer buffer = new StringBuffer(200);
        if (formatPrefix && formatPrefixAll)
            formatPrefix(buffer);
        int prefix = buffer.length();

        if (text != null)
        {
            if (values.length == 0)
            {
                buffer.append(text);
            }
            else
            {
                for (int i = 0; i < values.length; i++)
                {
                    int index = text.indexOf("{}");
                    if (index < 0)
                    {
                        break;
                    }
                    buffer.append(text.substring(0, index)).append(values[i]);
                    text = text.substring(index + 2);
                }
                buffer.append(text);
            }
        }

        try
        {
            if ((targetLogger != null) && (error2 != null))
            {
                if (level == DEBUG)
                {
                    if (throwable == null)
                    {
                        debug1.invoke(targetLogger, buffer.substring(prefix));
                    }
                    else
                    {
                        debug2.invoke(targetLogger, buffer.substring(prefix), throwable);
                    }
                    return;
                }
                else if (level == INFO)
                {
                    if (throwable == null)
                    {
                        info1.invoke(targetLogger, buffer.substring(prefix));
                    }
                    else
                    {
                        info2.invoke(targetLogger, buffer.substring(prefix), throwable);
                    }
                    return;
                }
                else if (level == ERROR)
                {
                    if (throwable == null)
                    {
                        error1.invoke(targetLogger, buffer.substring(prefix));
                    }
                    else
                    {
                        error2.invoke(targetLogger, buffer.substring(prefix), throwable);
                    }
                    return;
                }
            }
        }
        catch (IllegalAccessException exception)
        {
        }
        catch (InvocationTargetException exception)
        {
        }

        synchronized(Logger.class)
        {
            buffer.append("\n");

            open();

            if (output == null)
            {
                System.out.print(buffer.toString());
                if (throwable != null)
                {
                    throwable.printStackTrace();
                }
            }
            else
            {
                try
                {
                    output.write(buffer.toString().getBytes());
                    if (throwable != null)
                    {
                        throwable.printStackTrace(output);
                    }
                }
                catch (IOException exception)
                {
                   Toolbox.close(output);
                   if (Toolbox.toolbox().getStringProperty("logging.exitOnError", "N").equals("Y"))
                       System.exit(0);
                   rolltime = Long.MAX_VALUE;
                   output = null;
                }
            }
        }
    }

    private static void formatPrefix(StringBuffer buffer)
    {
        logFormat.format(new Date(), buffer, new FieldPosition(0));

        buffer.append(":[")
              .append(Thread.currentThread().hashCode())
              .append(":")
              .append(Thread.currentThread().getName())
              .append("] ");
    }
}
