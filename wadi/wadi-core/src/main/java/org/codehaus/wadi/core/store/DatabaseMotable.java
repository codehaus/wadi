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

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Motable;
import org.codehaus.wadi.core.motable.AbstractMotable;

/**
 * A Motable that represents its Bytes field as a row in a Shared DataBase table.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class DatabaseMotable extends AbstractMotable implements StoreMotable {
    private static final Log _log = LogFactory.getLog(DatabaseMotable.class);
    
    protected DatabaseMotableConfig _config;
    protected Connection _connection;
    
    public void init(StoreMotableConfig config) {
        _config = (DatabaseMotableConfig) config;
    }

    public void init(StoreMotableConfig config, String name) throws Exception {
        _config = (DatabaseMotableConfig) config;
        this.name = name;
        _config.loadHeader(_connection, this);
    }

    public Connection getConnection() {
        return _connection;
    }

    public void setConnection(Connection connection) {
        _connection = connection;
    }

    public byte[] getBodyAsByteArray() throws Exception {
        return (byte[]) _config.loadBody(_connection, this);
    }

    public void setBodyAsByteArray(byte[] body) throws Exception {
        store(body);
    }

    public void copy(Motable motable) throws Exception {
        try {
            _connection = _config.getDataSource().getConnection();
            super.copy(motable);
        } finally {
            try {
                _connection.close();
                _connection = null;
            } catch (SQLException e) {
                _log.warn("load (database) problem releasing connection", e);
            }
        }
    }

    public void mote(Motable recipient) throws Exception {
        try {
            _connection = _config.getDataSource().getConnection();
            recipient.copy(this);
            destroyForMotion();
        } finally {
            try {
                _connection.close();
                _connection = null;
            } catch (SQLException e) {
                if (_log.isWarnEnabled())
                    _log.warn("load (database) problem releasing connection", e);
            }
        }
    }

    public void destroy() throws Exception {
        _config.delete(_connection, this);
        super.destroy();
    }

    // destruction as our data is transferred to another storage medium.
    // if this is the same medium as we are using for replication, we do not
    // want to remove this copy, as it saves making a fresh replicant...
    public void destroyForMotion() throws Exception {
        super.destroyForMotion();

        if (!_config.getReusingStore()) {
            _config.delete(_connection, this);
        }
    }

    protected void store(Object body) throws Exception {
        _config.insert(_connection, this, body);
    }

}
