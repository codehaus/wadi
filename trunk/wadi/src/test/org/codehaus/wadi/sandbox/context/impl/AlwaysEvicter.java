/*
 * Created on Feb 17, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.codehaus.wadi.sandbox.context.impl;

import org.codehaus.wadi.sandbox.context.Evicter;
import org.codehaus.wadi.sandbox.context.Motable;

/**
 * @author jules
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class AlwaysEvicter implements Evicter {

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.sandbox.context.Evicter#evict(java.lang.String, org.codehaus.wadi.sandbox.context.Motable)
	 */
	public boolean evict(String key, Motable val) {
		return true;
	}

}
