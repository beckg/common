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
import java.net.*;
import java.io.*;
import java.sql.*;

import org.aventinus.util.*;

//-----------------------------------------------------------------------------------
//
//-----------------------------------------------------------------------------------
public class DatabaseStatement
{
    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    private Database.LocalConnection connection;
    private PreparedStatement statement;

    private String text;
    private Object[] values;

    public DatabaseStatement(Database.LocalConnection connection, String text)
    {
        try
        {
            this.connection = connection;
            this.text = text;

            statement = connection.getConnection().prepareStatement(text);

            if (connection.isLogging())
            {
                values = new Object[0];
            }
        }
        catch (SQLException exception)
        {
            throw new DatabaseException(exception);
        }
    }

    private void storeValue(int index, Object value)
    {
        if (index >= values.length)
        {
            Object[] temp = values;
            values = new Object[index + 1];
            System.arraycopy(temp, 0, values, 0, temp.length);
        }

        values[index] = value;
    }


    public void close()
    {
        if (! connection.contains(this))
        {
            throw new DatabaseException("Statement is no longer part of connection - invalid use of release ?");
        }

        try
        {
            statement.close();
            statement = null;

            connection.remove(this);
        }
        catch (SQLException exception)
        {
            throw new DatabaseException(exception);
        }
    }

    public void executeQuery(Retriever retriever)
    {
        executeQuery(retriever, 0);
    }

    public void executeQuery(Retriever retriever, int maxrows)
    {
        if (! connection.contains(this))
        {
            throw new DatabaseException("Statement is no longer part of connection - invalid use of release ?");
        }

        if (connection.isLogging())
        {
            connection.log(text, values, new int[] {maxrows});
        }

        connection.executeQuery(statement, retriever, maxrows);
    }

    public int executeUpdate()
    {
        if (! connection.contains(this))
        {
            throw new DatabaseException("Statement is no longer part of connection - invalid use of release ?");
        }

        if (connection.isLogging())
        {
            connection.log(text, values, new int[] {0});
        }

        return connection.executeUpdate(statement);
    }

    public void addBatch()
    {
        connection.addBatch(statement);
    }

    public int[] executeBatch()
    {
        return connection.executeBatch(statement);
    }

    public void setString(int index, String value)
    {
        if (statement == null)
        {
            throw new DatabaseException("Statement already closed");
        }

        try
        {
            statement.setString(index, value);

            if (values != null)
            {
                storeValue(index - 1, value);
            }
        }
        catch (SQLException exception)
        {
            throw new DatabaseException(exception);
        }
    }

    public void setInteger(int index, int value)
    {
        if (statement == null)
        {
            throw new DatabaseException("Statement already closed");
        }

        try
        {
            statement.setInt(index, value);

            if (values != null)
            {
                storeValue(index - 1, Integer.valueOf(value));
            }
        }
        catch (SQLException exception)
        {
            throw new DatabaseException(exception);
        }
    }

    public void setLong(int index, long value)
    {
        if (statement == null)
        {
            throw new DatabaseException("Statement already closed");
        }

        try
        {
            statement.setLong(index, value);

            if (values != null)
            {
                storeValue(index - 1, Long.valueOf(value));
            }
        }
        catch (SQLException exception)
        {
            throw new DatabaseException(exception);
        }
    }

    public void setDouble(int index, double value)
    {
        if (statement == null)
        {
            throw new DatabaseException("Statement already closed");
        }

        try
        {
            statement.setDouble(index, value);

            if (values != null)
            {
                storeValue(index - 1, Double.valueOf(value));
            }
        }
        catch (SQLException exception)
        {
            throw new DatabaseException(exception);
        }
    }

    public void setDateTime(int index,  DateTime value)
    {
        if (statement == null)
        {
            throw new DatabaseException("Statement already closed");
        }

        try
        {
            statement.setDate(index, new java.sql.Date(value.getTime()));

            if (values != null)
            {
                storeValue(index - 1, value);
            }
        }
        catch (SQLException exception)
        {
            throw new DatabaseException(exception);
        }
    }
}
