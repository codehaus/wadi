package org.codehaus.wadi.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.NewReplicater;

public class DummyReplicater implements NewReplicater {

    protected final Log _log = LogFactory.getLog(getClass());

    public void create(Object tmp) {
		_log.info("create: "+((AbstractReplicableSession)tmp).getId());
    }
    
    public void replicate(Object tmp) { //TODO
		_log.info("replicate: "+((AbstractReplicableSession)tmp).getId());
	}

    public void destroy(Object tmp) { //TODO
		_log.info("destroy: "+((AbstractReplicableSession)tmp).getId());
	}

}
