/*
 * Created on Feb 22, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.codehaus.wadi.sandbox.context;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * @author jules
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public interface Location extends Motable {
	/**
	 * @param req - the request object from the webcontainer
	 * @param res - the response object from the webcontainer
	 * @param promotionLock - used to collapse concurrent threads seeking same context into one stack descent
	 * @return - null if no migration has occurred (in which case the promotionLock has been released), or an immigrating Context (in which case the promotionLock should be released as soon as it has been promoted).
	 */
	public Context proxy(ServletRequest req, ServletResponse res, String id, Sync promotionLock);
}
