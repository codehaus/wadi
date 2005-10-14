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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.DatabaseMotableConfig;
import org.codehaus.wadi.Store;
import org.codehaus.wadi.StoreMotable;

public class DatabaseStore implements Store, DatabaseMotableConfig {

    protected final Log _log=LogFactory.getLog(getClass());
    protected final String _label;
    protected final DataSource _dataSource;
    protected final String _table;
    protected final boolean _useNIO;

    public DatabaseStore(String label, DataSource dataSource, String table, boolean useNIO) {
    	_label=label;
        _dataSource=dataSource;
        _table=table;
        _useNIO=useNIO;
        
        try {
        	DatabaseMotable.init(_dataSource, _table);
        } catch (SQLException e) {
        	_log.warn("unexpected exception", e);
        }
    }

    public String getLabel() {return _label;}
    public DataSource getDataSource() {return _dataSource;}
    public Connection getConnection() throws SQLException {return _dataSource.getConnection();}
    public String getTable() {return _table;}
    
    // Store
    
    public void clean() {
        Connection connection=null;
        Statement s=null;
        try {
            connection=_dataSource.getConnection();
            s=connection.createStatement();
            s.executeUpdate("DELETE FROM "+_table);
            if (_log.isTraceEnabled()) _log.trace("removed (shared database) sessions"); // TODO - how many ?
        } catch (SQLException e) {
            if (_log.isErrorEnabled()) _log.error("remove (shared database) failed", e);
        } finally {
            try {
                if (s!=null)
                    s.close();
            } catch (SQLException e) {
                _log.warn("problem closing database statement", e);
            }
            try {
                if (connection!=null)
                    connection.close();
            } catch (SQLException e) {
                _log.warn("problem closing database connection", e);
            }
        }
    }
    
    public void load(Putter putter, boolean accessOnLoad) {
        long time=System.currentTimeMillis();
        Statement s=null;
        int count=0;
        Connection connection=null;
        try {
            connection=_dataSource.getConnection();
            s=connection.createStatement();
            ResultSet rs=s.executeQuery("SELECT Name, CreationTime, LastAccessedTime, MaxInactiveInterval FROM "+_table);
            String name=null;
            while (rs.next()) {
                try {
                    int i=1;
                    DatabaseMotable motable=new DatabaseMotable();
                    name=(String)rs.getObject(i++);
                    long creationTime=rs.getLong(i++);
                    long lastAccessedTime=rs.getLong(i++);
                    lastAccessedTime=accessOnLoad?time:lastAccessedTime;
                    int maxInactiveInterval=rs.getInt(i++);
                    motable.init(creationTime, lastAccessedTime, maxInactiveInterval, name);
                    motable.init(this);
                    if (motable.getTimedOut(time)) {
                        if (_log.isWarnEnabled()) _log.warn("LOADED DEAD SESSION: "+motable.getName());
                        // we should expire it immediately, rather than promoting it...
                        // perhaps we could be even cleverer ?
                    }
                    putter.put(name, motable);
                    count++;
                } catch (Exception e) {
                   if (_log.isErrorEnabled()) _log.error("load (shared database) failed: "+name, e);
                }
            }
            _log.info("loaded sessions: "+count);
        } catch (SQLException e) {
            _log.warn("list (shared database) failed", e);
        } finally {
            if (s!=null)
                try {
                    s.close();
                } catch (SQLException e) {
                    if (_log.isWarnEnabled()) _log.warn("load (shared database) problem", e);
                }
        }

        try {
            s=connection.createStatement();
            s.executeUpdate("DELETE FROM "+_table);
        } catch (SQLException e) {
            _log.warn("removal (shared database) failed", e);
        } finally {
            if (s!=null)
                try {
                    s.close();
                } catch (SQLException e) {
                    if (_log.isWarnEnabled()) _log.warn("load (shared database) problem", e);
                }
        }
    }

    public String getStartInfo() {
        return _dataSource.toString();
    }

    public String getDescription() {
        return "database ["+_label+"/"+_table+"]";
    }

    public StoreMotable create() {
        return new DatabaseMotable();
    }
    
    // DatabaseMotableConfig
    
    public boolean getUseNIO() {return _useNIO;}
}