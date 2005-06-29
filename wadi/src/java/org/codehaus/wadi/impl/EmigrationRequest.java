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

import org.codehaus.wadi.Motable;

/**
 * A query for the location of the session with the enclosed ID - The response
 * should be a LocationResponse object sent whence this request arrived.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class EmigrationRequest implements Serializable {
	protected Motable _motable;

	/**
	 *
	 */
	public EmigrationRequest(Motable motable) {
		super();
		_motable=motable;
	}

	public EmigrationRequest() {
		// for use when demarshalling...
	}

	public Motable getMotable(){return _motable;}

  public String toString() {
    return "<EmigrationRequest: "+_motable.getName()+">";
  }

}
