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
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.DatabaseMotableConfig;
import org.codehaus.wadi.Motable;
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
        _config.loadHeader(_connection, this);
    }

    // Control of what gets done on the same connection is controlled externally, by set/get-ting
    // the Connection through this API...
    protected Connection _connection;
    public Connection getConnection() {return _connection;}
    public void setConnection(Connection connection) {_connection=connection;}
    
    public byte[] getBodyAsByteArray() throws Exception {
        return (byte[])_config.loadBody(_connection, this);
    }
    
    public void setBodyAsByteArray(byte[] body) throws Exception {
        store(body);
    }
    
    // is there a way to use direct ByteBuffers for DB access? - there must be ....
    
    public ByteBuffer getBodyAsByteBuffer() throws Exception {
    	throw new UnsupportedOperationException("NYI");
    }
    
    public void setBodyAsByteBuffer(ByteBuffer body) throws Exception {
    	throw new UnsupportedOperationException("NYI");
    }
    
	public void copy(Motable motable) throws Exception {
    	try {
    		_connection=_config.getDataSource().getConnection();
    		super.copy(motable);
    	} finally {
    		try {
    			_connection.close();
    			_connection=null;
    		} catch (SQLException e) {
    			if (_log.isWarnEnabled()) _log.warn("load (database) problem releasing connection", e);
    		}
    	}
	}
    
    public void mote(Motable recipient) throws Exception {
    	try {
    		_connection=_config.getDataSource().getConnection();
    		recipient.copy(this);
    		destroy(recipient); // this is a transfer, so use special case destructor...
    	} finally {
    		try {
    			_connection.close();
    			_connection=null;
    		} catch (SQLException e) {
    			if (_log.isWarnEnabled()) _log.warn("load (database) problem releasing connection", e);
    		}
    	}
    }

    // we have two destroy usecases :
    
    // normal destruction - should remove corresponding row from db...
    public void destroy() throws Exception {
    	_config.delete(_connection, this);
        super.destroy();
    }
        
    // destruction as our data is transferred to another storage medium.
    // if this is the same medium as we are using for replication, we do not want to remove this copy, as it saves making a fresh replicant...
    public void destroy(Motable recipient) throws Exception {
    	super.destroy();

    	if (!_config.getReusingStore())
    		_config.delete(_connection, this);
    }

    protected void store(Object body) throws Exception {
    	_config.insert(_connection, this, body);
    }
    
}