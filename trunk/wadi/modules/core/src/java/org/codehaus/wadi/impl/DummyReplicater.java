package org.codehaus.wadi.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Replicater;

public class DummyReplicater implements Replicater {

    protected final Log _log = LogFactory.getLog(getClass());

    public void create(Object tmp) {
		_log.info("create: "+((ReplicableSession)tmp).getId());
    }
    
    public void replicate(Object tmp) { //TODO
		_log.info("replicate: "+((ReplicableSession)tmp).getId());
	}

    public void destroy(Object tmp) { //TODO
		_log.info("destroy: "+((ReplicableSession)tmp).getId());
	}

}
