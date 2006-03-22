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

import java.util.Map;

import org.activecluster.Node;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Replicater;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class MemoryReplicater implements Replicater {

	protected final Log _log = LogFactory.getLog(getClass());

	protected int numReplicants;

	public MemoryReplicater(int numReplicants) {
	}

	// client part

	public void create(Object tmp) {
		if (_log.isTraceEnabled()) _log.trace("create: " + tmp);
		// decide on replication partners (not ourselves)
		// sned messages (sync) to insert replicants on these partners
	}

	public void update(Object tmp) {
		if (_log.isTraceEnabled()) _log.trace("update: " + tmp);
		// send messages (sync) to replicate to our partners
	}

	public void destroy(Object tmp) {
		if (_log.isTraceEnabled()) _log.trace("destroy: " + tmp);
		// send messages (sync) to remove replicants on partners
	}

    public void acquireFromOtherReplicater(Object tmp) {
        
    }
    
	public boolean getReusingStore() {
		return false;
	}

	// server part
	protected Map _replicants=new ConcurrentHashMap();

	public void insert(String key, Object tmp) {
		if (_log.isTraceEnabled()) _log.trace("insert: " + key);
		_replicants.put(key, tmp);
	}

	public void replicate(String key, Object value) {
		if (_log.isTraceEnabled()) _log.trace("replicate: " + key);
	}

	public void remove(String key) {
		if (_log.isTraceEnabled()) _log.trace("remove: " + key);
	}

	// restore

	public void nodeDied(Node node) {
		// the partitions owned by this node will be reconstructed and repopulated with session-key:location pairs
		// we need to know which sessions this node was owner of when it died - partition owners will know
		// these sessions may then be promoted to memory in our node and the partition owners will need to be updated as to their new location.
	}

}
