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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Replicater;

public class DatabaseReplicater implements Replicater {
	
	protected final Log _log = LogFactory.getLog(getClass());
	protected final DatabaseStore _store;
	protected boolean _reusingStore;
	
	public DatabaseReplicater(DatabaseStore store, boolean reusingStore) {
		_store=store;
		_reusingStore=reusingStore;
	}
	
	public boolean getReusingStore() {
		return _reusingStore;
	}
	
	public void create(Object tmp) {
		AbstractReplicableSession session=(AbstractReplicableSession)tmp;
		String name=session.getId();
		_log.info("create (database): "+name);
		// write a row into the DB
		Connection connection=null;
		try {
			connection=_store.getConnection();
			_store.insert(connection, session, session.getBodyAsByteArray());
		} catch (Exception e) {
			_log.warn("problem creating replicant", e);
		} finally {
			if (connection!=null)
				try {
					connection.close();
				} catch (Exception e) {
					_log.warn("problem releasing connection", e);
				}
		}
	}
	
	public void update(Object tmp) { //TODO
		AbstractReplicableSession session=(AbstractReplicableSession)tmp;
		String name=session.getId();
		_log.info("update (database) : "+name);
		// update a row in the DB
		Connection connection=null;
		try {
			connection=_store.getConnection();
			_store.update(connection, session);
		} catch (Exception e) {
			_log.warn("problem updating replicant", e);
		} finally {
			if (connection!=null)
				try {
					connection.close();
				} catch (Exception e) {
					_log.warn("problem releasing connection", e);
				}
		}
	}
	
	public void destroy(Object tmp) { //TODO
		AbstractReplicableSession session=(AbstractReplicableSession)tmp;
		String name=session.getId();
		_log.info("destroy (database) : "+name);
		// remove a row in the DB
		Connection connection=null;
		try {
			connection=_store.getConnection();
			_store.delete(connection, session);
		} catch (Exception e) {
			_log.warn("problem destroying replicant", e);
		} finally {
			if (connection!=null)
				try {
					connection.close();
				} catch (Exception e) {
					_log.warn("problem releasing connection", e);
				}
		}
	}
	
}

