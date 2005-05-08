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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

import org.codehaus.wadi.Evictable;

/**
 * A very Simple impementation of Evictable
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public abstract class SimpleEvictable implements Evictable, Serializable {

    public void init(long creationTime, long lastAccessedTime, int maxInactiveInterval) {
        _creationTime=creationTime;
        _lastAccessedTime=lastAccessedTime;
        _maxInactiveInterval=maxInactiveInterval;
    }
    
    public void destroy() {
        _creationTime=0;
        _lastAccessedTime=0;
        _maxInactiveInterval=0;
    }
    
    public void copy(Evictable evictable) throws Exception {
        _creationTime=evictable.getCreationTime();
        _lastAccessedTime=evictable.getLastAccessedTime();
        _maxInactiveInterval=evictable.getMaxInactiveInterval();
    }
    
    public void readContent(ObjectInput oi) throws IOException, ClassNotFoundException {
        _creationTime=oi.readLong();
        _lastAccessedTime=oi.readLong();
        _maxInactiveInterval=oi.readInt();
    }
    
    public void writeContent(ObjectOutput oo) throws IOException {
        oo.writeLong(_creationTime);
        oo.writeLong(_lastAccessedTime);
        oo.writeInt(_maxInactiveInterval);
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
    
    public boolean isNew(){return _lastAccessedTime==_creationTime;} // assumes lastAccessedTime is only updated once per request...
    
	public boolean checkTimeframe(long time) {return !(_creationTime>time || _lastAccessedTime>time);}
	
	public long getTimeToLive(long time) {return _maxInactiveInterval<0?Long.MAX_VALUE:(_maxInactiveInterval*1000)-(time-_lastAccessedTime);}

	public boolean getTimedOut(long time) {return getTimeToLive(time)<=0;}
	
}
