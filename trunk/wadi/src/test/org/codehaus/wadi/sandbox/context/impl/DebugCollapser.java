/*
 * Created on Feb 18, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.codehaus.wadi.sandbox.context.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.sandbox.context.Collapser;

import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * @author jules
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class DebugCollapser implements Collapser {
	protected final Log _log = LogFactory.getLog(getClass());

	class DebugSync implements Sync {
		protected int _counter;
		public void acquire(){_log.info("acquire: "+ (_counter++));}
		public void release(){_log.info("release: "+ (--_counter));}
		public boolean attempt(long timeout){_log.info("attempt: "+ (++_counter));return true;}
	}
	
	protected Sync _sync=new DebugSync();
	
	/**
	 * 
	 */
	public DebugCollapser() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.sandbox.context.Collapser#getLock(java.lang.String)
	 */
	public Sync getLock(String id) {
		return _sync;
	}
}
