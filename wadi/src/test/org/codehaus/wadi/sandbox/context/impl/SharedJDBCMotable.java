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
 * A Motable that represents its Bytes field as a File on LocalDisc.
 * N.B. The File field must be set before the Bytes field.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class SharedJDBCMotable extends AbstractMotable {
	protected static final Log _log = LogFactory.getLog(SharedJDBCMotable.class);
	
	protected String _id;
	public String getId(){return _id;}
	public void setId(String id){_id=id;}
	
	protected DataSource _dataSource;
	public DataSource getDataSource() {return _dataSource;}
	public void setDataSource(DataSource dataSource) {_dataSource=dataSource;}
	
	protected String _table;
	public String getTable() {return _table;}
	public void setTable(String table) {_table=table;}
	
	public void tidy() {
		remove(_dataSource, _table, _id);
	}
	
	// Motable
	public byte[] getBytes() throws SQLException {return load(_dataSource, _table, _id);}
	public void setBytes(byte[] bytes) throws SQLException {store(_dataSource, _table, _id, bytes);}
	
	protected static byte[] load(DataSource ds, String table, String id) throws SQLException {
		Connection c=null;
		Statement s=null;
		try {
			c=ds.getConnection();
			s=c.createStatement();
			ResultSet rs=s.executeQuery("SELECT MyValue FROM "+table+" WHERE MyKey='"+id+"'");
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
			if (c!=null)
				c.close();				
		}
	}
	
	protected static void store(DataSource ds, String table, String id, byte[] bytes) throws SQLException {
		Connection c=null;
		PreparedStatement ps=null;
		try {
			c=ds.getConnection();
			ps=c.prepareStatement("INSERT INTO "+table+" (MyKey, MyValue) VALUES ('"+id+"', ?)");
			ps.setObject(1, bytes);
			ps.executeUpdate();
			_log.info("stored (database): "+id);
		} catch (SQLException e) {
			_log.error("store (database) failed: "+id, e);
			throw e;
		} finally {
			if (ps!=null)
				ps.close();
			if (c!=null)
				c.close();				
		}
	}
	
	protected static void remove(DataSource ds, String table, String id) {
		Connection c=null;
		Statement s=null;
		try {
			c=ds.getConnection();
			s=c.createStatement();
			s.executeUpdate("DELETE FROM "+table+" WHERE MyKey='"+id+"'");
			_log.info("removed (database): "+id);
		} catch (SQLException e) {
			_log.error("remove (database) failed: "+id);
		} finally {
			try {
			if (s!=null)
				s.close();
			} catch (SQLException e) {}
			try {
			if (c!=null)
				c.close();				
			} catch (SQLException e) {}
		}
	}
}