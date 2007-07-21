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
package org.codehaus.wadi.core.motable;

import org.codehaus.wadi.core.eviction.SimpleEvictable;
import org.codehaus.wadi.core.eviction.SimpleEvictableMemento;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.WriterPreferenceReadWriteLock;

/**
 * Implement all of Motable except for the Bytes field. This is the field most likely to have different representations.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 2244 $
 */
public abstract class AbstractMotable extends SimpleEvictable implements Motable {
    protected ReadWriteLock readWriteLock;

    protected AbstractMotable() {
        readWriteLock = newReadWriteLock();
    }

    @Override
    protected SimpleEvictableMemento newMemento() {
        return new AbstractMotableMemento();
    }
    
    public AbstractMotableMemento getAbstractMotableMemento() {
        return (AbstractMotableMemento) memento;
    }
    
    public ReadWriteLock getReadWriteLock() {
        return readWriteLock;
    }
    
    public synchronized void init(long creationTime, long lastAccessedTime, int maxInactiveInterval, String name) {
        init(creationTime, lastAccessedTime, maxInactiveInterval);
        getAbstractMotableMemento().setName(name);
    }

    public synchronized void rehydrate(long creationTime, long lastAccessedTime, int maxInactiveInterval, String name, byte[] body)
            throws RehydrationException {
        initExisting(creationTime, lastAccessedTime, maxInactiveInterval, name, body);
    }

    public synchronized void restore(long creationTime, long lastAccessedTime, int maxInactiveInterval, String name, byte[] body)
            throws RehydrationException {
        initExisting(creationTime, lastAccessedTime, maxInactiveInterval, name, body);
    }

    public synchronized void copy(Motable motable) throws Exception {
        super.copy(motable);
        getAbstractMotableMemento().setName(motable.getName());
        getAbstractMotableMemento().setNewSession(false);
        setBodyAsByteArray(motable.getBodyAsByteArray());
    }

    public synchronized void mote(Motable recipient) throws Exception {
        recipient.copy(this);
        destroyForMotion();
    }

    public synchronized String getName() {
        return getAbstractMotableMemento().getName();
    }
    
    public synchronized boolean isNew() {
        return getAbstractMotableMemento().isNewSession();
    }

    public synchronized void destroy() throws Exception {
        super.destroy();
        getAbstractMotableMemento().setNewSession(false);
    }
    
    @Override
    protected void onDeserialization() {
        readWriteLock = newReadWriteLock();
    }
    
    protected ReadWriteLock newReadWriteLock() {
        return new WriterPreferenceReadWriteLock();
    }

    protected synchronized void initExisting(long creationTime,
            long lastAccessedTime,
            int maxInactiveInterval,
            String name,
            byte[] body) throws RehydrationException {
        getAbstractMotableMemento().setNewSession(false);
        init(creationTime, lastAccessedTime, maxInactiveInterval, name);
        try {
            setBodyAsByteArray(body);
        } catch (Exception e) {
            throw new RehydrationException(e);
        }
    }

    protected void destroyForMotion() throws Exception {
        super.destroy();
    }
    
}


