/**
 *
 * Copyright 2003-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.codehaus.wadi.sandbox.context.impl;

import java.io.Serializable;
import java.util.Set;

import org.codehaus.wadi.sandbox.context.Location;

/**
 * TODO - JavaDoc this type
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class LocationResponse implements Serializable {

	protected final Location _location;
	protected final Set _ids;
	/**
	 *
	 */
	public LocationResponse(Location location, Set ids) {
		super();
		_location=location;
		_ids=ids;
	}

	public Location getLocation(){return _location;}
	public Set getIds(){return _ids;}
}
