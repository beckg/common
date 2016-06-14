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
// The java Tokenizer is crippled for some uses.
// We properly account for quoted tokens in the string. The quoting character 
//       inside a token must be escaped by repetition, we remove the repetition.
//       Leading or trailing characters in a token containing quotes will cause
//       an error. We remove the quotes.
// The tab character and the space character cannot both be delimiters at the same time.
// If the tab character is not part of the delimiter set then we treat it as space.
// We properly return empty tokens where two delimiters occur together - unless this 
//       is the space character acting as delimiter.
// We remove leading/trailing spaces - unless these are protected with quotes. 
// I really struggle to see the use case for mixing \n and/or \r with other delimiters, 
//       or for specifying \r without \n. However, if both are specified and they occur 
//       as \r\n in the input then this counts as only one delimiter.
// An empty String returns one (empty) token.
//
// Clearly the previous rules create wierd situations where we use a space character 
//       as a  delimiter. In particular if the input String starts with one or more 
//       spaces then we will return one empty token at the start, and if it ends
//       with one or more spaces then we will return one empty token at the end.
// The reality is that space is a poor delimiter in a free format String.
// You really cannot use " or ' as delimiters !
//*************
// There should be a choice as to whether or not I care about quotes or not - in particular
//    when tokenizing lines where quotes may not span lines.
//-----------------------------------------------------------------------------------
public class Tokenizer
{
    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    private static Logger logger = Logger.logger(Tokenizer.class);

    private String mInput;
    private String mDelimiters;
    private int mOffset = 0;
    private boolean mIgnoreQuotes = false;

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    public static void main(String[] args) 
    {
        try
        {
            String[][] tests = {{"", ","},
                                {"", " "},
                                {"a,b,c", ","}, 
                                {"'a,a',b,'c''c',d", ","},
                                {"  a,  b, c ", ","}, 
                                {"  a,  b, c ", " "},
                                {"  a, \t b, c \t ", " "}};

            for (int i = 0; i < tests.length; i++)
            {
                Tokenizer tokenizer = new Tokenizer(tests[i][0], tests[i][1]);
                for (int j = 0; tokenizer.hasNext(); j++)
                {
                    if (j > 10)
                    {
                        throw new IllegalArgumentException("#tokens");
                    }
                    logger.log("test[" + i + "] token[" + tokenizer.next() + "]");
                }
            }
        }
        catch (Throwable exception)
        {
            logger.fatal(exception);
        }
    }

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    public Tokenizer(String input, String delimiters)
    {
        this(input, delimiters, false);
    }

    public Tokenizer(String input, String delimiters, boolean ignoreQuotes)
    {
        mDelimiters = delimiters;
        mIgnoreQuotes =  ignoreQuotes;

        if (mDelimiters.length() == 0)
        {
            throw new IllegalArgumentException("Must have at least one delimiter");
        }

        if (mDelimiters.indexOf('\'') >= 0)
        {
            throw new IllegalArgumentException("\"'\" cannot be a delimiter");
        }

        if (mDelimiters.indexOf('"') >= 0)
        {
            throw new IllegalArgumentException("\"\"\" cannot be a delimiter");
        }

        if ((mDelimiters.indexOf(' ') >= 0) && (mDelimiters.indexOf('\t') >= 0))
        {
            throw new IllegalArgumentException("\" \" and \"\\t\" cannot both be delimiters");
        }

        mInput = input;
    }

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    public boolean hasNext()
    {
        return (mOffset <= mInput.length());
    }

    public String next()
    {
        if (mOffset > mInput.length())
        {
            throw new IllegalStateException("next() when none");
        }

        int current = mOffset;
        char inquote = ' ';
        while (true)
        {
            if (current == mInput.length())
            {
                if (inquote != ' ')
                {
                    throw new IllegalArgumentException("Improper quoting");
                }

                // The result may be held onto for a long time. We really do not want this String to 
                //       be dependent on the original input data String content
                String value = new String(mInput.substring(mOffset, current).trim());
                if (value.length() <= 3)
                {
                    value = StringIntern.intern(value);
                }

                mOffset = ++current;
                return value;
            }

            char input = mInput.charAt(current);

            if ((input == '\t') && (mDelimiters.indexOf('\t') < 0))
            {
                input = ' ';
            }

            if (inquote == ' ')
            {
                if (mIgnoreQuotes)
                {
                }
                else if ((input == '"') || (input == '\''))
                {
                    if (mInput.substring(mOffset, current).trim().length() > 0)
                    {
                        throw new IllegalArgumentException("Leading character(s) prior to quote");
                    }

                    inquote = input;
                    current++;
                    continue;
                }
            }
            else
            {
                if (input == inquote)
                {
                    if (current + 1 < mInput.length())
                    {
                        if (mInput.charAt(current + 1) == inquote)
                        {
                            mInput = mInput.substring(0, current + 1) + mInput.substring(current + 2);
                        }
                        else
                        {
                            inquote = ' ';
                        }
                    }
                    else
                    {
                        inquote = ' ';
                    }
                }
                current++;
                continue;
            }     

            if (mDelimiters.indexOf(input) >= 0)
            {
                if ((input == '\r') && (current + 1 < mInput.length()) &&
                    (mInput.charAt(current + 1) == '\n') && (mDelimiters.indexOf('\n') >= 0))
                {
                    current++;
                }

                String value = mInput.substring(mOffset, current).trim(); 

                if (value.length() > 0)
                {
                    if ((value.charAt(0) == '"') || (value.charAt(0) == '\''))
                    {
                        char quote = value.charAt(0);
                        if (quote != value.charAt(value.length() - 1))
                        {
                            throw new IllegalArgumentException("Trailing character(s) after to quote");
                        }
                        value = value.substring(1, value.length() - 1);
                    }
                }

                // The result may be held onto for a long time. We really do not want this String to 
                //         be dependent on the original input String data. So we either intern it or new it.
                if (value.length() <= 3)
                {
                    value = StringIntern.intern(value);
                }
                else
                {
                    value = new String(value);
                }

                if (input == ' ')
                {
                    while (current < mInput.length())
                    {
                        input = mInput.charAt(current);
                        if ((input == '\t') && (mDelimiters.indexOf('\t') < 0))
                        {
                            input = ' ';
                        }

                        if (input == ' ')
                        {
                            current++;
                        }
                        else
                        {
                            break;
                        }
                    }
                }
                else
                {
                    current++;
                }
                mOffset = current;
                return value;
            }
            else
            {
                current++;
            }
        }
    }
}
