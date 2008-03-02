/**
 * Copyright 2006 The Apache Software Foundation
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
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.sql.DataSource;

import org.codehaus.wadi.core.motable.Motable;
import org.codehaus.wadi.core.store.Store.Putter;

import com.agical.rmock.core.describe.ExpressionDescriber;
import com.agical.rmock.core.match.operator.AbstractExpression;
import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class DatabaseStoreTest extends RMockTestCase {

    private DataSource dataSource;

    protected void setUp() throws Exception {
        dataSource = (DataSource) mock(DataSource.class);
    }
    
    public void testInit() throws Exception {
        beginSection(s.ordered("Create table"));
        Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        statement.execute("CREATE TABLE TABLE_NAME (name VARCHAR(50), creation_time BIGINT, last_accessed_time BIGINT, max_inactive_interval INTEGER, body BLOB)");
        statement.close();
        connection.close();
        endSection();
        
        startVerification();
        
        new DatabaseStore(dataSource, "TABLE_NAME", true, true);
    }
    
    public void testClean() throws Exception {
        beginSection(s.ordered("Delete table"));
        Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        statement.executeUpdate("DELETE FROM TABLE_NAME");
        statement.close();
        connection.close();
        endSection();
        startVerification();
        
        DatabaseStore store = new DatabaseStore(dataSource, "TABLE_NAME", false, true);
        store.clean();
    }
    
    public void testLoad() throws Exception {
        Putter putter = (Putter) mock(Putter.class);
        
        beginSection(s.ordered("Load"));
        Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("SELECT name, creation_time, last_accessed_time, max_inactive_interval FROM TABLE_NAME");

        rs.next();
        modify().returnValue(true);
        String name = "name";
        rs.getString(name);
        modify().returnValue(name);
        rs.getLong("creation_time");
        modify().returnValue(1);
        rs.getLong("last_accessed_time");
        final long lastAccessedTime = System.currentTimeMillis();
        modify().returnValue(lastAccessedTime);
        rs.getInt("max_inactive_interval");
        modify().returnValue(30000);

        putter.put(name, null);
        modify().args(is.AS_RECORDED, new AbstractExpression() {

            public void describeWith(ExpressionDescriber arg0) throws IOException {
            }

            public boolean passes(Object arg0) {
                Motable motable = (Motable) arg0;
                assertEquals(1, motable.getCreationTime());
                assertEquals(lastAccessedTime, motable.getLastAccessedTime());
                assertEquals(30000, motable.getMaxInactiveInterval());
                return true;
            }
            
        });
        
        rs.next();
        modify().returnValue(false);
        rs.close();
        statement.close();
        connection.close();
        endSection();
        
        startVerification();
        
        DatabaseStore store = new DatabaseStore(dataSource, "TABLE_NAME", false, false);
        store.load(putter);
    }

    public void testLoadTimedOutMotable() throws Exception {
        Putter putter = (Putter) mock(Putter.class);
        
        beginSection(s.ordered("Load"));
        Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("SELECT name, creation_time, last_accessed_time, max_inactive_interval FROM TABLE_NAME");

        rs.next();
        modify().returnValue(true);
        String name = "name";
        rs.getString(name);
        modify().returnValue(name);
        rs.getLong("creation_time");
        modify().returnValue(1);
        rs.getLong("last_accessed_time");
        modify().returnValue(3);
        rs.getInt("max_inactive_interval");
        modify().returnValue(30000);

        PreparedStatement ps = connection.prepareStatement("DELETE FROM TABLE_NAME WHERE name = ?");
        ps.setString(1, name);
        ps.executeUpdate();
        ps.close();
        
        rs.next();
        modify().returnValue(false);
        rs.close();
        statement.close();
        connection.close();
        endSection();
        
        startVerification();
        
        DatabaseStore store = new DatabaseStore(dataSource, "TABLE_NAME", false, false);
        store.load(putter);
    }

    public void testLoadBody() throws Exception {
        DatabaseStore store = new DatabaseStore(dataSource, "TABLE_NAME", false, false);
        Motable motable = (Motable) mock(Motable.class);
        
        beginSection(s.ordered("Load body"));
        motable.getName();
        String name = "name";
        modify().returnValue(name);

        Connection connection = dataSource.getConnection();
        PreparedStatement ps = connection.prepareStatement("SELECT body FROM TABLE_NAME WHERE name = ?");
        ps.setString(1, name);
        ResultSet rs = ps.executeQuery();
        rs.next();
        modify().returnValue(true);
        rs.getBinaryStream("body");
        modify().returnValue(new ByteArrayInputStream(new byte[0]));

        rs.close();
        ps.close();
        connection.close();
        
        endSection();
        startVerification();
        
        byte[] actualBody = store.loadBody(motable);
        assertEquals(0, actualBody.length);
    }
    
    public void testInsert() throws Exception {
        Motable motable = (Motable) mock(Motable.class);

        beginSection(s.ordered("Update"));
        Connection connection = dataSource.getConnection();
        PreparedStatement ps = connection.prepareStatement("INSERT INTO TABLE_NAME (name, creation_time, last_accessed_time, max_inactive_interval, body) VALUES (?, ?, ?, ?, ?)");
        
        motable.getName();
        modify().returnValue("name");
        ps.setString(1, "name");
        
        motable.getCreationTime();
        modify().returnValue(1);
        ps.setLong(2, 1);

        motable.getLastAccessedTime();
        modify().returnValue(2);
        ps.setLong(3, 2);
        
        motable.getMaxInactiveInterval();
        modify().returnValue(3);
        ps.setInt(4, 3);
        
        motable.getBodyAsByteArray();
        modify().returnValue(new byte[] {'1'});
        ps.setBinaryStream(5, null, 1);
        modify().args(is.AS_RECORDED, is.ANYTHING, is.AS_RECORDED);
        
        ps.executeUpdate();
        modify().returnValue(1);

        ps.close();
        connection.close();
        endSection();
        startVerification();
        
        DatabaseStore store = new DatabaseStore(dataSource, "TABLE_NAME", false, false);
        store.insert(motable);
    }
    
    public void testDelete() throws Exception {
        Motable motable = (Motable) mock(Motable.class);

        beginSection(s.ordered("Update"));
        Connection connection = dataSource.getConnection();
        PreparedStatement ps = connection.prepareStatement("DELETE FROM TABLE_NAME WHERE name = ?");
        
        motable.getName();
        modify().returnValue("name");
        ps.setString(1, "name");

        ps.executeUpdate();
        
        ps.close();
        connection.close();
        endSection();
        startVerification();
        
        DatabaseStore store = new DatabaseStore(dataSource, "TABLE_NAME", false, false);
        store.delete(motable);
    }
    
}
