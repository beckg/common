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
import java.math.*;
import java.sql.*;
import java.util.zip.*;

import org.aventinus.util.*;

//-----------------------------------------------------------------------------------
//
//-----------------------------------------------------------------------------------
public class TestDatabase
{
    private static Logger logger = Logger.logger(TestDatabase.class);

    private Database database;
    private int value10;
    private boolean useProc = false;

    public static void main(String[] args) 
    {
        new TestDatabase().test();
    }

    public TestDatabase() 
    {
    }

    public void test()
    {
        try
        {
//            System.setProperty("DEF_sqlHostname", "192.168.252.211");
//            System.setProperty("DEF_sqlPort", "1521");
//            System.setProperty("DEF_sqlUsername", "gordon");
//            System.setProperty("DEF_sqlPassword", "godollo");
//            System.setProperty("DEF_sqlService", "xe");
//            System.setProperty("DEF_sqlJDBC", "oracle:thin:oracle.jdbc.driver.OracleDriver");

            System.setProperty("DEF_sqlHostname", "lonuc24902.fm.rbsgrp.net");
            System.setProperty("DEF_sqlPort", "1623");
            System.setProperty("DEF_sqlUsername", "FUTOB_OWNER_04");
            System.setProperty("DEF_sqlPassword", "FUTOBOWNER04");
            System.setProperty("DEF_sqlService", "DOLNFOB1");
            System.setProperty("DEF_sqlJDBC", "oracle:thin:oracle.jdbc.driver.OracleDriver");

            database = new Database();

            StringBuilder buffer = new StringBuilder();

            drop();

            while(true)
            {
                buffer.setLength(0);
                buffer.append("create table TEST (");
                buffer.append(" VALUE").append(10).append(" int");
                for (int i = 1; i < 26; i++)
                {
                    buffer.append(", VALUE").append(i + 10).append(" varchar2(20)");
                }
                buffer.append(") INITRANS 5");
                database.executeUpdate(buffer.toString());

                database.executeUpdate("create unique index TEST_IDX1 on TEST(VALUE10) INITRANS 5");
                database.executeUpdate("create index TEST_IDX2 on TEST(VALUE11,VALUE15,VALUE22) INITRANS 5");
                database.executeUpdate("create index TEST_IDX3 on TEST(VALUE12,VALUE15,VALUE22) INITRANS 5");

                if (useProc)
                {
                    buffer.setLength(0);
                    buffer.append("create or replace procedure TESTPROC (P10 in number");
                    for (int i = 1; i < 26; i++)
                    {
                        buffer.append(", P").append(i + 10).append(" in varchar2");
                    }
                    buffer.append(")");
                    buffer.append(" is");
                    buffer.append(" begin");
                    buffer.append("   insert into TEST values(P10");
                    for (int i = 1; i < 26; i++)
                    {
                        buffer.append(", P").append(i + 10);
                    }
                    buffer.append(");");
                    buffer.append(" end TESTPROC;");

                    database.executeUpdate(buffer.toString());

                    showErrors("TESTPROC");
                }
//System.exit(0);

//database.executeUpdate("alter session set sql_trace=true");

                logger.log("Start...");
                long start = DateTime.now();

                value10 = 0;
                int threads = 10;
                int transactions = 50;
                int batchsize = 20;
                insert(threads, transactions, batchsize);

                logger.log("... end");

                logger.log("rate = " + ((double)(threads * transactions * batchsize) * 1000 / (DateTime.now() - start)));

                IntegerRetriever iretriever = new IntegerRetriever();
                database.executeQuery("select count(*) from TEST", iretriever);
                logger.log("count = " + iretriever.getValue());

                MyRetriever retriever = new MyRetriever();
                start = DateTime.now();
                database.executeQuery("select * from TEST where VALUE11 like 'C%'", retriever);
                logger.log("select rows=" + retriever.rows.size() 
                               + " rate=" + ((double)retriever.rows.size() * 1000 / (DateTime.now() - start)));

//                drop();
break;
            }
        }
        catch(Throwable exception)
        {
            logger.fatal(exception);
        }
    }

    private void insert(final int numberThreads, final int transactions, final int batchsize)
    {
        Thread[] threads = new Thread[numberThreads];

        for (int i = 0; i < numberThreads; i++)
        {
            final int thread = i;
            threads[i] = new Thread(new Runnable()
                            {
                                public void run()
                                {
                                    threadInsert(thread, transactions, batchsize);
                                }
                            });
            threads[i].start();
        }

        try
        {
            for (int i = 0; i < numberThreads; i++)
            {
                threads[i].join();
            }
        }
        catch (InterruptedException exception)
        {
            throw new RuntimeException(exception);
        }
    }

    private void threadInsert(int thread, int transactions, int batchsize)
    {
        for (int i = 0; i < transactions; i++)
        {
            database.begin();

            if (useProc)
            {
                StringBuilder buffer = new StringBuilder();
                buffer.append("{call TESTPROC (");
                for (int p = 0; p < 26; p++)
                {
                    if (p > 0)
                        buffer.append(",");
                    buffer.append("?");
                }
                buffer.append(")}");

                DatabaseStatement statement = database.prepareStatement(buffer.toString());

                for (int j = 0; j < batchsize; j++)
                {
                    Object[] values = createValues(thread);
                    statement.setInteger(1, (Integer)values[0]);
                    for (int k = 1; k < 26; k++)
                    {
                        statement.setString(k + 1, (String)values[k]);
                    }
//                    statement.executeUpdate();  
                    statement.addBatch();  
                }
                statement.executeBatch();  
            }
            else
            {
                String[] inserts = new String[10];
                for (int j = 0; j < batchsize; j++)
                {
                    Object[] values = createValues(thread);
                    inserts[j % 10] = createInsert(values);
                    if (j % 10 == 9)
                    {
                        database.executeUpdate(inserts);
                    }
                }
            }

            database.commit();
        }
    }

    private Object[] createValues(int thread)
    {
        Object[] values = new Object[26];

        for (int i= 0; i < 26; i++)
        {
            if (i == 0)
            {
                synchronized(this)
                {
//                    values[i] = new Integer(100000 * thread + ++value10);
                    values[i] = new Integer(++value10);
                }
            }
            else
            {
                StringBuilder buffer = new StringBuilder();
                if (! useProc)
                   buffer.append("'");
                for (int j = 0; j < 20; j++)
                {
                    int k = (int)(Math.random() * 26);
                    if ((k < 0) || (k > 25))
                    {
                        k = 0;
                    }
                    buffer.append("ABCDEFGHIJKLMNOPQRSTUVWXYZ".charAt(k));
                }
                if (! useProc)
                    buffer.append("'");
                values[i] = buffer.toString();
            }
        }

        return values;
    }

    private String createInsert(Object[] values)
    {
        StringBuilder buffer = new StringBuilder();
        buffer.append("insert into TEST values(");
        for (int i= 0; i < 26; i++)
        {
            if (i > 0)
                buffer.append(",");
            buffer.append(values[i]);
        } 
        buffer.append(")");
        return buffer.toString();
    }

    private class MyRetriever extends RetrieverAdapter
    {
        private ArrayList<ArrayList<String>> rows = new ArrayList<ArrayList<String>>();

        public void processRow()
        {
            ArrayList<String> row = new ArrayList<String>();
            rows.add(row);
            for (int i = 0; i < 20; i++)
            {
                row.add(getString(i+1));
            }
        }
    }

    private void drop()
    {
        StringBuilder buffer = new StringBuilder();

        try
        {
            buffer.setLength(0);
            buffer.append("drop table TEST");
            database.executeUpdate(buffer.toString());
            logger.log("Table TEST dropped");
        }
        catch (Exception exception)
        {
            logger.log("Table TEST does not exist");
        }

        try
        {
            buffer.setLength(0);
            buffer.append("drop proc TESTPROC");
            database.executeUpdate(buffer.toString());
            logger.log("Table TESTPROC dropped");
        }
        catch (Exception exception)
        {
            logger.log("Proc TESTPROC does not exist");
        }
    }

    private void showErrors(String name)
    {
        logger.info("Executing selectErrors...");
        String query = "select NAME, TYPE, TEXT from USER_ERRORS where NAME = " + Toolbox.sqlEscape(name);
        RetrieverAdapter retriever = new RetrieverAdapter()
                             {
                                 public void processRow()
                                 {
                                     logger.info(">>> error: name[" + getString(1) + "] type[" + getString(2) + "] text[" + getString(3) + "]");
                                 }
                             };
        database.executeQuery("DEF", query, retriever);

        if (retriever.getRows() > 0)
        {
            throw new IllegalArgumentException("Retrieved errors");
        }

        logger.info("... executed statement");
    }
}
