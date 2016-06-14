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
import java.lang.reflect.*;
import java.net.*;
import java.io.*;
import java.math.*;
import java.sql.*;
import java.util.zip.*;

import org.aventinus.util.*;

//-----------------------------------------------------------------------------------
//
//-----------------------------------------------------------------------------------
public abstract class RetrieverAdapter implements Retriever, Binder
{
    private static Logger logger = Logger.logger(RetrieverAdapter.class);

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    private ZipOutputStream output = null;
    private BufferedReader input = null;
    private String[] values;

    private ResultSet set;

    private ResultSetMetaData meta = null;

    private int rows = 0;

    private Class<?> oracleTimestampClass = null;
    private Class<?> oracleCLOBClass = null;
    private Method clobGetSubString = null;
    private Method clobLength = null;

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    public RetrieverAdapter()
    {
        try
        {
            oracleTimestampClass = Class.forName("oracle.sql.TIMESTAMP");
        }
        catch (Exception exception)
        {
            // ignore - we are not using oracle
        }

        try
        {
            oracleCLOBClass = Class.forName("oracle.sql.CLOB");
            clobGetSubString = oracleCLOBClass.getMethod("getSubString", Long.TYPE, Integer.TYPE);
            clobLength = oracleCLOBClass.getMethod("length", (Class)null);
        }
        catch (Exception exception)
        {
            // ignore - we are not using oracle
        }
    }

    public RetrieverAdapter(String inputFile, String outputFile)
    {
        try
        {
            if ((inputFile != null) && (inputFile.length() > 0))
            {
                ZipInputStream zipInput;
                if (inputFile.startsWith("http:"))
                {
                    URLConnection connection = new URL(inputFile).openConnection();
                    zipInput = new ZipInputStream(connection.getInputStream());
                }
                else
                {
                    zipInput = new ZipInputStream(new FileInputStream(inputFile));
                }

                ZipEntry entry = zipInput.getNextEntry();
                if ((entry == null) || (! entry.getName().equals("data")))
                {
                    throw new DatabaseException("Invalid zip file");
                }
                input = new BufferedReader(new InputStreamReader(zipInput));
            }

            if ((outputFile != null) && (outputFile.length() > 0))
            {
                output = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)));
                output.putNextEntry(new ZipEntry("data"));
            }
        }
        catch (IOException exception) 
        {
            throw new DatabaseException(exception);
        }
    }

    public void read()
    {
        try
        {
            while (true)
            {
                String line = input.readLine();
                if (line == null)
                {
                    break;
                }

                int cols = 1;
                for (int index = 0; index < line.length(); )
                {
                    if (index < line.length() - 1)
                    {
                        if ((line.charAt(index) == '\\') && (line.charAt(index + 1) == '\\'))
                        {
                            index += 2;
                            continue;
                        }
                        if ((line.charAt(index) == '\\') && (line.charAt(index + 1) == ','))
                        {
                            index += 2;
                            continue;
                        }
                    }

                    if (line.charAt(index) == ',')
                    {
                        cols++;
                    }
                    index++;
                }

                values = new String[cols];

                cols = 0;
                int start = 0;
                for (int index = 0; index < line.length(); )
                {
                    if (index < line.length() - 1)
                    {
                        if ((line.charAt(index) == '\\') && (line.charAt(index + 1) == '\\'))
                        {
                            index += 2;
                            continue;
                        }
                        if ((line.charAt(index) == '\\') && (line.charAt(index + 1) == ','))
                        {
                            index += 2;
                            continue;
                        }
                    }

                    if (line.charAt(index) == ',')
                    {
                        values[cols++] = new String(unescape(line.substring(start, index)));
                        start = index + 1;
                    }
                    index++;
                }
                values[cols++] = new String(unescape(line.substring(start)));

                rows++;
                try
                {
                    processRow();
                }
                catch(RuntimeException exception)
                {
                    logger.log("line[" + line + "]"); 
                    throw exception;
                }
            }

            input.close();
            input = null;
        }
        catch (IOException exception) 
        {
            throw new DatabaseException(exception);
        }
    }

    public void close()
    {
        try
        {
            if (output != null)
            {
                output.closeEntry();
                output.finish();
                output.close();
                output = null;
            }    
        }
        catch (IOException exception)
        {
            throw new DatabaseException(exception);
        }
    }

    public void processRow(ResultSet value)
    {
        set = value;
        rows++;
        processRow();

        try
        {
            if (output != null)
            {
                if (meta == null)
                {
                    meta = set.getMetaData();
                }

                StringBuilder buffer = new StringBuilder();
                for (int col = 0; col < meta.getColumnCount(); col++)
                {
                    Object object = getObject(col + 1);
                    if (object == null)
                    {
                        object = "";
                    }
                    if (object instanceof java.sql.Timestamp)
                    {
                        object = Long.valueOf((set.getTimestamp(col + 1).getTime()));
                    }
                    if (object instanceof java.sql.Date)
                    {
                        object = new Long(set.getTimestamp(col + 1).getTime());
                    }

                    if (buffer.length() > 0)
                    {
                        buffer.append(",");
                    }
                    buffer.append(escape(object.toString()));
                }
                buffer.append("\n");
                output.write(buffer.toString().getBytes());
            }
        }
        catch (IOException exception)
        {
            throw new DatabaseException(exception);
        }
        catch (SQLException exception)
        {
            throw new DatabaseException(exception);
        }
    }

    public abstract void processRow();

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    private String escape(String value)
    {
        int index = 0;
        while (true)
        {
            index = value.indexOf('\\', index);
            if (index < 0)
            {
                break;
            }
            value = value.substring(0, index) + '\\' + value.substring(index);
            index += 2;
        }

        while (true)
        {
            index = value.indexOf(',', index);
            if (index < 0)
            {
                break;
            }
            value = value.substring(0, index) + '\\' + value.substring(index);
            index += 2;
        }

        return value;
    }

    private String unescape(String value)
    {
        int index = 0;
        while (true)
        {
            index = value.indexOf('\\', index);
            if (index < 0)
            {
                break;
            }
            if (index + 1 == value.length())
            {
                throw new DatabaseException("Invalid escaping");
            }

            if (value.charAt(index + 1) == '\\')
            {
                value = value.substring(0, index) + value.substring(index + 1);
            }
            else if (value.charAt(index + 1) == ',')
            {
                value = value.substring(0, index) + value.substring(index + 1);
            }
            else
            {
                throw new DatabaseException("Invalid escaping");
            }
            
            index++;
        }

        return value;
    }

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    public int getRow()
    {
        return rows;
    }

    public int getRows()
    {
        return rows;
    }

    public int getColumns()
    {
        try
        {
            if (meta == null)
            {
               meta = set.getMetaData();
            }
            return meta.getColumnCount();
        }
        catch (SQLException exception)
        {
            throw new DatabaseException(exception);
        }
    }

    public String getName(int i)
    {
        try
        {
            if (meta == null)
            {
                meta = set.getMetaData();
            }
            String name = meta.getColumnName(i);
            if ((name == null) || (name.length() == 0))
            {
                name = "column" + i;
            }

            return name; 
        }
        catch (SQLException exception)
        {
            throw new DatabaseException(exception);
        }
    }

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    public boolean isNull(int column)
    {
        try
        {
            return (set.getObject(column) == null);
        }
        catch (SQLException exception)
        {
            throw new DatabaseException(exception);
        }
    }
    
    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    public String getString(int column)
    {
        return getString(column, true);
    }

    public String getString(int column, boolean trim)
    {
        try
        {
            String value;

            if (input != null)
            {
                value = values[column - 1];
                if (trim)
                {
                    value = value.trim();
                }

                if (value.length() <= 3)
                {
                    value = StringIntern.intern(value);
                }

                return value;
            }

            value = set.getString(column);
            if (value == null)
            {
                value = "";
            }

            if (trim)
            {
                value = value.trim();
            }

            if (value.length() <= 3)
            {
                value = StringIntern.intern(value);
            }

            return value;
        }
        catch (SQLException exception)
        {
            throw new DatabaseException(exception);
        }
    }

    public int getInteger(int column)
    {
        try
        {
            if (input != null)
            {
                if (values[column - 1].length() == 0)
                {
                    return 0;
                }
                return Integer.parseInt(values[column - 1]);
            }

            return set.getInt(column);
        }
        catch (SQLException exception)
        {
            throw new DatabaseException(exception);
        }
    }

    public long getLong(int column)
    {
        try
        {
            if (input != null)
            {
                if (values[column - 1].length() == 0)
                {
                    return 0;
                }
                return Long.parseLong(values[column - 1]);
            }

            return set.getLong(column);
        }
        catch (SQLException exception)
        {
            throw new DatabaseException(exception);
        }
    }

    public double getDouble(int column)
    {
        try
        {
            if (input != null)
            {
                if (values[column - 1].length() == 0)
                {
                    return 0;
                }
                return Double.parseDouble(values[column - 1]);
            }

            return set.getDouble(column);
        }
        catch (SQLException exception)
        {
            throw new DatabaseException(exception);
        }
    }

    public BigDecimal getBigDecimal(int column)
    {
        try
        {
            if (input != null)
            {
                if (values[column - 1].length() == 0)
                {
                    return new BigDecimal(0);
                }
                return new BigDecimal(values[column - 1]);
            }

            return set.getBigDecimal(column);
        }
        catch (SQLException exception)
        {
            throw new DatabaseException(exception);
        }
    }

    public long getDate(int column)
    {
        try
        {
            if (input != null)
            {
                if (values[column - 1].length() == 0)
                {
                   return 0;
                }
                return Long.parseLong(values[column - 1]);
            }

            java.sql.Timestamp date = set.getTimestamp(column);

            if (date == null)
            {
                return 0;
            }
            return date.getTime();
        }
        catch (SQLException exception)
        {
            throw new DatabaseException(exception);
        }
    }

    public String getCLOB(int column)
    {
        if (input != null)
        {
            throw new DatabaseException("Not supported");
        }

        try
        {
            Object object = set.getObject(column);
            if (object == null)
            {
                return null;
            }

            if ((oracleCLOBClass != null) && object.getClass().isAssignableFrom(oracleCLOBClass) &&
                (clobLength != null) && (clobGetSubString != null))
            {
                return (String)clobGetSubString.invoke(oracleCLOBClass, 1, (Long)clobLength.invoke(oracleCLOBClass));
            }

            throw new RuntimeException("Not implemented");
        }
        catch (Exception exception)
        {
            throw new DatabaseException(exception);
        }
    }

    public Object getObject(int column)
    {
        if (input != null)
        {
            throw new DatabaseException("Not supported");
        }

        try
        {
            Object object = set.getObject(column);
            if (object == null)
            {
                return null;
            }

            if ((oracleTimestampClass != null) && object.getClass().isAssignableFrom(oracleTimestampClass))
            {
                object = set.getTimestamp(column);
            }

            return object;
        }
        catch (SQLException exception)
        {
            throw new DatabaseException(exception);
        }
    }
}
