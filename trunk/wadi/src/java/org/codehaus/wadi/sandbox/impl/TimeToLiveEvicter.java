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

import org.codehaus.wadi.sandbox.Evictable;
import org.codehaus.wadi.sandbox.Evicter;

/**
 * An Evicter which evicts Evictables with less than a certain time to live remaining
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class TimeToLiveEvicter implements Evicter {
	protected final long _ttl;

	public TimeToLiveEvicter(long ttl) {
		_ttl=ttl;
	}

	public boolean evict(String id, Evictable evictable) {return evict(id, evictable, System.currentTimeMillis());}
	public boolean evict(String id, Evictable evictable, long time) {return evictable.getTimeToLive(time)<=_ttl;}
}
