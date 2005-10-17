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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Replicater;

public class DatabaseReplicater implements Replicater {

    protected final Log _log = LogFactory.getLog(getClass());

    public void create(Object tmp) {
		_log.info("create (database): "+((AbstractReplicableSession)tmp).getId());
		// write a row into the DB
    }
    
    public void replicate(Object tmp) { //TODO
		_log.info("replicate (database) : "+((AbstractReplicableSession)tmp).getId());
		// update a row in the DB
	}

    public void destroy(Object tmp) { //TODO
		_log.info("destroy (database) : "+((AbstractReplicableSession)tmp).getId());
		// remove a row in the DB
	}

}

