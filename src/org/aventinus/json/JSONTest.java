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
public class JSONTest
{
    private static Toolbox toolbox = Toolbox.toolbox();
    private static Logger logger = Logger.logger(JSONTest.class);

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    public static void main(String[] args) 
    {
        new JSONTest().run();
    }

    public JSONTest() 
    {
    }

    public void run()
    {
//        JSONObject.verbose(true);

        String value;
        JSONObject object;

        try
        {

            test("{}");
//        test("{a:1}");
//        test("{a:{}");
//        test("{1}", true);
//        test("{a:1,b:2}");
//        test("{a:1 b:2}");
//        test("{1 b:2}", true);
//        test("{a:1 2}", true);
//        test("{a:1 ,}", true);
//        test("{, a:1 }", true);
//        test("{a:1 ,, b:2}", true);
//        test(" { a : 1 , b : 2 } ");

//        test("{a:[]}");
//        test("{a:[2]}");
//        test("{a:[2 3]}");
//        test("{a:[2,3]}");
//        test("{a:[b:2]}", true);
//        test("{a:[,3]}", true);
//        test("{a:[2,,3]}", true);
//        test("{a:[2,]}", true);

//        test("{a:{}}");
//        test("{a:{b:2}}");
//        test("{a:{b:2 c:3}}");
//        test("{a:{b:2,c:3}}");
//        test("{a:{2}}", true);
//        test("{a:{,c:3}}", true);
//        test("{a:{b:2,,c:3}}", true);
//        test("{a:{b:2,}}", true);

//        test("{a:{b:{}}}");
//        test("{a:{[]}}");
//        test("{a:[b:{}]}");
//        test("{a:[[]]}");

//        test("{a:[{b:2}]}");
//        test("{a:[{b:2 c:3}]}");

//        test("{a:{c:[2,3]} b:{d:[4,5]}}");
//        test(" { a : { c : [ 2 , 3 ] }  b : { d : [ 4 , 5 ] } } ");

//          object = new JSONObject("{\"a\":{c:[2,3]} b:{d:[4,5], h:{i:3}, j:p} c:6 d:\"7\" e:[2, {f:9}, [10, 11]]}");
//          test(object, "getString('c')", object.getString("c"), "6");
//          test(object, "getString('d')", object.getString("d"), "7");
//          test(object, "getObject('a').toString()", object.getObject("a").toString(), "{c:[2,3]}");

//          test(object, "getObject('b').toString()", object.getObject("b").toString(), "{d:[4,5], h:{i:3}, j:p}");
//          test(object, "getObject('b').getString('j')", object.getObject("b").getString("j"), "p");
//          test(object, "getObject('b').getObject('h').toString()", object.getObject("b").getObject("h").toString(), "{i:3}");
//          test(object, "getObject('b').getArray('d').toString()", object.getObject("b").getArray("d").toString(), "[4,5]");
//          test(object, "getObject('b').getArray('d').getString(1)", object.getObject("b").getArray("d").getString(1), "5");

//          test(object, "getArray('e').getString(0)", object.getArray("e").getString(0), "2");
//          test(object, "getArray('e').getObject(1).toString()", object.getArray("e").getObject(1).toString(), "{f:9}");
//          test(object, "getArray('e').getArray(2).toString()", object.getArray("e").getArray(2).toString(), "[10,11]");

//        test("{a:\"1\",b:2}");
//        test("{a:\"1\" b:2}");
//        test("{a:\"1\"\tb:2}");
//        test("{ a : 1 , b : \"3\" }");
//        test("{ a : 1 , b : \"3\\\"\" }");
//        test("{ a : 1 , b : {c:3, b:4} }");
//        test("{ a : 1 , b : [], c : d }");
//        test("{ a : 1  b : [] c : d }");
//        test("{ a : 1 , b : [3, 4], c : d }");
//        test("{ a : 1 , b : [3, {c:4}], c : d }");
//        test("{ a : 1 b : [3 {c:4}] c : d }");

//        value = "{ a : \"\\tz\\n\" , b : [3, {c:\"\\t4\"}], d : {e:1,f:2} }";
//        test(value);
//        object = new JSONObject(value);
//        logger.log("a=[" + object.toString("a") + "]");
//        logger.log("b[1]=[" + object.getArray("b").toString(1) + "]");
//        logger.log("b[1].c=[" + object.getArray("b").getObject(1).toString("c") + "]");
//        logger.log("d=[" + object.toString("d") + "]");

//        value = "{ a : 1 , b : [3, [1, {c:4}]], d : {e:1,f:2} }";
//        test(value);
//        object = new JSONObject(value);
//        logger.log("a=" + object.toString("a"));
//        logger.log("b=" + object.toString("b"));
//        logger.log("b.size()=" + object.getArray("b").size());
//        logger.log("b[0]=" + object.getArray("b").toString(0));
//        logger.log("b[1]=" + object.getArray("b").toString(1));
//        logger.log("b[1][0]="   + object.getArray("b").getArray(1).toString(0));
//        logger.log("b[1][1]="   + object.getArray("b").getArray(1).toString(1));
//        logger.log("b[1][1].c=" + object.getArray("b").getArray(1).getObject(1).toString("c"));
//        logger.log("d=" + object.toString("d"));
//        logger.log("d.f=" + object.getObject("d").toString("f"));
//        logger.log("z=" + object.toString("z"));

//        value = "{a:4, b:[3, {c:4}], d:{e:1, f:2}}";
//        test(value);
//        object = new JSONObject(value);
//        List<String> names = object.getNames();
//        for (String name: names)
//        {
//            logger.log("name=" + name);
//        }
//        JSONMap map = object.getMap();
//        for (String key: map.keySet())
//        {
//            logger.log("key=" + key);
//        }

//        value = "{a:4, b:[3, {c:4}], d:{e:1, f:2}}";
//        object = new JSONObject(value);
//        object.add("h", "4");
//        object.add("g", 5);
//        JSONObject object2 = new JSONObject("{ a : 1 , b : [3, {c:4}], c : d }");
//        object.add("k", object2);
//        JSONArray object3 = new JSONArray().add(1).add(2).add(3).add(4);
//        object.add("p", object3);
//        logger.log("obj=" + object.toString());
//        logger.log("h=" + object.toString("h"));
//        logger.log("k.c=" + object.getObject("k").toString("c"));
//        logger.log("p[2]=" + object.getArray("p").toString(2));
//        JSONObject object4 = object.removeObject("k");
//        logger.log("object4=" + object4.toString());
//        logger.log("object=" + object.toString());
//        object.dump("object");
//        object4.dump("k");
//        JSONArray object5 = object.removeArray("b");
//        logger.log("object=" + object.toString());
//        object.dump("object");
//        object5.dump("b");

//        object = new JSONObject();
//        object.key("h");
//        object.startArray();
//        object.value("f");
//        object.startObject();
//        object.key("h");
//        object.value("i");
//        object.endObject();
//        object.endArray();

//        logger.log("object=[" + object.toString() + "]");
//        object.dump();

//        object = new JSONObject();
//        object.add("a", "1");
//        logger.log("a=" + object.getLong("a"));

        }
        catch (Throwable exception)
        {
            logger.log(exception);
        }
    }

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    private static void test(String value)
    {
        test(value, false);
    }

    private static void test(String value, boolean shouldFail)
    {
        logger.log("Considering [" + value + "]");
        try
        {
            JSONObject object = new JSONObject(value);
            object.dump();

            if (shouldFail)
            {
                logger.log("... test failed");
            }
        }
        catch (Throwable exception)
        {
            if (shouldFail)
            {
                return;
            }
            logger.log("... test failed", exception);
        }
    }

    private static void test(JSONObject object, String text, String value, String result)
    {
        logger.log("Considering [" + object.toString() + "] " + text + "=" + value);
        if (! value.equals(result))
        {
            logger.log("... test failed result=" + result);
        }
    }
}
