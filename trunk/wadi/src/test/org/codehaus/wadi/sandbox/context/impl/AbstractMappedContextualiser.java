/*
 * Created on Feb 16, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.codehaus.wadi.sandbox.context.impl;

import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.sandbox.context.Context;
import org.codehaus.wadi.sandbox.context.Contextualiser;
import org.codehaus.wadi.sandbox.context.Evicter;

import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * @author jules
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public abstract class AbstractMappedContextualiser extends
		AbstractChainedContextualiser {

	protected final Log _log = LogFactory.getLog(getClass());
	protected final Map _map;
	protected final Evicter _evicter;
	
	/**
	 * @param next
	 */
	public AbstractMappedContextualiser(Contextualiser next, Map map, Evicter evicter) {
		super(next);
		_map=map;
		_evicter=evicter;
	}
	
	protected String _stringPrefix="<"+getClass().getName()+":";
	protected String _stringSuffix=">";
	public String toString() {
		return new StringBuffer(_stringPrefix).append(_map.size()).append(_stringSuffix).toString();
	}

	public void evict() {
		for (Iterator i=_map.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry e=(Map.Entry)i.next();
			String key=(String)e.getKey();
			Context val=(Context)e.getValue();
			if (_evicter.evict(key, val)) { // first test without lock - cheap
				Sync exclusive=val.getExclusiveLock();
				try {
					if (exclusive.attempt(0) && _evicter.evict(key, val)) { // then confirm with exclusive lock
						// do we need the promotion lock ? don't think so - TODO
						_next.demote(key, val);
						i.remove();
						exclusive.release();
					}
				} catch (InterruptedException ie) {
					_log.warn("unexpected interruption to eviction - ignoring", ie);
				}
			}
		}
	}
}
