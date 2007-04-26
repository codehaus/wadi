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

package org.codehaus.wadi.replication;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.core.motable.Motable;
import org.codehaus.wadi.core.store.DatabaseStore;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class DatabaseReplicater implements Replicater {
	private static final Log _log = LogFactory.getLog(DatabaseReplicater.class);
    
	protected final DatabaseStore store;

	public DatabaseReplicater(DatabaseStore store) {
        if (null == store) {
            throw new IllegalArgumentException("store is required");
        }
        this.store = store;
    }

    public void create(Object tmp) {
        Motable motable = (Motable) tmp;
        try {
            store.insert(motable);
        } catch (Exception e) {
            throw new ReplicaterException("problem creating replicant", e);
        }
    }

    public void update(Object tmp) {
        Motable motable = (Motable) tmp;
        try {
            store.update(motable);
        } catch (Exception e) {
            throw new ReplicaterException("problem updating replicant", e);
        }
    }

    public void destroy(Object tmp) {
        Motable motable = (Motable) tmp;
        try {
            store.delete(motable);
        } catch (Exception e) {
            _log.warn("problem destroying replicant", e);
        }
    }

    public void acquireFromOtherReplicater(Object tmp) {
    }
}

