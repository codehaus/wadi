/**
 *
 * Copyright 2003-2005 Core Developers Network Ltd.
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
package org.codehaus.wadi.impl;

import java.io.Serializable;

import org.codehaus.wadi.Location;

/**
 * A message used to acknowledge a successful immigration
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class ImmigrationAcknowledgement implements Serializable {
	protected String _id;
	protected Location _location;

	/**
	 *
	 */
	public ImmigrationAcknowledgement(String id, Location location) {
		super();
		_id=id;
		_location=location;
	}

	public ImmigrationAcknowledgement() {
		// for use when demarshalling...
	}

	public String getId(){return _id;}
	public Location getLocation(){return _location;}
}