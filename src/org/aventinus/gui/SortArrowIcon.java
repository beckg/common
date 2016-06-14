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

//-----------------------------------------------------------------------------------
//
//-----------------------------------------------------------------------------------
public class SortArrowIcon implements Icon
{
    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    public static final int NONE = 0;
    public static final int DESCENDING = 1;
    public static final int ASCENDING = 2;
    public static final int NEUTRAL = 3;

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    protected int m_direction;
    protected int m_width = 8;
    protected int m_height = 8;

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    public SortArrowIcon(int direction)
    {
        m_direction = direction;
    }

    public int getIconWidth()
    {
        return m_width;
    }

    public int getIconHeight()
    {
        return m_height;
    }

    public void paintIcon(Component c, Graphics g, int x, int y)
    {
        Color bg = c.getBackground();
        
        Color light = bg.brighter();
        Color shade = bg.darker();

        int w = m_width;
        int h = m_height;
        int m = w / 2;

        if (m_direction == ASCENDING)
        {
            g.setColor(bg);
            g.fillRect(x, y, w, h);
            g.setColor(shade);
            g.drawLine(x, y, x + w, y);
            g.drawLine(x, y, x + m, y + h);
            g.setColor(light);
            g.drawLine(x + w, y, x + m, y + h);
        }

        if (m_direction == DESCENDING)
        {
            g.setColor(bg);
            g.fillRect(x, y, w, h);
            g.setColor(shade);
            g.drawLine(x + m, y, x, y + h);
            g.setColor(light);
            g.drawLine(x, y + h, x + w, y + h);
            g.drawLine(x + m, y, x + w, y + h);
        }

        if (m_direction == NEUTRAL)
        {
            g.setColor(bg);
            g.fillRect(x, y, w, h);
            g.setColor(shade);
            g.drawLine(x, y, x, y + h);
            g.drawLine(x, y + h, x + w, y + h);
            g.setColor(light);
            g.drawLine(x + w, y + h, x + w, y);
            g.drawLine(x + h, y, x, y);
        }
    }
}
