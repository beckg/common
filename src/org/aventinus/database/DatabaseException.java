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

//-----------------------------------------------------------------------------------------------
//
//-----------------------------------------------------------------------------------------------
public class DatabaseException extends RuntimeException
{
    static final long serialVersionUID = 0;

    private int response;

    public DatabaseException() 
    {
        super();
    }

    public DatabaseException(String message) 
    {
        super(message);
    }

    public DatabaseException(String message, int value) 
    {
        super(message);

        response = value;
    }

    public DatabaseException(Throwable exception) 
    {
        super();

        initCause(exception);
    }

    public DatabaseException(String message, Throwable exception) 
    {
        super(message);
        initCause(exception);
    }

    public int getResponse()
    {
        return response;
    } 
}
