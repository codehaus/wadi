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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Emoter;
import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.Motable;

/**
 * A Motable that represents its Bytes field as a row in a Shared DataBase table.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class SharedJDBCMotable extends AbstractMotable {
	protected static final Log _log = LogFactory.getLog(SharedJDBCMotable.class);

	protected byte[] _bytes;
	public byte[] getBytes(){return _bytes;}
	public void setBytes(byte[] bytes){_bytes=bytes;}

	protected String _table;
	public String getTable() {return _table;}
	public void setTable(String table) {_table=table;}

	protected Connection _connection;
	public Connection getConnection(){return _connection;}
	public void setConnection(Connection connection){_connection=connection;}

	public void destroy() {
	    super.destroy();
	    remove(_connection, _table, this);
	}

	public void copy(Motable motable) throws Exception {
		super.copy(motable);
		store(_connection, _table, this);
	}

    protected static int load(Connection connection, String table, Emoter emoter, Immoter immoter, boolean accessOnLoad) throws Exception {
        long time=System.currentTimeMillis();
        Statement s=null;
        int count=0;
        try {
            s=connection.createStatement();
            ResultSet rs=s.executeQuery("SELECT Id, CreationTime, LastAccessedTime, MaxInactiveInterval, Bytes FROM "+table);
            while (rs.next()) {
                int i=1;
                Motable motable=new SharedJDBCMotable();
                String id=(String)rs.getObject(i++);
                long creationTime=rs.getLong(i++);
                long lastAccessedTime=rs.getLong(i++);
                lastAccessedTime=accessOnLoad?time:lastAccessedTime;
                int maxInactiveInterval=rs.getInt(i++);
                motable.init(creationTime, lastAccessedTime, maxInactiveInterval, id);
                motable.setBytes((byte[])rs.getObject(i++));

                if (motable.getTimedOut(time)) {
                    if (_log.isWarnEnabled()) _log.warn("LOADED DEAD SESSION: "+motable.getName());
                    // we should expire it immediately, rather than promoting it...
                    // perhaps we could be even cleverer ?
                }

                Utils.mote(emoter, immoter, motable, id);
                count++;
            }
            _log.info("loaded sessions: "+count);
        } catch (SQLException e) {
            _log.warn("list (shared database) failed", e);
            throw e;
        } finally {
            if (s!=null)
                s.close();
        }

        try {
            s=connection.createStatement();
            s.executeUpdate("DELETE FROM "+table);
        } catch (SQLException e) {
            _log.warn("removal (shared database) failed", e);
            throw e;
        } finally {
            if (s!=null)
                s.close();
        }
        
        return count;
    }

    protected static Motable load(Connection connection, String table, Motable motable) throws Exception {
		String id=motable.getName();
		Statement s=null;
		try {
			s=connection.createStatement();
			ResultSet rs=s.executeQuery("SELECT CreationTime, LastAccessedTime, MaxInactiveInterval, Bytes FROM "+table+" WHERE Id='"+id+"'");
			int i=1;
			if (rs.next()) {
				long creationTime=rs.getLong(i++);
				long lastAccessedTime=rs.getLong(i++);
				int maxInactiveInterval=rs.getInt(i++);
                motable.init(creationTime, lastAccessedTime, maxInactiveInterval, id);
				motable.setBytes((byte[])rs.getObject(i++));

				if (!motable.checkTimeframe(System.currentTimeMillis()))
				    if (_log.isWarnEnabled()) _log.warn("loaded session from the future!: "+id);

				if (_log.isTraceEnabled()) _log.trace("loaded (shared database): "+id);
				return motable;
			} else {
				return null;
			}
		} catch (SQLException e) {
			if (_log.isWarnEnabled()) _log.warn("load (shared database) failed: "+id, e);
			throw e;
		} finally {
			if (s!=null)
				s.close();
		}
	}

	protected static void store(Connection connection, String table, Motable motable) throws Exception {
		String id=motable.getName();
		PreparedStatement ps=null;
		try {
			ps=connection.prepareStatement("INSERT INTO "+table+" (Id, CreationTime, LastAccessedTime, MaxInactiveInterval, Bytes) VALUES (?, ?, ?, ?, ?)");
			int i=1;
			ps.setString(i++, id);
			ps.setLong(i++, motable.getCreationTime());
			ps.setLong(i++, motable.getLastAccessedTime());
			ps.setInt(i++, motable.getMaxInactiveInterval());
			ps.setObject(i++, motable.getBytes());
			ps.executeUpdate();
			if (_log.isTraceEnabled()) _log.trace("stored (shared database): "+id);
		} catch (SQLException e) {
			if (_log.isErrorEnabled()) _log.error("store (shared database) failed: "+id, e);
			throw e;
		} finally {
			if (ps!=null)
				ps.close();
		}
	}

	protected static void remove(Connection connection, String table, Motable motable) {
	    String id=motable.getName();
	    Statement s=null;
	    try {
	        s=connection.createStatement();
	        s.executeUpdate("DELETE FROM "+table+" WHERE Id='"+id+"'");
	        if (_log.isTraceEnabled()) _log.trace("removed (shared database): "+id);
	    } catch (SQLException e) {
	        if (_log.isErrorEnabled()) _log.error("remove (shared database) failed: "+id);
	    } finally {
	        try {
	            if (s!=null)
	                s.close();
	        } catch (SQLException e) {
	            _log.warn("problem closing database connection", e);
	        }
	    }
	}

	public static void init(DataSource dataSource, String table) throws SQLException {
		Connection c=dataSource.getConnection();
		Statement s=c.createStatement();
        try {
            s.execute("CREATE TABLE "+table+"(Id varchar, CreationTime long, LastAccessedTime long, MaxInactiveInterval int, Bytes java_object)");
        } catch (SQLException e) {
            // ignore - table may already exist...
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
        }
//		s.execute("SHUTDOWN");
		s.close();
		c.close();
	}
}
