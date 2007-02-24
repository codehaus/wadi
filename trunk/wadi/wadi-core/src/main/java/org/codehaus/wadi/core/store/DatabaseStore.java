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

package org.codehaus.wadi.core.store;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.core.motable.Motable;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class DatabaseStore implements Store, DatabaseMotableConfig {
  
  protected final Log _log=LogFactory.getLog(getClass());
  protected final String _label;
  protected final DataSource _dataSource;
  protected final String _table;
  protected final boolean _useNIO;
  protected final boolean _reusingStore;
  protected final boolean _build;
  protected final boolean _hasBigInt;
  protected final long _blobOffset;
  
  public DatabaseStore(String label, DataSource dataSource, String table, boolean useNIO, boolean reusingStore, boolean build) {
    _label=label;
    _dataSource=dataSource;
    _table=table;
    _useNIO=useNIO;
    _reusingStore=reusingStore;
    _build=build;
    boolean usingAxion=_dataSource.getClass().getName().startsWith("org.axiondb.");
    _hasBigInt=!usingAxion; // axion-1.0-M3-dev does not have the type 'BIGINT' use 'LONG' instead
    _blobOffset=usingAxion?0:1; // axion-1.0-M3-dev copies from blobs starting at 0 instead of 1
    
    if (_build) {
      try {
        init();
      } catch (SQLException e) {
        _log.warn("unexpected exception", e);
      }
    }
  }
  
  public String getLabel() {
    return _label;
  }
  
  public DataSource getDataSource() {
    return _dataSource;
  }
  
  public Connection getConnection() throws SQLException {
    return _dataSource.getConnection();
  }
  
  public String getTable() {
    return _table;
  }
  
  public boolean getReusingStore() {
    return _reusingStore;
  }
  
  // Store
  
  public void init() throws SQLException {
      Connection c=_dataSource.getConnection();
      String bigInt=_hasBigInt?"bigint":"long"; // Axion release does not yet support BIGINT type...
      Statement s=c.createStatement();
      try {
          // should now work for MySQL, Axion and Derby...
          String statement="CREATE TABLE "+_table+"(Name varchar(50), CreationTime "+bigInt+", LastAccessedTime "+bigInt+", MaxInactiveInterval int, Body blob)"; // TODO - parameterise Name width
          s.execute(statement);
      } catch (SQLException e) {
          // ignore - table may already exist...
          _log.warn(e);
      } finally {
          try {
              if (s!=null)
                  s.close();
          } catch (SQLException e) {
              _log.warn("problem closing database statement", e);
          }
          try {
              if (c!=null)
                  c.close();
          } catch (SQLException e) {
              _log.warn("problem closing database connection", e);
          }
      }
  }
  
  public void destroy() throws SQLException {
      Connection c=_dataSource.getConnection();
      Statement s=c.createStatement();
      try {
          s.execute("DROP TABLE "+_table);
      } catch (SQLException e) {
          // ignore - table may have already been deleted...
          _log.warn(e);
      } finally {
          //		s.execute("SHUTDOWN");
          try {
              if (s!=null)
                  s.close();
          } catch (SQLException e) {
              _log.warn("problem closing database statement", e);
          }
          try {
              if (c!=null)
                  c.close();
          } catch (SQLException e) {
              _log.warn("problem closing database connection", e);
          }
      }
  }
  
  public void clean() {
    Connection connection=null;
    Statement s=null;
    try {
      connection=_dataSource.getConnection();
      s=connection.createStatement();
      s.executeUpdate("DELETE FROM "+_table);
      if (_log.isTraceEnabled()) _log.trace("removed (database) sessions from last run"); // TODO - how many ?
    } catch (SQLException e) {
      if (_log.isErrorEnabled()) _log.error("remove (database) failed", e);
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
          try {
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
              if (_log.isInfoEnabled()) _log.info("loaded sessions: " + count);
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
          
          if (!_reusingStore) {
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
      } catch (SQLException e) {
          if (_log.isWarnEnabled()) _log.warn("load (shared database) problem", e);
      } finally {
          try {
              if (connection!=null)
                  connection.close();
          } catch (SQLException e) {
              _log.warn("problem closing database connection", e);
          }
      }
  }
  
  public void loadHeader(Connection connection, Motable motable) {
    String name=motable.getName();
    Statement s=null;
    try {
      s=connection.createStatement();
      ResultSet rs=s.executeQuery("SELECT CreationTime, LastAccessedTime, MaxInactiveInterval FROM "+_table+" WHERE Name='"+name+"'");
      int i=1;
      if (rs.next()) {
        long creationTime=rs.getLong(i++);
        long lastAccessedTime=rs.getLong(i++);
        int maxInactiveInterval=rs.getInt(i++);
        
        motable.init(creationTime, lastAccessedTime, maxInactiveInterval);
        
        if (_log.isTraceEnabled()) {
            _log.trace("loaded (database ["+_table+"]): "+name);
        }
      }
    } catch (SQLException e) {
      if (_log.isWarnEnabled()) _log.warn("load (database ["+_table+"]) failed: "+name, e);
    } finally {
      if (s!=null)
        try {
          s.close();
        } catch (SQLException e) {
          if (_log.isWarnEnabled()) _log.warn("load (database ["+_table+"]) problem: "+name, e);
        }
    }
  }
  
  public Object loadBody(Connection connection, Motable motable) throws Exception {
    String name=motable.getName();
    Statement s=null;
    Object body=null;
    try {
      s=connection.createStatement();
      ResultSet rs=s.executeQuery("SELECT Body FROM "+_table+" WHERE Name='"+name+"'");
      int i=1;
      if (rs.next()) {
        if (_useNIO) {
          // hmm...
          throw new UnsupportedOperationException("NYI");
        } else {
          Blob blob=rs.getBlob(i++);
          body=blob.getBytes(_blobOffset, (int)blob.length()); // TODO - axion starts copying from '0', not '1' - needs fixing...
        }
        if (_log.isTraceEnabled()) _log.trace("loaded (database): "+_label+"/"+_table+"/"+name+": "+((byte[])body).length+" bytes");
        return body;
      } else {
        return null;
      }
    } catch (SQLException e) {
      if (_log.isWarnEnabled()) _log.warn("load (database ["+_table+"]) failed: "+name, e);
      throw e;
    } finally {
      if (s!=null)
        try {
          s.close();
        } catch (SQLException e) {
          if (_log.isWarnEnabled()) _log.warn("load (database ["+_table+"]) problem: "+name, e);
        }
    }
  }
  
  public void update(Connection connection, Motable motable) throws Exception {
    PreparedStatement ps=null;
    String name=motable.getName();
    byte[] body=motable.getBodyAsByteArray();
    InputStream is=new ByteArrayInputStream(body);
    try {
      ps=connection.prepareStatement("UPDATE "+_table+" SET LastAccessedTime=?, MaxInactiveInterval=?, Body=? where Name=?");
      int i=1;
      ps.setLong(i++, motable.getLastAccessedTime());
      ps.setInt(i++, motable.getMaxInactiveInterval());
      if (_useNIO) {
        i++; // hmm...
        throw new UnsupportedOperationException("NYI");
      } else {
        ps.setBinaryStream(i++, is, body.length);
      }
      ps.setString(i++, name);
      ps.executeUpdate();
      if (_log.isTraceEnabled()) _log.trace("updated (database): "+_label+"/"+_table+"/"+name+": "+body.length+" bytes");
    } catch (SQLException e) {
      if (_log.isErrorEnabled()) _log.error("update (database) failed: "+name, e);
      throw e;
    } finally {
      if (ps!=null)
        ps.close();
    }
  }
  
  public void insert(Connection connection, Motable motable, Object body) throws Exception {
    PreparedStatement ps=null;
    String name=motable.getName();
    try {
      ps=connection.prepareStatement("INSERT INTO "+_table+" (Name, CreationTime, LastAccessedTime, MaxInactiveInterval, Body) VALUES (?, ?, ?, ?, ?)");
      int i=1;
      ps.setString(i++, name);
      ps.setLong(i++, motable.getCreationTime());
      ps.setLong(i++, motable.getLastAccessedTime());
      ps.setInt(i++, motable.getMaxInactiveInterval());
      if (_useNIO) {
        i++; // hmm...
        throw new UnsupportedOperationException("NYI");
      } else {
        ps.setObject(i++, body);
      }
      ps.executeUpdate();
      if (_log.isTraceEnabled()) _log.trace("stored (database): "+_label+"/"+_table+"/"+name+": "+((byte[])body).length+" bytes");
    } catch (SQLException e) {
      if (_log.isErrorEnabled()) _log.error("store (database) failed: "+name, e);
      throw e;
    } finally {
      if (ps!=null)
        ps.close();
    }
  }
  
  public void delete(Connection connection, Motable motable) {
    Statement s=null;
    try {
      s=connection.createStatement();
      s.executeUpdate("DELETE FROM "+_table+" WHERE Name='"+motable.getName()+"'");
      if (_log.isTraceEnabled()) _log.trace("removed (database ): "+_label+"/"+_table+"/"+motable);
    } catch (SQLException e) {
      if (_log.isErrorEnabled()) _log.error("remove (database ["+_table+"]) failed: "+motable, e);
    } finally {
      try {
        if (s!=null)
          s.close();
      } catch (SQLException e) {
        _log.warn("problem closing database connection", e);
      }
    }
  }
  
  public StoreMotable create() {
    return new DatabaseMotable();
  }
  
  // DatabaseMotableConfig
  
  public boolean getUseNIO() {
    return _useNIO;
  }
  
}
