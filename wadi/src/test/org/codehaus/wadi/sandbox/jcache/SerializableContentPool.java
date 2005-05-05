/*
 * Created on Feb 15, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.codehaus.wadi.sandbox.jcache;

import org.codehaus.wadi.sandbox.SerializableContent;

/**
 * @author jules
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public interface SerializableContentPool {

	SerializableContent take();

	SerializableContent poll(long msecs);

}
