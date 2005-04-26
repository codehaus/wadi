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
package org.codehaus.wadi.sandbox.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.sandbox.Emoter;
import org.codehaus.wadi.sandbox.Immoter;
import org.codehaus.wadi.sandbox.Motable;

/**
 * A Motable that represents its Bytes field as a row in a Shared DataBase table.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class SharedJDBCMotable extends AbstractMotable {
	protected static final Log _log = LogFactory.getLog(SharedJDBCMotable.class);

	protected String _id;
	public String getId(){return _id;}
	public void setId(String id){_id=id;}

	protected byte[] _bytes;
	public byte[] getBytes(){return _bytes;}
	public void setBytes(byte[] bytes){_bytes=bytes;}

	protected String _table;
	public String getTable() {return _table;}
	public void setTable(String table) {_table=table;}

	protected Connection _connection;
	public Connection getConnection(){return _connection;}
	public void setConnection(Connection connection){_connection=connection;}

	public void tidy() {
	    super.tidy();
	    remove(_connection, _table, this);
	}

	public void copy(Motable motable) throws Exception {
		super.copy(motable);
		store(_connection, _table, this);
	}

    protected static int list(Connection connection, String table, Emoter emoter, Immoter immoter) throws Exception {
        Statement s=null;
        int count=0;
        try {
            s=connection.createStatement();
            ResultSet rs=s.executeQuery("SELECT Id, CreationTime, LastAccessedTime, MaxInactiveInterval, Bytes FROM "+table);
            while (rs.next()) {
                int i=1;
                Motable motable=new SharedJDBCMotable();
                String id=(String)rs.getObject(i++);
                motable.setId(id);
                motable.setCreationTime(rs.getLong(i++));
                motable.setLastAccessedTime(rs.getLong(i++));
                motable.setMaxInactiveInterval(rs.getInt(i++));
                motable.setBytes((byte[])rs.getObject(i++));
                Utils.mote(emoter, immoter, motable, id);
                count++;
            }
        } catch (SQLException e) {
            _log.warn("list (shared database) failed", e);
            throw e;
        } finally {
            if (s!=null)
                s.close();
        }
        return count;
    }
    
    protected static Motable load(Connection connection, String table, Motable motable) throws Exception {
		String id=motable.getId();
		Statement s=null;
		try {
			s=connection.createStatement();
			ResultSet rs=s.executeQuery("SELECT CreationTime, LastAccessedTime, MaxInactiveInterval, Bytes FROM "+table+" WHERE Id='"+id+"'");
			int i=1;
			if (rs.next()) {
				motable.setCreationTime(rs.getLong(i++));
				motable.setLastAccessedTime(rs.getLong(i++));
				motable.setMaxInactiveInterval(rs.getInt(i++));
				motable.setBytes((byte[])rs.getObject(i++));

				if (!motable.checkTimeframe(System.currentTimeMillis()))
				    _log.warn("loaded session from the future!: "+id);
				
				_log.info("loaded (shared database): "+id);
				return motable;
			} else {
				return null;
			}
		} catch (SQLException e) {
			_log.warn("load (shared database) failed: "+id, e);
			throw e;
		} finally {
			if (s!=null)
				s.close();
		}
	}

	protected static void store(Connection connection, String table, Motable motable) throws Exception {
		String id=motable.getId();
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
			_log.info("stored (shared database): "+id);
		} catch (SQLException e) {
			_log.error("store (shared database) failed: "+id, e);
			throw e;
		} finally {
			if (ps!=null)
				ps.close();
		}
	}

	protected static void remove(Connection connection, String table, Motable motable) {
	    String id=motable.getId();
	    Statement s=null;
	    try {
	        s=connection.createStatement();
	        s.executeUpdate("DELETE FROM "+table+" WHERE Id='"+id+"'");
	        _log.info("removed (shared database): "+id);
	    } catch (SQLException e) {
	        _log.error("remove (shared database) failed: "+id);
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
		s.execute("CREATE TABLE "+table+"(Id varchar, CreationTime long, LastAccessedTime long, MaxInactiveInterval int, Bytes java_object)");
		s.close();
		c.close();
	}

	public static void destroy(DataSource dataSource, String table) throws SQLException {
		Connection c=dataSource.getConnection();
		Statement s=c.createStatement();
		s.execute("DROP TABLE "+table);
//		s.execute("SHUTDOWN");
		s.close();
		c.close();
	}
}
