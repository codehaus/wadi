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
 * A very Simple impementation of Evictable
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public abstract class SimpleEvictable implements Evictable {

	public SimpleEvictable() {
		_creationTime=System.currentTimeMillis();
		_lastAccessedTime=_creationTime;
	}

	protected long _creationTime;
	public long getCreationTime() {return _creationTime;}
	public void setCreationTime(long creationTime){_creationTime=creationTime;}

	protected long _lastAccessedTime;
	public long getLastAccessedTime(){return _lastAccessedTime;}
	public void setLastAccessedTime(long lastAccessedTime){_lastAccessedTime=lastAccessedTime;}

	protected int _maxInactiveInterval;
	public int  getMaxInactiveInterval(){return _maxInactiveInterval;}
	public void setMaxInactiveInterval(int maxInactiveInterval){_maxInactiveInterval=maxInactiveInterval;}

	public long getTimeToLive(long time) {return _maxInactiveInterval<0?Long.MAX_VALUE:(_maxInactiveInterval*1000)-(time-_lastAccessedTime);}

	public boolean getTimedOut() {return getTimedOut(System.currentTimeMillis());}
	public boolean getTimedOut(long time) {return getTimeToLive(time)<=0;}
	
	protected boolean _invalidated;
	public boolean getInvalidated(){return _invalidated;}
	public void setInvalidated(boolean invalidated){_invalidated=invalidated;}
	
	public boolean getValid() {return !getInvalidated() && !getTimedOut();}

	public void copy(Evictable evictable) throws Exception {
		_creationTime=evictable.getCreationTime();
		_lastAccessedTime=evictable.getLastAccessedTime();
		_maxInactiveInterval=evictable.getMaxInactiveInterval();
	}
}
