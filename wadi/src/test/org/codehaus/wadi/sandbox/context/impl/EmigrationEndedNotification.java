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
package org.codehaus.wadi.sandbox.context.impl;

import java.io.Serializable;

import javax.jms.Destination;

/**
 * Sent to notify Cluster memebers of a new Queue from which they should take and acknowledge
 * Context emigrations...
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class EmigrationEndedNotification implements Serializable {
	protected Destination _emigrationQueue;

	/**
	 *
	 */
	public EmigrationEndedNotification(Destination emigrationQueue) {
		super();
		_emigrationQueue=emigrationQueue;
	}

	public EmigrationEndedNotification() {
		// for use when demarshalling...
	}

	public Destination getDestination(){return _emigrationQueue;}
}
