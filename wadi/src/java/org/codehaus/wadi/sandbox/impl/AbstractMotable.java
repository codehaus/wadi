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
package org.codehaus.wadi.sandbox.impl;

import java.io.Serializable;

import org.codehaus.wadi.sandbox.Motable;

/**
 * Implement all of Motable except for the Bytes field. This is the field most likely to have different representations.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public abstract class AbstractMotable extends SimpleEvictable implements Motable, Serializable {

	public void copy(Motable motable) throws Exception {
		super.copy(motable); // Evictable fields
		_id=motable.getId();
		setBytes(motable.getBytes());
	}

	protected String _id;
	public String getId(){return _id;}
	public void setId(String id){_id=id;}
	
	public void tidy(){setInvalidated(true);}

	public void init(long creationTime, long lastAccessedTime, int maxInactiveInterval, boolean invalidated, String id) {
	    init(creationTime, lastAccessedTime, maxInactiveInterval, invalidated);
	    _id=id;
	}
	
	public void destroy() {
	    super.destroy();
	    _id=null;
	}
	
	// N.B. implementation of Bytes field is left abstract...
}


