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

/**
 * An Evicter which also evicts Evictables after an absolute period of inactivity.
 * For example, using this Evicter, you could evict sessions after 30 minutes of inactivity.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class AbsoluteEvicter extends AbstractEvicter {
	protected final long _interval;

	public AbsoluteEvicter(long interval) {
	    super();
	    _interval=interval;
	}

	public boolean evict(String id, Evictable evictable, long time) {
	    return super.evict(id, evictable, time) || time-evictable.getLastAccessedTime()>=_interval;
	    }
}
