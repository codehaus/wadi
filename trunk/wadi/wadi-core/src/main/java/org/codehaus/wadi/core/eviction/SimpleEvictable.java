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
package org.codehaus.wadi.core.eviction;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;


/**
 * A very Simple impementation of Evictable
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 2244 $
 */
public abstract class SimpleEvictable implements Evictable, Externalizable {
    protected SimpleEvictableMemento memento;

    public SimpleEvictable() {
        memento = newMemento();
    }

    protected SimpleEvictableMemento newMemento() {
        return new SimpleEvictableMemento();
    }
    
    public synchronized void init(long creationTime, long lastAccessedTime, int maxInactiveInterval) {
        memento.setCreationTime(creationTime);
        memento.setLastAccessedTime(lastAccessedTime);
        memento.setMaxInactiveInterval(maxInactiveInterval);
    }

    public synchronized void destroy() throws Exception {
    }

    public synchronized void copy(Evictable evictable) throws Exception {
        memento.setCreationTime(evictable.getCreationTime());
        memento.setLastAccessedTime(evictable.getLastAccessedTime());
        memento.setMaxInactiveInterval(evictable.getMaxInactiveInterval());
    }

    public synchronized void mote(Evictable recipient) throws Exception {
        recipient.copy(this);
        destroy();
    }

    public synchronized long getCreationTime() {
        return memento.getCreationTime();
    }

    public synchronized long getLastAccessedTime() {
        return memento.getLastAccessedTime();
    }

    public synchronized void setLastAccessedTime(long lastAccessedTime) {
        memento.setLastAccessedTime(lastAccessedTime);
    }

    public synchronized int getMaxInactiveInterval() {
        return memento.getMaxInactiveInterval();
    }

    public synchronized void setMaxInactiveInterval(int maxInactiveInterval) {
        memento.setMaxInactiveInterval(maxInactiveInterval);
    }

    public synchronized long getTimeToLive(long time) {
        return memento.getTimeToLive(time);
    }

    public synchronized boolean getTimedOut(long time) {
        return memento.getTimedOut(time);
    }
    
    public synchronized void readExternal(ObjectInput oi) throws IOException, ClassNotFoundException {
        memento.readExternal(oi);
        onDeserialization();
    }

    public synchronized void writeExternal(ObjectOutput oo) throws IOException {
        onSerialization();
        memento.writeExternal(oo);
    }

    protected void onDeserialization() {
    }
    
    protected void onSerialization() {
    }

}
