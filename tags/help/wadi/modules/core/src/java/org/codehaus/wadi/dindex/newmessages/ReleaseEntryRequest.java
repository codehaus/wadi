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
package org.codehaus.wadi.dindex.newmessages;

import java.io.Serializable;

import org.codehaus.wadi.Motable;
import org.codehaus.wadi.OldMessage;

/**
 * A request for the emigration of the enclosed session - The response
 * should be a ReleaseEntryResponse object sent whence this request arrived.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class ReleaseEntryRequest implements OldMessage, Serializable {
	protected Motable _motable;
	
	/**
	 *
	 */
	public ReleaseEntryRequest(Motable motable) {
		super();
		_motable=motable;
	}
	
	public ReleaseEntryRequest() {
		// for use when demarshalling...
	}
	
	public Motable getMotable() {
		return _motable;
	}
	
	public String toString() {
		return "<EmigrationRequest: "+_motable.getName()+">";
	}
	
}
