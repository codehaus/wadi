/**
 *
 * Copyright 2003-2005 Core Developers Network Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.codehaus.wadi.impl;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.DatabaseMotableConfig;
import org.codehaus.wadi.StoreMotable;
import org.codehaus.wadi.StoreMotableConfig;

/**
 * A Motable that represents its Bytes field as a row in a Shared DataBase table.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class DatabaseMotable extends AbstractMotable implements StoreMotable {
    
    protected static final Log _log = LogFactory.getLog(DatabaseMotable.class);
    
    protected DatabaseMotableConfig _config;
    
    public void init(StoreMotableConfig config) { // used when we are going to store something...
        _config=(DatabaseMotableConfig)config;
    }
    
    public void init(StoreMotableConfig config, String name) throws Exception { // used when we are going to load something...
        _config=(DatabaseMotableConfig)config;
        _name=name;
        loadHeader();
    }

    // Control of what gets done on the same connection is controlled externally, by set/get-ting
    // the Connection through this API...
    protected Connection _connection;
    public Connection getConnection() {return _connection;}
    public void setConnection(Connection connection) {_connection=connection;}
    
    public byte[] getBodyAsByteArray() throws Exception {
        return (byte[])loadBody(false);
    }
    
    public void setBodyAsByteArray(byte[] body) throws Exception {
        store(false, body);
    }
    
    // is there a way to use direct ByteBuffers for DB access? - there must be ....
    
    public ByteBuffer getBodyAsByteBuffer() throws Exception {
        //return (ByteBuffer)loadBody(true);
        return null;
    }
    
    public void setBodyAsByteBuffer(ByteBuffer body) throws Exception {
        //store(true, body);
    }
    
    public void destroy() {
    	destroy(_config, _connection, _name);
        super.destroy();
    }
    
    public static void destroy(DatabaseMotableConfig config, Connection connection, String name) {
        String table=config.getTable();
        Statement s=null;
        try {
            s=connection.createStatement();
            s.executeUpdate("DELETE FROM "+table+" WHERE Name='"+name+"'");
            if (_log.isTraceEnabled()) _log.trace("removed (database ): "+config.getLabel()+"/"+config.getTable()+"/"+name);
        } catch (SQLException e) {
            if (_log.isErrorEnabled()) _log.error("remove (database ["+table+"]) failed: "+name, e);
        } finally {
            try {
                if (s!=null)
                    s.close();
            } catch (SQLException e) {
                _log.warn("problem closing database connection", e);
            }
        }
    }
    
    protected void loadHeader() {
        Statement s=null;
        String table=_config.getTable();
        try {
            s=_connection.createStatement();
            ResultSet rs=s.executeQuery("SELECT CreationTime, LastAccessedTime, MaxInactiveInterval FROM "+table+" WHERE Name='"+_name+"'");
            int i=1;
            if (rs.next()) {
                _creationTime=rs.getLong(i++);
                _lastAccessedTime=rs.getLong(i++);
                _maxInactiveInterval=rs.getInt(i++);
                
                if (!checkTimeframe(System.currentTimeMillis()))
                    if (_log.isWarnEnabled()) _log.warn("loaded (database ["+table+"]) from the future!: "+_name);
                
                if (_log.isTraceEnabled()) _log.trace("loaded (database ["+table+"]): "+_name);
            }
        } catch (SQLException e) {
            if (_log.isWarnEnabled()) _log.warn("load (database ["+table+"]) failed: "+_name, e);
        } finally {
            if (s!=null)
                try {
                    s.close();
                } catch (SQLException e) {
                    if (_log.isWarnEnabled()) _log.warn("load (database ["+table+"]) problem: "+_name, e);
                }
        }
    }
    
    protected Object loadBody(boolean useNIO) throws Exception {
        String table=_config.getTable();
        Statement s=null;
        Object body=null;
        try {
            s=_connection.createStatement();
            ResultSet rs=s.executeQuery("SELECT Body FROM "+table+" WHERE Name='"+_name+"'");
            int i=1;
            if (rs.next()) {
                if (useNIO) {
                    // hmm...
                	throw new UnsupportedOperationException("NYI");
                } else {
                    body=rs.getObject(i++);
                }
                if (_log.isTraceEnabled()) _log.trace("loaded (database): "+_config.getLabel()+"/"+_config.getTable()+"/"+_name+": "+((byte[])body).length+" bytes");
                return body;
            } else {
                return null;
            }
        } catch (SQLException e) {
            if (_log.isWarnEnabled()) _log.warn("load (database ["+table+"]) failed: "+_name, e);
            throw e;
        } finally {
            if (s!=null)
                try {
                    s.close();
                } catch (SQLException e) {
                    if (_log.isWarnEnabled()) _log.warn("load (database ["+table+"]) problem: "+_name, e);
                }
        }
    }
    
    // move back into store ?
    public static void update(DatabaseMotableConfig config, Connection connection, boolean useNIO, String name, long lastAccessedTime, int maxInactiveInterval, byte[] body) throws Exception {
        PreparedStatement ps=null;
        try {
            ps=connection.prepareStatement("UPDATE "+config.getTable()+" SET LastAccessedTime=?, MaxInactiveInterval=?, Body=? where Name=?");
            int i=1;
            ps.setLong(i++, lastAccessedTime);
            ps.setInt(i++, maxInactiveInterval);
            if (useNIO) {
                i++; // hmm...
            	throw new UnsupportedOperationException("NYI");
            } else {
                ps.setObject(i++, body);
            }
            ps.setString(i++, name);
            ps.executeUpdate();
            if (_log.isTraceEnabled()) _log.trace("updated (database): "+config.getLabel()+"/"+config.getTable()+"/"+name+": "+body.length+" bytes");
        } catch (SQLException e) {
            if (_log.isErrorEnabled()) _log.error("update (database) failed: "+name, e);
            throw e;
        } finally {
            if (ps!=null)
                ps.close();
        }
    }

    protected void store(boolean useNIO, Object body) throws Exception {
    	store(_config, _connection, useNIO, _name, _creationTime, _lastAccessedTime, _maxInactiveInterval, body);
    }
    
    // move back into store ?
    public static void store(DatabaseMotableConfig config, Connection connection, boolean useNIO, String name, long creationTime, long lastAccessedTime, int maxInactiveInterval, Object body) throws Exception {
        PreparedStatement ps=null;
        try {
            ps=connection.prepareStatement("INSERT INTO "+config.getTable()+" (Name, CreationTime, LastAccessedTime, MaxInactiveInterval, Body) VALUES (?, ?, ?, ?, ?)");
            int i=1;
            ps.setString(i++, name);
            ps.setLong(i++, creationTime);
            ps.setLong(i++, lastAccessedTime);
            ps.setInt(i++, maxInactiveInterval);
            if (useNIO) {
                i++; // hmm...
            	throw new UnsupportedOperationException("NYI");
            } else {
                ps.setObject(i++, body);
            }
            ps.executeUpdate();
            if (_log.isTraceEnabled()) _log.trace("stored (database): "+config.getLabel()+"/"+config.getTable()+"/"+name+": "+((byte[])body).length+" bytes");
        } catch (SQLException e) {
            if (_log.isErrorEnabled()) _log.error("store (database) failed: "+name, e);
            throw e;
        } finally {
            if (ps!=null)
                ps.close();
        }
    }
    
    public static void init(DataSource dataSource, String table) throws SQLException {
        Connection c=dataSource.getConnection();
        Statement s=c.createStatement();
        try {
            s.execute("CREATE TABLE "+table+"(Name varchar(50), CreationTime long, LastAccessedTime long, MaxInactiveInterval int, Body varbinary("+700*1024+"))");
        } catch (SQLException e) {
            // ignore - table may already exist...
        	_log.warn(e);
        }
        s.close();
        c.close();
    }
    
    public static void destroy(DataSource dataSource, String table) throws SQLException {
        Connection c=dataSource.getConnection();
        Statement s=c.createStatement();
        try {
            s.execute("DROP TABLE "+table);
        } catch (SQLException e) {
            // ignore - table may have already been deleted...
        	_log.warn(e);
        }
//      s.execute("SHUTDOWN");
        s.close();
        c.close();
    }
    
}
