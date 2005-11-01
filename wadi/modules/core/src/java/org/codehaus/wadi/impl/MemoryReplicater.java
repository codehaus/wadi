package org.codehaus.wadi.impl;

import java.util.Map;

import org.activecluster.Node;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Replicater;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;

public class MemoryReplicater implements Replicater {

	protected final Log _log = LogFactory.getLog(getClass());
	
	protected int numReplicants;
	
	public MemoryReplicater(int numReplicants) {
	}
	
	// client part
	
	public void create(Object tmp) {
		_log.trace("create: "+tmp);
		// decide on replication partners (not ourselves)
		// sned messages (sync) to insert replicants on these partners
	}

	public void update(Object tmp) {
		_log.trace("update: "+tmp);
		// send messages (sync) to replicate to our partners
	}

	public void destroy(Object tmp) {
		_log.trace("destroy: "+tmp);
		// send messages (sync) to remove replicants on partners
	}

	public boolean getReusingStore() {
		return false;
	}

	// server part
	protected Map _replicants=new ConcurrentHashMap();
	
	public void insert(String key, Object tmp) {
		_log.trace("insert: "+key);
		_replicants.put(key, tmp);
	}
	
	public void replicate(String key, Object value) {
		_log.trace("replicate: "+key);
	}
	
	public void remove(String key) {
		_log.trace("remove: "+key);
	}
	
	// restore
	
	public void nodeDied(Node node) {
		// the partitions owned by this node will be reconstructed and repopulated with session-key:location pairs
		// we need to know which sessions this node was owner of when it died - partition owners will know
		// these sessions may then be promoted to memory in our node and the partition owners will need to be updated as to their new location.
	}
	
}
