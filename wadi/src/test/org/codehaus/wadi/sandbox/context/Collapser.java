/*
 * Created on Feb 16, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.codehaus.wadi.sandbox.context;

import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * @author jules
 *
 * Used to provide a Lock on which to collpase requests sharing the same session id.
 */
public interface Collapser {
	
	Sync getLock(String id);

}
