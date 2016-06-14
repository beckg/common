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
package org.aventinus.gui;

import java.awt.*;
import javax.swing.*;

public class DefaultExceptionHandler implements Thread.UncaughtExceptionHandler
{  
    private Component anchor;
    private boolean active = false;
    private int count = 0;

    private DefaultExceptionHandler(Component anchor)
    {
        this.anchor = anchor;
    }

    public void uncaughtException(Thread thread, Throwable exception) 
    {
        String message = exception.getMessage();
        if (message == null)
        {
            message = exception.getClass().getName();
            if (message.lastIndexOf('.') >= 0)
            {
                message = message.substring(message.lastIndexOf('.') + 1);
            }
        }
        if (message.equals("Suppress action"))
            return;
        exception.printStackTrace();
        count++;
        if (active || (count > 5))
            return;
        active = true;
        JOptionPane.showMessageDialog(anchor, "An error occurred: " + message, 
                                      "Debug", JOptionPane.ERROR_MESSAGE);
        active = false;
    }

    public static void install()
    {
        install(null);
    }

    public static void install(Component anchor)
    {
        Thread.setDefaultUncaughtExceptionHandler(new DefaultExceptionHandler(anchor));
        System.setProperty("sun.awt.exception.handler", DefaultExceptionHandler.class.getName());
    }
}
