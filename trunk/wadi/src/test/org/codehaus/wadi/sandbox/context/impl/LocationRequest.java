/*
 * Created on Feb 22, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.codehaus.wadi.sandbox.context.impl;

import java.io.Serializable;

/**
 * @author jules
 * 
 * A query for the location of the session with the enclosed ID - The response
 * should be a LocationResponse object sent whence this request arrived.
 */
public class LocationRequest implements Serializable {
	protected String _id;
	protected long _handOverPeriod;

	/**
	 * 
	 */
	public LocationRequest(String id, long handOverPeriod) {
		super();
		_id=id;
		_handOverPeriod=handOverPeriod;
	}
	
	public LocationRequest() {
		// for use when demarshalling...
	}
	
	public String getId(){return _id;}
	public long getHandOverPeriod(){return _handOverPeriod;}
}
