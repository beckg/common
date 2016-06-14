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

//-----------------------------------------------------------------------------------
//
//-----------------------------------------------------------------------------------
public final class TextUtil
{
    //------------------------------------------------------------------------------- 
    // 
    //------------------------------------------------------------------------------- 
    private TextUtil() 
    {
    }

    public static String[] splitCSV(String line) 
    { 
        return splitCSV(line, "\"'", -1); 
    } 
 
    public static String[] splitCSV(String line, String quoteMarks) 
    { 
        return splitCSV(line, quoteMarks, -1); 
    } 
 
    public static String[] splitCSV(String line, String quoteMarks, int numtokens) 
    { 
        if ((line == null) || (quoteMarks.indexOf(',') >= 0)) 
        {
            throw new IllegalArgumentException("Invalid parameter"); 
        }
 
        ArrayList<String> tokens = new ArrayList<String>(); 
 
        // A cheat for simpler counting 
        line = line.trim() + ","; 
 
        char inQuote = ' '; 
        for (int i = 0; i < line.length(); ) 
        { 
            if (inQuote != ' ') 
            { 
                if (line.charAt(i) == inQuote) 
                { 
                    if ((line.length() > i + 1) && (line.charAt(i + 1) == inQuote)) 
                    { 
                        line = line.substring(0, i) + line.substring(i + 1); 
                        i ++; 
                    } 
                    else 
                    { 
                        // If are quoting then the quoting must surround the entire token 
                        int index = line.indexOf(',', i); 
                        if (line.substring(i + 1, index).trim().length() > 0) 
                        {
                            throw new IllegalArgumentException("Invalid quoting [" + line + "] (escaped quoted have been reduced)"); 
                        }
 
                        if ((numtokens >= 0) && (tokens.size() >= numtokens)) 
                        {
                            throw new IllegalArgumentException("Spurious text at end of tokens [" + line + "] - too many delimiters"); 
                        }
 
                        tokens.add(line.substring(1, i)); // No trimming 
 
                        inQuote = ' '; 
                        line = line.substring(index + 1).trim(); 
                        i = 0; 
                    } 
                } 
                else 
                {
                    i++; 
                }
            } 
            else if ((i == 0) && (quoteMarks.indexOf(line.charAt(i)) >= 0)) 
            { 
                inQuote = line.charAt(i); 
                i++; 
            }                    
            else if (line.charAt(i) == ',') 
            { 
                if ((numtokens >= 0) && (tokens.size() >= numtokens)) 
                {
                    throw new IllegalArgumentException("Spurious text at end of tokens [" + line + "] - too many delimiters"); 
                }
 
                tokens.add(line.substring(0, i).trim()); 
 
                line = line.substring(i + 1).trim(); 
                i = 0; 
            } 
            else 
            {
                i++; 
            }
        } 
        if (inQuote != ' ') 
        {
            throw new IllegalArgumentException("Incorrect quoting"); 
        }
 
        if (numtokens >= 0) 
        { 
            if (tokens.size() < numtokens) 
            {
                throw new IllegalArgumentException("Insufficient text - too few tokens"); 
            }
        } 
 
        return tokens.toArray(new String[tokens.size()]); 
    } 
}  
