/**
 * Copyright 2007 The Apache Software Foundation
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
package org.codehaus.wadi.core.eviction;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;


/**
 * 
 * @version $Revision: 1538 $
 */
public class SimpleEvictableMemento implements Externalizable {
    protected long creationTime;
    protected long lastAccessedTime;
    protected int maxInactiveInterval;
    protected boolean neverEvict;

    public long getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }
    
    public long getLastAccessedTime() {
        return lastAccessedTime;
    }

    public void setLastAccessedTime(long lastAccessedTime) {
        this.lastAccessedTime = lastAccessedTime;
    }

    public int getMaxInactiveInterval() {
        return maxInactiveInterval;
    }

    public void setMaxInactiveInterval(int maxInactiveInterval) {
        this.maxInactiveInterval = maxInactiveInterval;
    }

    public long getTimeToLive(long time) {
        return maxInactiveInterval < 0 ? Long.MAX_VALUE : (maxInactiveInterval * 1000) - (time - lastAccessedTime);
    }

    public boolean getTimedOut(long time) {
        return getTimeToLive(time) <= 0;
    }
    
    public void setNeverEvict(boolean neverEvict) {
        this.neverEvict = neverEvict;
    }
    
    public boolean isNeverEvict() {
        return neverEvict;
    }
    
    public synchronized void readExternal(ObjectInput oi) throws IOException, ClassNotFoundException {
        creationTime = oi.readLong();
        lastAccessedTime = oi.readLong();
        maxInactiveInterval = oi.readInt();
        neverEvict = oi.readBoolean();
    }

    public synchronized void writeExternal(ObjectOutput oo) throws IOException {
        oo.writeLong(creationTime);
        oo.writeLong(lastAccessedTime);
        oo.writeInt(maxInactiveInterval);
        oo.writeBoolean(neverEvict);
    }

}
