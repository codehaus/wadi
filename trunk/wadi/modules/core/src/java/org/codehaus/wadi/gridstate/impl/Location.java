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
package org.codehaus.wadi.gridstate.impl;

import java.io.Serializable;

import javax.jms.Destination;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.ReaderPreferenceReadWriteLock;


/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class Location implements Serializable {

	protected transient ReadWriteLock _lock;
	protected transient boolean _invalid;
	protected Destination _destination;

	public Location(Destination destination) {
		_lock=new ReaderPreferenceReadWriteLock();
		_destination=destination;
	}

	public ReadWriteLock getLock() {
		return _lock;
	}

	public void invalidate() {
		_invalid=true;
	}

	public Object getValue() {
		return _destination;
	}

	public void setValue(Object destination) {
		_destination=(Destination)destination;
	}
}
