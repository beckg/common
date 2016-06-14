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
import java.lang.reflect.Proxy;
import java.net.*;
import java.io.*;
import java.sql.*;

import org.aventinus.util.*;

//-----------------------------------------------------------------------------------
//
//-----------------------------------------------------------------------------------
public class Database implements Runnable
{
    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    public static final String DEFAULT_TAG = "DEF";

    private static Toolbox toolbox = Toolbox.toolbox();
    private static Logger logger = Logger.logger(Database.class);

    private static HashMap<String, Driver> drivers;
    private ArrayList<LocalConnection> connections;
    private Object oracleCursor = null;
    private Class<?> oracleTimestampClass = null;

    private boolean debugLog = false;

    private int requests = 0;
    private long requestTime = 0;

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    public Database()
    {
        connections = new ArrayList<LocalConnection>();

        if (toolbox.getStringProperty("sqlLogSql", "N").equals("Y"))
            setLogging(true);

        // Start the thead to close conections
        Thread tick = new Thread(this);
        tick.setName("DatabaseTick");
        tick.setDaemon(true);
        tick.start();
    }

    public void setLogging(boolean log)
    {
        debugLog = log;
    }

    public boolean isLogging()
    {
        return debugLog;
    }

    //-------------------------------------------------------------------------------------------
    // Close idle connections after a timeout
    //-------------------------------------------------------------------------------------------
    public void run()
    {
        int minutes = 0;
        while (true)
        {
            try
            {
                // this ticks every 60 seconds
                Thread.sleep(1 * 60 * 1000);

                cleanup();
                minutes++;
                if (minutes > 60)
                {
                    dumpStatistics();
                    minutes = 0;
                }
            }
            catch (InterruptedException exception)
            {
                // ignore
            }
            catch (Throwable exception)
            {
                logger.log(exception);
                // We ignore the exception as it is a background event and does not in 
                //        princpal invalid the application - we just stop cleaning up connections.
                return;
            }
        }
    }

    public void dumpStatistics()
    {
        logger.log("requestCount=" + requests + " avgResponse=" + Toolbox.normalise((double)requestTime / requests, 2));
    }

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    public void executeQuery(String text, Retriever retriever)
    {
        executeQuery(DEFAULT_TAG, text, retriever, 0);
    }

    public void executeQuery(String text, Retriever retriever, int maxrows)
    {
        executeQuery(DEFAULT_TAG, text, retriever, maxrows);
    }

    public void executeQuery(String tag, String text, Retriever retriever)
    {
        executeQuery(tag, text, retriever, 0);
    }

    public void executeQuery(String tag, String text, Retriever retriever, int maxrows)
    {
        requests++;
        long start = System.currentTimeMillis();
        getConnection(tag).executeQuery(text, retriever, maxrows);
        requestTime += (System.currentTimeMillis() - start);
    }

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    public int executeUpdate(String text)
    {
        return executeUpdate(DEFAULT_TAG, new String[] {text})[0];
    }

    public int[] executeUpdate(String[] text)
    {
        return executeUpdate(DEFAULT_TAG, text);
    }

    public int executeUpdate(String tag, String text)
    {
        return executeUpdate(tag, new String[] {text})[0];
    }

    public int[] executeUpdate(String tag, String[] text)
    {
        if (toolbox.getStringProperty(tag + "_sqlReadOnly", "false").equalsIgnoreCase("true"))
        {
            throw new DatabaseException("readonly");
        }

        requests += text.length;
        long start = System.currentTimeMillis();
        int[] result = getConnection(tag).executeUpdate(text);
        requestTime += (System.currentTimeMillis() - start);
        return result;
    }

    //-------------------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------------------
    public void executeSP(String name, String[] paramTypes, Object[] paramValues)
    {
        executeSP(DEFAULT_TAG, name, paramTypes, paramValues, null, null);
    }    

    public void executeSP(String tag, String name, String[] paramTypes, Object[] paramValues)
    {
        executeSP(tag, name, paramTypes, paramValues, null, null);
    }    


    public void executeSP(String name, String[] paramTypes, Object[] paramValues, Retriever retriever)
    {
        executeSP(DEFAULT_TAG, name, paramTypes, paramValues, new Retriever[] {retriever}, null);
    }

    public void executeSP(String tag, String name, String[] paramTypes, Object[] paramValues, Retriever retriever)
    {
        executeSP(tag, name, paramTypes, paramValues, new Retriever[] {retriever}, null);
    }

    public void executeSP(String name, String[] paramTypes, Object[] paramValues, Retriever retriever, int maxrow)
    {
        executeSP(DEFAULT_TAG, name, paramTypes, paramValues, new Retriever[] {retriever}, new int[] {maxrow});
    }

    public void executeSP(String tag, String name, String[] paramTypes, Object[] paramValues, Retriever retriever, int maxrow)
    {
        executeSP(tag, name, paramTypes, paramValues, new Retriever[] {retriever}, new int[] {maxrow});
    }

    public void executeSP(String name, String[] paramTypes, Object[] paramValues, Retriever[] retrievers)
    {
        executeSP(DEFAULT_TAG, name, paramTypes, paramValues, retrievers, null);
    }

    public void executeSP(String tag, String name, String[] paramTypes, Object[] paramValues, Retriever[] retrievers)
    {
        executeSP(tag, name, paramTypes, paramValues, retrievers, null);
    }

    public void executeSP(String name, String[] paramTypes, Object[] paramValues, 
                                                   Retriever[] retrievers, int[] maxrows)
    {
        executeSP(DEFAULT_TAG, name, paramTypes, paramValues, retrievers, maxrows);
    }

    public void executeSP(String tag, String name, String[] paramTypes, Object[] paramValues, 
                                                   Retriever[] retrievers, int[] maxrows)
    {
        requests++;
        long start = System.currentTimeMillis();
        getConnection(tag).executeSP(name, paramTypes, paramValues, retrievers, maxrows);
        requestTime += (System.currentTimeMillis() - start);
    }

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    public DatabaseStatement prepareStatement(String text)
    {
        return prepareStatement(DEFAULT_TAG, text);
    }

    public DatabaseStatement prepareStatement(String tag, String text)
    {
        return prepareStatement(getConnection(tag), text);
    }

    private DatabaseStatement prepareStatement(LocalConnection connection, String text)
    {
        DatabaseStatement statement = new DatabaseStatement(connection, text);

        connection.add(statement);

        return statement;
    }

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    public void begin()
    {
        begin(DEFAULT_TAG);
    }

    public void begin(String tag)
    {
        getConnection(tag).begin();
    }

    public void rollback()
    {
        rollback(DEFAULT_TAG);
    }

    public void rollback(String tag)
    {
        if (connectionExists(tag))
        {
            getConnection(tag).rollback();
            release(tag);
        }
    }

    public void commit()
    {
        commit(DEFAULT_TAG);
    }

    public void commit(String tag)
    {
        if (connectionExists(tag))
        {
            getConnection(tag).commit();
            release(tag);
        }
    }

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    public void closeAll()
    {
        synchronized(this)
        {
            for (int entry = 0; entry < connections.size(); )
            {
                LocalConnection connection = connections.get(entry);

                if (connection.getThread() == null)
                {
                    connections.remove(entry);
                    try
                    {
                        connection.rollback();
                        connection.close();
                    }
                    catch (DatabaseException exception)
                    {
                        // ignore
                    }
                }
                else
                {
                    entry++;
                }
            }
        }
    }

    public void releaseAll()
    {
        synchronized(this)
        {
            for (int entry = 0; entry < connections.size(); )
            {
                LocalConnection connection = connections.get(entry);

                if (connection.getThread() == null)
                {
                    entry++;
                    continue;
                }

                if (! connection.getThread().equals(Thread.currentThread()))
                {
                    entry++;
                    continue;
                }

                if (connection.inError())
                {
                    connections.remove(entry);
                    try
                    {
                        connection.rollback();
                        connection.close();
                    }
                    catch (DatabaseException exception)
                    {
                        // ignore
                    }
                    continue;
                }

                connection.rollback();
                connection.release();
                connection.setThread(null);
                entry++;
            }
        }
    }

    public void release(String tag)
    {
        if (tag.equals(""))
        {
            throw new DatabaseException("Invalid tag[" + tag + "]");
        }

        synchronized(this)
        {
            for (int entry = 0; entry < connections.size(); )
            {
                LocalConnection connection = connections.get(entry);

                if (connection.getThread() == null)
                {
                    entry++;
                    continue;
                }

                if (! connection.getTag().equals(tag))
                {
                    entry++;
                    continue;
                }

                if (! connection.getThread().equals(Thread.currentThread()))
                {
                    entry++;
                    continue;
                }

                if (connection.inError())
                {
                    connections.remove(entry);
                    try
                    {
                        connection.rollback();
                        connection.close();
                    }
                    catch (DatabaseException exception)
                    {
                        // ignore
                    }
                    continue;
                }

                connection.rollback();
                connection.release();
                connection.setThread(null);
                entry++;
            }
        }
    }

    private void cleanup()
    {
        synchronized(this)
        {
            long now = DateTime.now();

            int inuse = 0;
            for (int entry = 0; entry < connections.size(); )
            {
                LocalConnection connection = connections.get(entry);

                if (connection.getThread() == null)
                {
                    // somewhere between 5 & 10 minutes depending on the tick frequency.
                    if ((now - connection.getLastUsed()) / 1000 > 5 * 60)
                    {
                        connections.remove(entry);
                        connection.close();
                    }
                    else
                    {
                        entry++;
                    }
                }
                else
                {
                    inuse++;
                    logger.log("Thread id[" + connection.getThread().hashCode() 
                                             + "] name[" + connection.getThread().getName() 
                                             + "] currently has an assigned connection for tag[" + connection.getTag()
                                             + "]");
                    entry++;
                }
            }

            if (connections.size() != 0)
            {
                if (inuse > 0)
                {
                    logger.log("There are now " + connections.size() + " connections (" + inuse + " inuse)");
                }
            }
        }
    }

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    private boolean connectionExists(String tag)
    {
        if (tag.equals(""))
        {
            throw new DatabaseException("Invalid tag[" + tag + "]");
        }

        synchronized(this)
        {
            for (int entry = 0; entry < connections.size(); entry++)
            {
                LocalConnection connection = connections.get(entry);

                if (connection.getTag().equals(tag))
                {
                    if (connection.getThread() != null)
                    {
                        if (connection.getThread().equals(Thread.currentThread()))
                        {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private LocalConnection getConnection(String tag)
    {
        if (tag.equals(""))
        {
            throw new DatabaseException("Invalid tag[" + tag + "]");
        }

        synchronized(this)
        {
            LocalConnection candidate = null;

            for (LocalConnection connection: connections)
            {
                if (connection.getTag().equals(tag))
                {
                    if (connection.getThread() == null)
                    {
                        candidate = connection;
                    }
                    else if (connection.getThread().equals(Thread.currentThread()))
                    {
                        return connection;
                    }
                }
            }

            if (candidate != null)
            {
                candidate.setThread(Thread.currentThread());
                return candidate;
            }

// not sure why it sometimes fails to find the candidate
            for (int entry = 0; entry < connections.size(); entry++)
            {
                LocalConnection connection = connections.get(entry);
                if (connection.getTag().equals(tag) && (connection.getThread() == null))
                {
                    logger.info("Connection: tag[" + connection.getTag() + ":" + tag + "]=" + connection.getTag().equals(tag) 
                                + " thread[" + (connection.getThread() == null ? "null" : String.valueOf(connection.getThread().hashCode())) + "]");
                    logger.info("Failed to match candidate - exiting", new DatabaseException());
                    System.exit(0);
                }
            }
        }

        LocalConnection connection = createConnection(tag);

        synchronized(this)
        {
            connection.setThread(Thread.currentThread());
            connections.add(connection);

            return connection;
        }
    }

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    private LocalConnection createConnection(String tag)
    {
        ConnectionDetails details = new ConnectionDetails();
        details.tag = tag;
        getConnectionDetails(details);

        LocalConnection connection = new LocalConnection(tag);

        if (details.jdbcProtocol.indexOf("sybase") >= 0)
        {
            Connection conn = connectSybase(details);
            connection.setConnection(conn);
            configureSybase(details, connection);
            return connection;
        }
        else if (details.jdbcProtocol.indexOf("sqlserver") >= 0)
        {
            String[] hostnames = details.hostname.split(",");
            String[] ports = details.port.split(",");

            if ((hostnames.length <= 0) || (hostnames.length != ports.length))
                throw new DatabaseException("Mismatched failover hostnames/ports");

            RuntimeException first = null;
            for (int i = 0; i < hostnames.length; i++)
            {
                details.hostname = hostnames[i].trim();
                details.port = ports[i].trim();

                try
                {
                    Connection conn = connectSqlServer(details);
                    connection.setConnection(conn);
                    configureSqlServer(details, connection);
                    return connection;
                }
                catch (RuntimeException exception)
                {
                    if (first == null)
                        first = exception;
                    logger.error("... alternate " + i + " failed", exception);
                }
            }

            throw first;
        }
        else if (details.jdbcProtocol.indexOf("oracle") >= 0)
        {
            Connection conn = connectOracle(details);
            connection.setConnection(conn);
            configureOracle(details, connection);
            return connection;
        }
        else if (details.jdbcProtocol.indexOf("as400") >= 0)
        {
            Connection conn = connectAS400(details);
            connection.setConnection(conn);
            configureAS400(details, connection);
            return connection;
        }
        else
        {
            throw new DatabaseException("Invalid protocol for tag[" + tag + "]");
        }
    }

    private class ConnectionDetails
    {
        String tag;

        String hostname;
        String port;
        String username;
        String password;
        String service;
        String database;
        String schema;
        String ldap;

        String jdbcProtocol;
        String jdbcDriver;
    }

    private void getConnectionDetails(ConnectionDetails details)
    {
        details.hostname = toolbox.getStringProperty(details.tag + "_sqlHostname", "").trim();
        details.port = toolbox.getStringProperty(details.tag + "_sqlPort", "").trim();
        details.username = toolbox.getStringProperty(details.tag + "_sqlUsername", "").trim();
        details.password = toolbox.getStringProperty(details.tag + "_sqlPassword", "").trim();

        details.service = toolbox.getStringProperty(details.tag + "_sqlService", "").trim();
        details.database = toolbox.getStringProperty(details.tag + "_sqlDatabase", "").trim();
        details.schema = toolbox.getStringProperty(details.tag + "_sqlSchema", "").trim();
        details.ldap = toolbox.getStringProperty("ldap", "").trim();

        String jdbc = toolbox.getStringProperty(details.tag + "_sqlJDBC");
        details.jdbcProtocol = "";
        details.jdbcDriver = "";

        if (jdbc != null)
        {
            int index = jdbc.lastIndexOf(':');
            if (index < 0)
            {
                throw new DatabaseException("Invalid _sqlJDBC details.tag");
            }

            details.jdbcProtocol = jdbc.substring(0, index).trim();
            details.jdbcDriver = jdbc.substring(index + 1).trim();
        }

        String id = toolbox.getStringProperty(details.tag + "_sqlID", "").trim();

        if (id.length() == 0)
        {
            if ((details.ldap.length() == 0) || (details.service.length() == 0))
            {
                if (details.hostname.length() == 0)
                {
                    throw new DatabaseException(details.tag + "_sqlHostname not configured");
                }
            }
            if (details.username.length() == 0)
            {
                throw new DatabaseException(details.tag + "_sqlUsername not configured");
            }
            if (details.password.length() == 0)
            {
                throw new DatabaseException(details.tag + "_sqlPassword not configured");
            }
        }
        else
        {
            if (details.hostname.length() != 0)
            {
                throw new DatabaseException(details.tag + "_sqlHostname conflicts with " + details.tag + "_sqlID");
            }
            if (details.port.length() != 0)
            {
                throw new DatabaseException(details.tag + "_sqlPort conflicts with " + details.tag + "_sqlID");
            }
            if (details.username.length() != 0)
            {
                throw new DatabaseException(details.tag + "_sqlUsername conflicts with " + details.tag + "_sqlID");
            }
            if (details.password.length() != 0)
            {
                throw new DatabaseException(details.tag + "_sqlPassword conflicts with " + details.tag + "_sqlID");
            }
            if (details.service.length() != 0)
            {
                throw new DatabaseException(details.tag + "_sqlService conflicts with " + details.tag + "_sqlID");
            }

            String filename = toolbox.getStringProperty("user.home") + "/.sqlLogins";
            if (toolbox.getStringProperty("sqlLogins") != null)
            {
                filename = toolbox.getStringProperty("sqlLogins");
            }

            try
            {
                BufferedReader reader;

                if (filename.startsWith("http:"))
                {
                    URLConnection connection = new URL(filename).openConnection();
                    reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                }
                else
                {
                    reader = new BufferedReader(new FileReader(filename));
                }

                while(true)
                {
                    String line = reader.readLine();
                    if (line == null)
                    {
                        break;
                    }

                    line = line.trim();
                    if (line.startsWith("#"))
                    {
                        continue;
                    }
                    if (line.startsWith("//"))
                    {
                        continue;
                    }

                    // id hostname port username password [service] [schema=ssss | database=bbbb]

                    String[] parms = line.split("[\t ]+");

                    if (parms.length < 5)
                    {
                        continue;
                    }

                    if (! parms[0].equals(id))
                    {
                        continue;
                    }

                    int i = 5;
                    if (parms.length > i)
                    {
                        if (parms[i].indexOf('=') < 0)
                        {
                            details.service = parms[i];
                            i++;
                        }
                    }

                    while (parms.length > i)
                    {
                        if (parms[i].indexOf('=') < 0)
                        {
                            break;
                        }

                        if (parms[i].startsWith("schema="))
                        {
                            if (! details.schema.equals(""))
                            {
                                break;
                            }
                            details.schema = parms[i].substring(7);
                            i++;
                        }
                        else if (parms[i].startsWith("database="))
                        {
                            if (! details.database.equals(""))
                            {
                                break;
                            }
                            details.database = parms[i].substring(9);
                            i++;
                        }
                        else if (parms[i].startsWith("protocol="))
                        {
                            if (! details.jdbcProtocol.equals(""))
                            {
                                break;
                            }
                            details.jdbcProtocol = parms[i].substring(9);
                            i++;
                        }
                        else if (parms[i].startsWith("driver="))
                        {
                            if (! details.jdbcDriver.equals(""))
                            {
                                break;
                            }
                            details.jdbcDriver = parms[i].substring(7);
                            i++;
                        }
                        else
                        {
                            break;
                        }
                    }

                    if (parms.length > i)
                    {
                        continue;
                    }

                    details.hostname = parms[1];
                    details.port = parms[2];
                    details.username = parms[3];
                    details.password = parms[4];

                    break;
                }
                reader.close();

                if (details.hostname.length() == 0)
                {
                    throw new DatabaseException("ID[" + id + "] not found in sqlLogin file");
                }
            }
            catch (IOException exception)
            {
                throw new DatabaseException("Failed to read sqlLogin file[" + filename + "]", exception);
            }             
        }

        if (details.jdbcProtocol.length() == 0)
        {
            if (details.service.length() > 0)
            {
                details.jdbcProtocol = "oracle:thin";
            }
            else if (details.schema.length() > 0)
            {
                details.jdbcProtocol = "as400";
            }
            else 
            {
                details.jdbcProtocol = "sybase:Tds";
            }
        }

        if (details.jdbcDriver.length() == 0)
        {
            if (details.jdbcProtocol.indexOf("oracle") >= 0)
            {
                details.jdbcDriver = "oracle.jdbc.driver.OracleDriver";
            }
            else if (details.jdbcProtocol.indexOf("as400") >= 0)
            {
                details.jdbcDriver = "com.ibm.as400.access.AS400JDBCDriver";
            }
            else if (details.jdbcProtocol.indexOf("jtds:sqlserver") >= 0)
            {
                details.jdbcDriver = "net.sourceforge.jtds.jdbc.Driver";
            }
            else if (details.jdbcProtocol.indexOf("sqlserver") >= 0)
            {
                details.jdbcDriver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
//                details.jdbcProtocol = "sqlserver";
            }
            else
            {
                details.jdbcDriver = "com.sybase.jdbc3.jdbc.SybDriver";
            }
        }

        if (jdbc == null)
        {
            System.setProperty(details.tag + "_sqlJDBC", details.jdbcProtocol + ":" + details.jdbcDriver);
        }

        if (toolbox.getStringProperty(details.tag + "_sqlReadOnly", "false").equalsIgnoreCase("true"))
        {
            logger.log("tag=" + details.tag + " is readonly");
        }
    }

    private Connection connectDatabase(String jdbcDriver, String mURL, Properties info)
    {
        synchronized(Database.class)
        {
            if (drivers == null)
            {
                drivers = new HashMap<String, Driver>();
            }

            Driver driver = drivers.get(jdbcDriver);
            if (driver == null)
            {
                try
                {
                    Class<?> jdbcClass = Class.forName(jdbcDriver);
                    driver = (Driver)jdbcClass.getConstructor().newInstance((Object[])null);
                    drivers.put(jdbcDriver, driver);
                }
                catch (Exception exception)
                {
                    logger.log("Failed to load jdbc driver[" + jdbcDriver + "] - " + exception.getMessage());
                    throw new DatabaseException(exception.getMessage(), exception);
                }
            }

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
                Class<?> oracleClass = Class.forName("oracle.jdbc.driver.OracleTypes");
                oracleCursor = oracleClass.getField("CURSOR").get(null);
            }
            catch (Exception exception)
            {
                // ignore
            }

            try
            {
                Connection conn = driver.connect(mURL, info);
                if (conn == null)
                {
                    throw new DatabaseException("Failed to create connection from driver[" + jdbcDriver + "]");
                }

                return conn;
            }
            catch (SQLException exception)
            {
                if ((exception.getMessage().indexOf("ConnectException") >= 0) ||
                    (exception.getMessage().indexOf("Connection is already closed") >= 0))
                {
                    throw new DisconnectException(exception);
                }

                throw new DatabaseException(exception.getMessage(), exception);
            }
        }
    }

    private Connection connectSybase(ConnectionDetails details)
    {
        if (details.port.length() == 0)
        {
            throw new DatabaseException(details.tag + "_sqlPort not configured");
        }
        if (details.service.length() > 0)
        {
            throw new DatabaseException(details.tag + "_sqlService only valid for oracle");
        }
        if (details.schema.length() > 0)
        {
            throw new DatabaseException(details.tag + "_sqlSchema only valid for oracle/as400");
        }

        String mURL = "jdbc:" + details.jdbcProtocol + ":" + details.hostname + ":" + details.port;

        Properties info = new Properties();

        info.put("user", details.username);
        info.put("password", details.password);

        if (details.database.length() > 0)
        {
            info.put("database", details.database);
        }

        String localhost = getHostName();
        if (localhost != null)
        {
            info.put("hostname", localhost);
        }

        info.put("applicationname", toolbox.getStringProperty("program", "java"));

        if (toolbox.getStringProperty("pid", "").length() > 0)
        {
            info.put("hostproc", toolbox.getStringProperty("pid", ""));
        }

        String converterClass = "";
//        converterClass = toolbox.getStringProperty("sqlCharSetConverter", "com.sybase.jdbc2.utils.TruncationConverter");
        converterClass = toolbox.getStringProperty("sqlCharSetConverter", "DEFAULT");
        if (! converterClass.equals("DEFAULT"))
        {
            info.put("CHARSET_CONVERTER_CLASS", converterClass);
        }

        String charSet = "";
//        charSet = toolbox.getStringProperty("sqlCharSet", "cp850");
        charSet = toolbox.getStringProperty("sqlCharSet", "DEFAULT");
        if (! charSet.equals("DEFAULT"))
        {
            info.put("CHARSET", charSet);
        }

        String message = "Connecting tag[" + details.tag + "] to URL[" + mURL + "] user[" + details.username + "]";
        if (details.database.length() > 0)
        {
            message = message + " database[" + details.database + "]";
        }
        if (charSet.length() > 0)
        {
            message = message + " CharSet[" + charSet + "]";
        }
        if (converterClass.length() > 0)
        {
            message = message + " ConverterClass[" + converterClass + "]";
        }

        logger.log(message);

        return connectDatabase(details.jdbcDriver, mURL, info);
    }

    private class MessageHandler implements InvocationHandler
    {
        public Object invoke(Object proxy, Method method, Object[] args)
        {
            if (args.length != 1)
                throw new DatabaseException("Invalid call to SybMessageHandler");

            SQLException exception = (SQLException)args[0];

            // The following supresses the ... 
            //     "Transaction count after EXECUTE indicates that a COMMIT or ROLLBACK TRAN is missing." 
            // ... message 
            if (exception.getErrorCode() == 266) 
                return null; 

            // We want to see the message for all others 
            logger.info("Message handler [" + exception.getMessage().trim() + "][" + exception.getErrorCode() + "]"); 

            // ErrorCode of 0 are print statements, so ignore them 
            if (exception.getErrorCode() == 0) 
                return null; 

            // Need a different exception for deadlocks 
            if (exception.getErrorCode() == 1205) 
            { 
                return new DeadlockException("Sybase Deadlock"); 
            } 

            return exception; 
        }
    }

    private void configureSybase(ConnectionDetails details, LocalConnection connection)
    {
        try
        {
            Class<?> klass = Class.forName("com.sybase.jdbcx.SybMessageHandler");
            if (klass == null)
                throw new DatabaseException("Unable to locate SybMessageHandler");

            MessageHandler messageHandler = new MessageHandler();
            Object proxy = Proxy.newProxyInstance(klass.getClassLoader(), new Class<?>[] {klass}, messageHandler);

            Method setSybMessageHandler = connection.getConnection().getClass().getMethod("setSybMessageHandler", klass);
            if (setSybMessageHandler == null)
            {
                throw new DatabaseException("Unable to locate SybaseMessageHandler.setSybMessageHandler()");
            }

            setSybMessageHandler.invoke(connection.getConnection(), proxy);
        }
        catch (Exception exception)
        {
            logger.log("Failed to setSybaseMessageHandler - " + exception.getMessage());
            throw new DatabaseException(exception.getMessage(), exception);
        }

//        connection.connection.setAutoCommit(true);
//        We cannot use the above as it depends on things being installed in
//            the master database, the following is meant to be equivalent.

        connection.executeUpdate("set chained off ");
         
        if (details.database.length() > 0)
        {
            connection.executeUpdate("use " + details.database.trim());
        }
   }

    private Connection connectSqlServer(ConnectionDetails details)
    {
        if (details.port.length() == 0)
        {
            throw new DatabaseException(details.tag + "_sqlPort not configured");
        }
        if (details.service.length() > 0)
        {
            throw new DatabaseException(details.tag + "_sqlService only valid for oracle");
        }
        if (details.schema.length() > 0)
        {
            throw new DatabaseException(details.tag + "_sqlSchema only valid for oracle/as400");
        }

        String mURL = "jdbc:" + details.jdbcProtocol + "://" + details.hostname + ":" + details.port;

        Properties info = new Properties();

        info.put("user", details.username);
        info.put("password", details.password);

        if (details.database.length() > 0)
        {
            info.put("database", details.database);
        }

        String localhost = getHostName();
        if (localhost != null)
        {
            info.put("hostname", localhost);
        }

        info.put("applicationname", toolbox.getStringProperty("program", "java"));

        if (toolbox.getStringProperty("pid", "").length() > 0)
        {
            info.put("hostproc", toolbox.getStringProperty("pid", ""));
        }

        String message = "Connecting tag[" + details.tag + "] to URL[" + mURL + "] user[" + details.username + "]";
        if (details.database.length() > 0)
        {
            message = message + " database[" + details.database + "]";
        }

        logger.log(message);

        return connectDatabase(details.jdbcDriver, mURL, info);
    }

    private void configureSqlServer(ConnectionDetails details, LocalConnection connection)
    {
        try
        {
            connection.getConnection().setAutoCommit(true);
            connection.setHasAutoCommit(true);
        }
        catch (SQLException exception)
        {
            throw new DatabaseException(exception.getMessage(), exception);
        }
    }

    private Connection connectOracle(ConnectionDetails details)
    {
        // "jdbc:oracle:thin:@ldap://ccs.fm.rbsgrp.net:4042/FPBLNP1,cn=OracleContext,dc=fm,dc=rbsgrp,dc=net ..." 

        if (details.ldap.length() == 0)
        {
            if (details.port.length() == 0)
            {
                throw new DatabaseException(details.tag + "_sqlPort not configured");
            }
        }
        if (details.database.length() > 0)
        {
            throw new DatabaseException(details.tag + "_sqlDatabase only valid for sybase");
        }

        if (details.service.length() == 0)
        {
            throw new DatabaseException("Failed to determine service for oracle");
        }

        String mURL = "jdbc:" + details.jdbcProtocol;

        if (details.hostname.length() > 0)
        {
            mURL = mURL + ":@//" + details.hostname + ":" + details.port + "/" + details.service;
        }
        else
        {
            mURL = mURL + ":@";
            String ldap = details.ldap;
            while (true)
            {
                if (ldap.length() == 0)
                {
                    break;
                }

                String token1;
                int index = ldap.indexOf('+');
                if (index < 0)
                {
                    token1 = ldap.trim();
                    ldap = "";
                }
                else
                {
                    token1 = ldap.substring(0, index).trim();
                    ldap = ldap.substring(index + 1).trim();
                }                  
                if (token1.length() > 0)
                {
                    String token2 = "";
                    index = token1.indexOf(',');
                    if (index >= 0)
                    {
                        token2 = token1.substring(index);
                        token1 = token1.substring(0, index);
                    }

                    mURL = mURL + "ldap://" + token1 + "/" + details.service + token2 + " ";
                }
            }

            mURL = mURL.trim();
        }

        Properties info = new Properties();

        info.put("user", details.username);
        info.put("password", details.password);
        info.put("defaultRowPrefetch", "1000"); 
        info.put("v$session.program", toolbox.getStringProperty("program", "java"));

        String message = "Connecting tag[" + details.tag + "] to URL[" + mURL + "] user[" + details.username + "]";
        if (details.schema.length() > 0)
        {
            message = message + " schema[" + details.schema + "]";
        }

        logger.log(message);

        return connectDatabase(details.jdbcDriver, mURL, info);
    }

    private void configureOracle(ConnectionDetails details, LocalConnection connection)
    {
        try
        {
            connection.getConnection().setAutoCommit(true);
            connection.setHasAutoCommit(true);

//connection.executeUpdate("alter session set TIME_ZONE='+00:00'"); 

        }
        catch (SQLException exception)
        {
            throw new DatabaseException(exception.getMessage(), exception);
        }

        if (details.schema.length() > 0)
        {
            connection.executeUpdate("alter session set CURRENT_SCHEMA=" + details.schema.trim());
        }
    }

    private Connection connectAS400(ConnectionDetails details)
    {
        if ((details.port.length() > 0) && (! details.port.equals("0")))
        {
            logger.log(details.tag + "_sqlPort ignored for as400");
        }
        if (details.service.length() > 0)
        {
            throw new DatabaseException(details.tag + "_sqlService only valid for oracle");
        }
        if (details.database.length() > 0)
        {
            throw new DatabaseException(details.tag + "_sqlDatabase only valid for sybase");
        }

        String mURL = "jdbc:" + details.jdbcProtocol + ":" + details.hostname + ";block criteria=2;block size=512";

        Properties info = new Properties();

        info.put("user", details.username);
        info.put("password", details.password);
        info.put("prompt", "false");

//        if (details.schema.length() > 0)
//            info.put("databasename", details.schema);

        String message = "Connecting tag[" + details.tag + "] to URL[" + mURL + "] user[" + details.username + "]";
        if (details.schema.length() > 0)
        {
            message = message + " schema[" + details.schema + "]";
        }

        logger.log(message);

        return connectDatabase(details.jdbcDriver, mURL, info);
    }

    private void configureAS400(ConnectionDetails details, LocalConnection connection)
    {
        try
        {
            connection.getConnection().setAutoCommit(true);
            connection.setHasAutoCommit(true);
        }
        catch (SQLException exception)
        {
            throw new DatabaseException(exception.getMessage(), exception);
        }

        if (details.schema.length() > 0)
        {
            connection.executeUpdate("set SCHEMA " + details.schema.trim());
        }
    }

    //-------------------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------------------
    private void warnings(Statement statement) throws SQLException
    {
        SQLWarning warning = statement.getWarnings();
        while (warning != null)
        {
            int code = warning.getErrorCode();
            if (code != 0)
            {
                String message = warning.getMessage();
                if (message == null)
                {
                    message = "";
                }
                message = message.trim();

                if (message.startsWith("Warning:"))
                {
                    message = message.substring(8).trim();
                }

                logger.log("SQL Warning [" + code + "] " + message);
            }

            warning = warning.getNextWarning();
        }

        statement.clearWarnings();
    }

    private String getHostName()
    {
        try 
        {
            return InetAddress.getLocalHost().getHostName();
        }
        catch (IOException exception)
        {
            return null;
        }
    }

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    public final class LocalConnection
    {
        private Connection connection;
        private String tag;
        private Thread thread;
        private long lastUsed = 0;

        private boolean hasAutoCommit = false;

        private ArrayList<DatabaseStatement> statements;

        private Statement statement;

        private boolean error = false;

        //---------------------------------------------------------------------------
        //
        //---------------------------------------------------------------------------
        private LocalConnection(String tag)
        {
            this.tag = tag;
            statements = new ArrayList<DatabaseStatement>();
        }

        public boolean isLogging()
        {
            return Database.this.debugLog;
        }

        private void setHasAutoCommit(boolean value)
        {
            hasAutoCommit = value;
        }

        public boolean inError()
        {
            return error;
        }

        //---------------------------------------------------------------------------
        //
        //---------------------------------------------------------------------------
        protected Connection getConnection()
        {
            return connection;
        }

        private void setConnection(Connection value)
        {
            connection = value;
        }

        protected String getTag()
        {
            return tag;
        }

        protected Thread getThread()
        {
            return thread;
        }

        private void setThread(Thread value)
        {
            if (statements.size() > 0)
            {
                throw new DatabaseException("Statement list is not empty");
            }

            thread = value;

            if (thread == null)
            {
                lastUsed = DateTime.now();
            }
        }

        public long getLastUsed()
        {
            return lastUsed;
        }

        //---------------------------------------------------------------------------
        //
        //---------------------------------------------------------------------------
        protected void add(DatabaseStatement statement)
        {
            if (statements.contains(statement))
            {
                throw new DatabaseException("Invalid addition of statement");
            }

            statements.add(statement);
        }

        protected void remove(DatabaseStatement statement)
        {
            if (! statements.contains(statement))
            {
                throw new DatabaseException("Invalid removal of statement");
            }

            statements.remove(statement);
        }

        protected boolean contains(DatabaseStatement statement)
        {
            return statements.contains(statement);
        }

        private Statement getSimpleStatement() throws SQLException
        {
            if (statement == null)
            {
                statement = connection.createStatement();
            }

           return statement;
        }

        private void release()
        {
            while (statements.size() > 0)
            {
                // the close removes
                statements.get(0).close();
            }
        }

        //---------------------------------------------------------------------------
        //
        //---------------------------------------------------------------------------
        private void log(String text)
        {
            log(text, 0);
        }

        private void log(String text, int maxrows)
        {
            if (maxrows > 0)
            {
                logger.log("Executing SQL[" + text + "] for tag[" + tag + "] with maxrows[" + maxrows + "]");
            }
            else
            {
                logger.log("Executing SQL[" + text + "] for tag[" + tag + "]");
            }
        }

        private void log(String text, String[] paramTypes, Object[] paramValues, int[] maxrows)
        {
            StringBuilder buffer = new StringBuilder();

            buffer.append("Executing SQL[").append(text).append("](");

            int length;
            if (paramTypes == null)
            {
                length = paramValues.length;
            }
            else
            {
                length = paramTypes.length;
            }
            for (int i = 0; i < length; i++)
            {
                if (i > 0)
                {
                    buffer.append(",");
                }

                if ((paramTypes == null) || (! paramTypes[i].equals('O')))
                {
                    if (paramValues[i] instanceof String)
                    {
                        buffer.append("'").append(paramValues[i].toString()).append("'");
                    }
                    else if (paramValues[i] instanceof DateTime)
                    {
                        buffer.append("'").append(paramValues[i].toString()).append("'");
                    }
                    else
                    {
                        buffer.append(paramValues[i].toString());
                    }
                }
            }
            buffer.append(")");
            buffer.append(" for tag[" + tag + "]");

            if (maxrows != null)
            {
                buffer.append(" with maxrows[");
                for (int i = 0; i < maxrows.length; i++)
                {
                    if (i > 0)
                        buffer.append(",");
                    buffer.append(maxrows[i]);
                }
                buffer.append("]");
            }

            logger.log(buffer.toString());
        }

        protected void log(String text, Object[] paramValues, int[] maxrows)
        {
            log(text, null, paramValues, maxrows);
        }

        //---------------------------------------------------------------------------
        //
        //---------------------------------------------------------------------------
        private void executeQuery(String text, Retriever retriever, int maxrows)
        {
            Statement local = null;

            try
            {
                if (debugLog)
                {
                    log(text, maxrows);
                }

                local = getSimpleStatement();
                boolean resultType = local.execute(text);
                fetchResults(resultType, local, retriever, maxrows);
            }
            catch (SQLException exception)
            {
                cancel(local);
                error = true;
                logger.error("Query failed -" + text);
                throw new DatabaseException(exception.getMessage(), exception);
            }
        }

        protected void executeQuery(PreparedStatement statement, Retriever retriever, int maxrows)
        {
            try
            {
                boolean resultType = statement.execute();
                fetchResults(resultType, statement, retriever, maxrows);
            }
            catch (SQLException exception)
            {
                cancel(statement);
                error = true;
                throw new DatabaseException(exception.getMessage(), exception);
            }
        }

        //---------------------------------------------------------------------------
        //
        //---------------------------------------------------------------------------
        private int executeUpdate(String text)
        {
            return executeUpdate(new String[] {text})[0];
        }

        private int[] executeUpdate(String[] text)
        {
            Statement local = null;
            try
            {
                if (debugLog)
                {
                    for (int i = 0; i < text.length; i++)
                    {
                        log(text[i]);
                    }
                }

                local = getSimpleStatement();

                if (text.length == 1)
                {
                    boolean resultType = local.execute(text[0]);
                    return new int[] {fetchResults(resultType, local, null, 0)};
                }
                else
                {
                    for (int i = 0; i < text.length; i++)
                    {
                        local.addBatch(text[i]);
                    }
                    return local.executeBatch();
                }
            }
            catch (SQLException exception)
            {
                cancel(statement);
                error = true;
                throw new DatabaseException(exception.getMessage(), exception);
            }
        }

        protected int executeUpdate(PreparedStatement statement)
        {
//            if (toolbox.getStringProperty(tag + "_sqlReadOnly", "true").equalsIgnoreCase("true"))
//                throw new DatabaseException("readonly");

            try
            {
                boolean resultType = statement.execute();
                return fetchResults(resultType, statement, null, 0);
            }
            catch (SQLException exception)
            {
                cancel(statement);
                error = true;
                throw new DatabaseException(exception.getMessage(), exception);
            }
        }

        protected void addBatch(PreparedStatement statement)
        {
            try
            {
                statement.addBatch();
            }
            catch (SQLException exception)
            {
                cancel(statement);
                error = true;
                throw new DatabaseException(exception.getMessage(), exception);
            }
        }

        protected int[] executeBatch(PreparedStatement statement)
        {
            try
            {
                return statement.executeBatch();
            }
            catch (SQLException exception)
            {
                cancel(statement);
                error = true;
                throw new DatabaseException(exception.getMessage(), exception);
            }
        }

        //---------------------------------------------------------------------------
        //
        //---------------------------------------------------------------------------
        private void executeSP(String name, String[] paramTypes, Object[] paramValues,
                                            Retriever[] retrievers, int[] maxrows)
        {
            CallableStatement local = null;

            // In this context by paramType we mean returnValue(R) input(I), output(O) or both(IO); we also indicate cursor(C)

            if (paramTypes.length != paramValues.length)
                throw new DatabaseException("Inconsistent type/parameter length");

            if ((retrievers == null) && (maxrows == null))
            {
            }
            else if ((retrievers == null) || ((maxrows != null) && (retrievers.length != maxrows.length)))
                throw new DatabaseException("Inconsistent retrievers/maxrows values/length");

            int cursor = 0;
            for (int entry = 0; entry < paramTypes.length; entry++)
            {
                if ("R:RE:RC:I:IO:O:OC".indexOf(paramTypes[entry]) < 0)
                    throw new DatabaseException("Invalid parameter type");

                if (paramTypes[entry].startsWith("I") && (paramValues[entry] == null))
                    throw new DatabaseException("Missing input parameter");

                if (paramTypes[entry].endsWith("C"))
                {
                    if ((retrievers == null) || (retrievers.length < cursor + 1) || (retrievers[cursor] == null))
                        throw new DatabaseException("Invalid retriever value(s)");
                    cursor++;
                }
            }
            if ((retrievers != null) && (retrievers.length != cursor))
               throw new DatabaseException("Mismatched retrievers/types");

            try
            {
                StringBuilder buffer = new StringBuilder(1000);
                int entry = 0;
                if ((paramTypes.length > 0) && paramTypes[0].startsWith("R"))
                {
                    buffer.append("{?= call ").append(name).append(" (");
                    entry++;
                }
                else
                {
                    buffer.append("{ call ").append(name).append(" (");
                }
                for (; entry < paramTypes.length; entry++)
                {
                    if (buffer.charAt(buffer.length() - 1) != '(')
                    {
                        buffer.append(",");
                    }
                    buffer.append("?");
                }
                buffer.append(")}");

                if (debugLog)
                {
                    log(buffer.toString(), paramTypes, paramValues, maxrows);
                }

                local = connection.prepareCall(buffer.toString());

                int paramcount = 0;

                entry = 0;
                if ((paramTypes.length > 0) && paramTypes[0].startsWith("R"))
                {
                    ++paramcount;

                    if (paramTypes[0].endsWith("C"))
                    {
                        if (oracleCursor == null)
                            throw new RuntimeException("Failed to load oracleCursor type");
                        local.registerOutParameter(paramcount, (Integer)oracleCursor);
                    }
                    else
                    {
                        local.registerOutParameter(paramcount, (Integer)paramValues[entry]);
                    }
                    entry++;
                }

                for (; entry < paramTypes.length; entry++)
                {
                    ++paramcount;

                    if (paramTypes[entry].indexOf("O") >= 0)
                    {
                        if (paramTypes[entry].endsWith("C"))
                        {
                            if (oracleCursor == null)
                                throw new RuntimeException("Failed to load oracleCursor type");
                            local.registerOutParameter(paramcount, (Integer)oracleCursor);
                        }
                        else
                        {
                            local.registerOutParameter(paramcount, (Integer)paramValues[entry]);
                        }
                    }
                    if (paramTypes[entry].indexOf("I") >= 0)
                    {
                        local.setObject(paramcount, paramValues[entry]);
                    }
                }

                boolean resultType = local.execute();
//logger.info("resultType=" + resultType);
                fetchResults(resultType, local, (retrievers == null ? null : retrievers[0]), (maxrows == null ? 0 : maxrows[0]));

                cursor = 0;
                paramcount = 0;
                for (entry = 0; entry < paramTypes.length; entry++)
                {
                    ++paramcount;

                    if ((paramTypes[entry].indexOf("O") >= 0) || paramTypes[entry].startsWith("R"))
                    {

                        if (paramTypes[entry].endsWith("C"))
                        {
                            ResultSet set = (ResultSet)local.getObject(paramcount);
                            fetchResults(set, retrievers[cursor], (maxrows == null ? 0 : maxrows[cursor]));
                            cursor++;
                        }
                        else
                        {
                            Object value = local.getObject(paramcount);
                            if (value != null)
                            {
                                if ((oracleTimestampClass != null) && value.getClass().isAssignableFrom(oracleTimestampClass))
                                {
                                    value = local.getTimestamp(paramcount);
                                }

                                if (! value.getClass().getName().startsWith("java."))
                                    throw new DatabaseException("Invalid output class: " + value.getClass().getName());
                            }

                            paramValues[entry] = value;
                        }
                    }
                }

                if (paramTypes[0].startsWith("R") && (paramTypes[0].indexOf("E") > 0))
                {
                    // sybase style exception handling from the return value

                    int resp = local.getInt(1);
                    if (resp != 0)
                    {
                        warnings(local);
                        local.close();
                        throw new DatabaseException("SP returns non-zero response " + resp, resp);
                    }
                }

                warnings(local);
                local.close();
            }
            catch (SQLException exception)
            {
                cancel(local);
                error = true;
                throw new DatabaseException(exception.getMessage(), exception);
            }
        }

        //---------------------------------------------------------------------------
        //
        //---------------------------------------------------------------------------
        private int fetchResults(boolean resultType, Statement statement, Retriever retriever, int maxrows) throws SQLException
        {
            int resp = -1;
            while (1 == 1)
            {
                if (resultType)
                {
                    ResultSet set = statement.getResultSet();
                    fetchResults(set, retriever, maxrows);
                }
                else
                {
                    int count = statement.getUpdateCount();

                    if (count < 0)
                    {
                        break;
                    }

                    resp = count;
                }

                resultType = statement.getMoreResults();
            }

            warnings(statement);
            return resp;
        }

        private void fetchResults(ResultSet set, Retriever retriever, int maxrows) throws SQLException
        {
            if (set == null)
            {
                statement.cancel();
                warnings(statement);
                throw new DatabaseException("SQL Unexpected missing resultSet");
            }

            if (retriever == null)
            {
                statement.cancel();
                warnings(statement);
                throw new DatabaseException("SQL Unexpected resultSet without Retreiver");
            }

            int rows = 0;
            while (set.next())
            {
                retriever.processRow(set);
                rows++;
                if ((maxrows > 0) && (rows >= maxrows))
                {
                    break;             
                }
            }

            // Report all warnings
            SQLWarning warning = set.getWarnings();
            while (warning != null)
            {
                logger.log("SQL Warning [" + warning.getErrorCode() + "]");
                warning = warning.getNextWarning();
            }

            set.close();
        }

        private void cancel(Statement statement)
        {
            try
            {
                if (statement != null)
                {
                    statement.cancel();
                    warnings(statement);
                }
            }
            catch (SQLException secondary)
            {
                // ignore
            }
        }

        //---------------------------------------------------------------------------
        //
        //---------------------------------------------------------------------------
        private void begin()
        {
            try
            {
                if (hasAutoCommit)
                {
                    connection.setAutoCommit(false);
                }
                else
                {
                    getSimpleStatement().executeUpdate("begin tran ");
                    warnings(statement);
                }
            }
            catch (SQLException exception)
            {
                try
                {
                    if (statement != null)
                    {
                        statement.cancel();
                    }
                }
                catch (SQLException secondary)
                {
                    // ignore
                }

                error = true;
                throw new DatabaseException(exception.getMessage(), exception);
            }
        }

        private void commit()
        {
            try
            {
                connection.commit();
                if (hasAutoCommit)
                {
                    connection.setAutoCommit(true);
                }
            }
            catch (SQLException exception)
            {
                error = true;
                throw new DatabaseException(exception.getMessage(), exception);
            }
        }

        private void rollback()
        {
            try
            {
                if (hasAutoCommit)
                {
                    if (connection.getAutoCommit() == false)
                    {
                        connection.rollback();
                        connection.setAutoCommit(true);
                    }
                }
                else
                {
                    connection.rollback();
                }
            }
            catch (SQLException exception)
            {
//                if ((exception.getMessage().indexOf("rollback() should not be called while in auto-commit mode") >= 0) ||
//                    (exception.getMessage().indexOf("Cannot invoke a rollback operation when the AutoCommit mode is set to") >= 0)) 
//                {
//                    // would really like to count begins - but some transaction unwind silently.
//                    return;
//                }

                error = true;
                throw new DatabaseException(exception.getMessage(), exception);
            }
        }

        private void close()
        {
            try
            {
                connection.close();
                connection = null;
            }
            catch (SQLException exception)
            {
                error = true;
                throw new DatabaseException(exception.getMessage(), exception);
            }
        }
    }
}
