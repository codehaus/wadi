/*
 * Created on Feb 22, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.codehaus.wadi.sandbox.context.impl;

import java.io.Serializable;
import java.util.Set;

import org.codehaus.wadi.sandbox.context.Location;

/**
 * @author jules
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Locations implements Serializable {

	protected final Location _location;
	protected final Set _ids;
	/**
	 * 
	 */
	public Locations(Location location, Set ids) {
		super();
		_location=location;
		_ids=ids;
	}

	public Location getLocation(){return _location;}
	public Set getIds(){return _ids;}
}
