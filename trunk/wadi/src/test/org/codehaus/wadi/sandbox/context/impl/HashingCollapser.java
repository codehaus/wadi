/*
 * Created on Feb 16, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.codehaus.wadi.sandbox.context.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.sandbox.context.Collapser;

import EDU.oswego.cs.dl.util.concurrent.Mutex;
import EDU.oswego.cs.dl.util.concurrent.Sync;
import EDU.oswego.cs.dl.util.concurrent.TimeoutSync;

/**
 * @author jules
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class HashingCollapser implements Collapser {

	protected final Log    _log = LogFactory.getLog(getClass());
	protected final int    _numSyncs;
	protected final Sync[] _syncs;
	protected final long   _timeout;
	
	/**
	 * 
	 */
	public HashingCollapser(int numSyncs, long timeout) {
		super();
		_numSyncs=numSyncs;
		_timeout=timeout;
		_syncs=new Sync[_numSyncs];
		for (int i=0; i<_numSyncs; i++)
			_syncs[i]=new TimeoutSync(new Mutex(), _timeout);
	}

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.sandbox.context.Collapser#getLock(java.lang.String)
	 */
	public Sync getLock(String id) {
		int index=id.hashCode()%_numSyncs;
		_log.info("collapsed "+id+" to index: "+index);
		return _syncs[index];
	}
}
