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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
public class DatabaseStore implements Store {
    public static final String DEFAULT_BLOB_TYPE_NAME = "BLOB";
    public static final String PGSQL_LOB_TYPE_NAME = "BYTEA";
    
    private static final Log log = LogFactory.getLog(DatabaseStore.class);

    private final DataSource ds;
    private final String createTableDDL;
    private final String selectAllSQL;
    private final String selectMotableBodySQL;
    private final String insertMotableSQL;
    private final String updateMotableSQL;
    private final String deleteMotableSQL;
    private final String deleteAllSQL;
    private final boolean accessOnLoad;

    public DatabaseStore(DataSource dataSource,
        String table,
        boolean build,
        boolean accessOnLoad) {
        this(dataSource, table, DEFAULT_BLOB_TYPE_NAME, build, accessOnLoad);
    }
    
    public DatabaseStore(DataSource dataSource,
        String table,
        String lobTypeName,
        boolean build,
        boolean accessOnLoad) {
        this(dataSource,
            "CREATE TABLE " + table + " (name VARCHAR(50), creation_time BIGINT, last_accessed_time BIGINT, max_inactive_interval INTEGER, body " + lobTypeName + ")",
            "SELECT name, creation_time, last_accessed_time, max_inactive_interval FROM " + table,
            "SELECT body FROM " + table + " WHERE name = ?",
            "DELETE FROM " + table,
            "INSERT INTO " + table + " (name, creation_time, last_accessed_time, max_inactive_interval, body) VALUES (?, ?, ?, ?, ?)",
            "UPDATE " + table + " SET last_accessed_time = ?, max_inactive_interval = ?, body = ? WHERE name = ?",
            "DELETE FROM " + table + " WHERE name = ?",
            build,
            accessOnLoad);
    }
    
    public DatabaseStore(DataSource dataSource,
            String createTableDDL,
            String selectAllSQL,
            String selectMotableBodySQL,
            String deleteAllSQL,
            String insertMotableSQL,
            String updateMotableSQL,
            String deleteMotableSQL,
            boolean build,
            boolean accessOnLoad) {
        this.ds = dataSource;
        this.createTableDDL = createTableDDL;
        this.selectAllSQL = selectAllSQL;
        this.selectMotableBodySQL = selectMotableBodySQL;
        this.insertMotableSQL = insertMotableSQL;
        this.updateMotableSQL = updateMotableSQL;
        this.deleteMotableSQL = deleteMotableSQL;
        this.deleteAllSQL = deleteAllSQL;
        this.accessOnLoad = accessOnLoad;

        if (build) {
            init();
        }
    }

    public void clean() {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            clean(conn);
        } catch (SQLException e) {
            log.warn("See nested", e);
        } finally {
            closeConnection(conn);
        }
    }

    public void load(Putter putter) {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            load(putter, conn);
        } catch (SQLException e) {
            log.warn("See nested", e);
        } finally {
            closeConnection(conn);
        }
    }

    public byte[] loadBody(Motable motable) throws Exception {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        String name = motable.getName();
        byte[] body = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(selectMotableBodySQL);
            ps.setString(1, name);
            rs = ps.executeQuery();
            if (rs.next()) {
                body = loadBody(rs);
                if (log.isTraceEnabled()) {
                    log.trace("Body of Motable [" + name + "] has been loaded");
                }
                return body;
            } else {
                return null;
            }
        } catch (SQLException e) {
            log.warn("See nested", e);
            throw e;
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
            closeConnection(conn);
        }
    }

    public void insert(Motable motable) throws Exception {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(insertMotableSQL);
            int i = 1;
            String name = motable.getName();
            ps.setString(i++, name);
            ps.setLong(i++, motable.getCreationTime());
            ps.setLong(i++, motable.getLastAccessedTime());
            ps.setInt(i++, motable.getMaxInactiveInterval());
            byte[] body = motable.getBodyAsByteArray();
            ps.setBinaryStream(i++, new ByteArrayInputStream(body), body.length);
            ps.executeUpdate();
            if (log.isTraceEnabled()) {
                log.trace("Motable [" + name + "] has been inserted");
            }
        } catch (SQLException e) {
            log.warn("See nested", e);
            throw e;
        } finally {
            closeStatement(ps);
            closeConnection(conn);
        }
    }

    public void delete(Motable motable) {
        Connection conn = null;
        try {
            conn = getConnection();
            delete(motable, conn);
        } catch (SQLException e) {
            log.warn("See nested", e);
        } finally {
            closeConnection(conn);
        }
    }

    public Motable create() {
        return new BasicStoreMotable(this);
    }
    
    protected Connection getConnection() throws SQLException {
        return ds.getConnection();
    }
    
    protected void init() {
        Connection conn = null;
        Statement s = null;
        try {
            conn = ds.getConnection();
            s = conn.createStatement();
            s.execute(createTableDDL);
        } catch (SQLException e) {
            // ignore - table may already exist...
            log.warn("See nested", e);
        } finally {
            closeStatement(s);
            closeConnection(conn);
        }
    }

    protected byte[] loadBody(ResultSet rs) throws SQLException {
        InputStream in = rs.getBinaryStream("body");
        ByteArrayOutputStream memOut = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read;
        try {
            while (-1 < (read = in.read(buffer))) {
                memOut.write(buffer, 0, read);   
            }
        } catch (IOException e) {
            throw (SQLException) new SQLException().initCause(e);
        }
        return memOut.toByteArray();
    }
    
    protected void load(Putter putter, Connection conn) {
        long time = System.currentTimeMillis();
        int count = 0;
        Statement s = null;
        ResultSet rs = null;
        try {
            s = conn.createStatement();
            rs = s.executeQuery(selectAllSQL);
            while (rs.next()) {
                Motable motable = load(putter, time, rs, conn);
                if (null != motable) {
                    putter.put(motable.getName(), motable);
                    count++;
                }
            }
            log.info("[" + count + "] Motables loaded");
        } catch (SQLException e) {
            log.warn("See nested", e);
        } finally {
            closeResultSet(rs);
            closeStatement(s);
        }
    }

    protected Motable load(Putter putter, long time, ResultSet rs, Connection conn) {
        Motable motable = new BasicStoreMotable(this);
        String name = null;
        try {
            name = rs.getString("name");
            long creationTime = rs.getLong("creation_time");
            long lastAccessedTime = rs.getLong("last_accessed_time");
            lastAccessedTime = accessOnLoad ? time : lastAccessedTime;
            int maxInactiveInterval = rs.getInt("max_inactive_interval");
            motable.init(creationTime, lastAccessedTime, maxInactiveInterval, name);
            if (motable.getTimedOut(time)) {
                log.warn("Loaded dead Motable [" + motable.getName() + "]");
                delete(motable, conn);
                return null;
            }
        } catch (Exception e) {
            log.warn("See nested", e);
            return null;
        }
        return motable;
    }

    protected void clean(Connection conn) {
        Statement s = null;
        try {
            s = conn.createStatement();
            int nbDeleted = s.executeUpdate(deleteAllSQL);
            log.debug("Removed [" +nbDeleted + "] Motables");
        } catch (SQLException e) {
            log.warn("See nested", e);
        } finally {
            closeStatement(s);
        }
    }

    protected void delete(Motable motable, Connection conn) {
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(deleteMotableSQL);
            ps.setString(1, motable.getName());
            ps.executeUpdate();
            if (log.isTraceEnabled()) {
                log.trace("Motable [" +  motable + "] has been deleted");
            }
        } catch (SQLException e) {
            log.warn("See nested", e);
        } finally {
            closeStatement(ps);
        }
    }

    protected void closeResultSet(ResultSet rs) {
        try {
            if (null != rs) {
                rs.close();
            }
        } catch (SQLException e) {
            log.warn("See nested", e);
        }
    }
    
    protected void closeStatement(Statement s) {
        try {
            if (null != s) {
                s.close();
            }
        } catch (SQLException e) {
            log.warn("See nested", e);
        }
    }
    
    protected void closeConnection(Connection c) {
        try {
            if (null != c) {
                c.close();
            }
        } catch (SQLException e) {
            log.warn("See nested", e);
        }
    }

}
