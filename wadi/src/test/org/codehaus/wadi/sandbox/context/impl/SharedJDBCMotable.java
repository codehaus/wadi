/**
*
* Copyright 2003-2004 The Apache Software Foundation
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
package org.codehaus.wadi.sandbox.context.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A Motable that represents its Bytes field as a row ina DataBase table.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class SharedJDBCMotable extends AbstractMotable {
	protected static final Log _log = LogFactory.getLog(SharedJDBCMotable.class);
	
	protected String _id;
	public String getId(){return _id;}
	public void setId(String id){_id=id;}
	
	protected String _table;
	public String getTable() {return _table;}
	public void setTable(String table) {_table=table;}
	
	protected Connection _connection;
	public Connection getConnection(){return _connection;}
	public void setConnection(Connection connection){_connection=connection;}
	
	public void tidy() {remove(_connection, _table, _id);}
	
	// Motable
	public byte[] getBytes() throws SQLException {return load(_connection, _table, _id);}
	public void setBytes(byte[] bytes) throws SQLException {store(_connection, _table, _id, bytes);}
	
	protected static byte[] load(Connection connection, String table, String id) throws SQLException {
		Statement s=null;
		try {
			s=connection.createStatement();
			ResultSet rs=s.executeQuery("SELECT Bytes FROM "+table+" WHERE Id='"+id+"'");
			if (rs.next()) {
				byte[] buffer=(byte[])rs.getObject(1);
				_log.info("loaded (database): "+id);
				return buffer;
			} else {
				return null;
			}
		} catch (SQLException e) {
			_log.warn("load (database) failed: "+id, e);
			throw e;
		} finally {
			if (s!=null)
				s.close();
		}
	}
	
	protected static void store(Connection connection, String table, String id, byte[] bytes) throws SQLException {
		PreparedStatement ps=null;
		try {
			ps=connection.prepareStatement("INSERT INTO "+table+" (Id, Bytes) VALUES ('"+id+"', ?)");
			ps.setObject(1, bytes);
			ps.executeUpdate();
			_log.info("stored (database): "+id);
		} catch (SQLException e) {
			_log.error("store (database) failed: "+id, e);
			throw e;
		} finally {
			if (ps!=null)
				ps.close();
		}
	}
	
	protected static void remove(Connection connection, String table, String id) {
		Statement s=null;
		try {
			s=connection.createStatement();
			s.executeUpdate("DELETE FROM "+table+" WHERE Id='"+id+"'");
			_log.info("removed (database): "+id);
		} catch (SQLException e) {
			_log.error("remove (database) failed: "+id);
		} finally {
			try {
			if (s!=null)
				s.close();
			} catch (SQLException e) {}
		}
	}
	
	public static void initialise(DataSource dataSource, String table) throws SQLException {
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