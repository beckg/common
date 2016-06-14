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

import org.aventinus.util.*;

//-----------------------------------------------------------------------------------
//
//-----------------------------------------------------------------------------------
public class JSONPerf
{
    private static Toolbox toolbox = Toolbox.toolbox();
    private static Logger logger = Logger.logger(JSONPerf.class);

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    public static void main(String[] args) 
    {
        new JSONPerf().run();
    }

    public JSONPerf() 
    {
    }

    public void run()
    {
        try
        {
            if (1 == 1)
            {
                JSONObject object = new JSONObject();

                for (int i = 0; i < 1000; i++)
                {
                    object.add("i" + i, i);
                }

                // We get jit out of the way
                for (int i = 0; i < 20000; i++)
                {
                    JSONObject test = new JSONObject(object.toString());
                    if (! test.getString("i0").equals("0"))
                    {
                        throw new JSONException("??");
                    }
//                    if (! test.getString("i0").equals("0"))
//                    {
//                        throw new JSONException("??");
//                    }
                    if (! test.getMap().getString("i0").equals("0"))
                    {
                        throw new JSONException("??");
                    }
                }

                // We test the parsing and extraction of a single value;
                long start = System.currentTimeMillis();
                for (int i = 0; i < 10000; i++)
                {
                    JSONObject test = new JSONObject(object.toString());
//                    if (! test.getString("i0").equals("0"))
//                    {
//                        throw new JSONException("??");
//                    }
                    if (! test.getMap().getString("i0").equals("0"))
                    {
                        throw new JSONException("??");
                    }
                }
                logger.log("Duration1:" + (System.currentTimeMillis() - start));
            }

            //-------------------------------------------------------------------
/*
            if (1 == 1)
            {
                String json = "{";
                for (int i = 0; i < 1000; i++)
                {
                    if (i > 0)
                        json = json + ",";
                    json = json + "\"i" + i + "\":" + i;
                }
                json = json + "}";

                // We get jit out of the way
                for (int i = 0; i < 20000; i++)
                {
                    org.json.JSONObject other = new org.json.JSONObject(json);
                    if (! other.getString("i0").equals("0"))
                    {
                        throw new JSONException("??");
                    }
                }

                // We test the parsing and extraction of a single value;
                long start = System.currentTimeMillis();
                for (int i = 0; i < 10000; i++)
                {
                    org.json.JSONObject other = new org.json.JSONObject(json);
                    if (! other.getString("i0").equals("0"))
                    {
                        throw new JSONException("??");
                    }
                }

                logger.log("Duration2:" + (System.currentTimeMillis() - start));
            }
*/
        }
        catch (Throwable exception)
        {
            logger.log(exception);
        }
    }
}
