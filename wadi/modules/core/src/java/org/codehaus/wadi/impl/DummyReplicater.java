package org.codehaus.wadi.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Replicater;

public class DummyReplicater implements Replicater {

    protected final Log _log = LogFactory.getLog(getClass());

    public void create(Object tmp) {
		_log.info("create: "+((AbstractReplicableSession)tmp).getId());
		// write a row into the DB
    }
    
    public void replicate(Object tmp) { //TODO
		_log.info("replicate: "+((AbstractReplicableSession)tmp).getId());
		// update a row in the DB
	}

    public void destroy(Object tmp) { //TODO
		_log.info("destroy: "+((AbstractReplicableSession)tmp).getId());
		// remove a row in the DB
	}

}
