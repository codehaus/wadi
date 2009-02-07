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

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.codehaus.wadi.core.eviction.SimpleEvictable;
import org.codehaus.wadi.core.eviction.SimpleEvictableMemento;
import org.codehaus.wadi.core.util.LoggingReadWriteLock;
import org.codehaus.wadi.core.util.LoggingLock.IdAccessor;

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
    
    public synchronized void init(long creationTime, long lastAccessedTime, int maxInactiveInterval, Object id) {
        init(creationTime, lastAccessedTime, maxInactiveInterval);
        getAbstractMotableMemento().setId(id);
    }

    public synchronized void rehydrate(long creationTime, long lastAccessedTime, int maxInactiveInterval, Object id, byte[] body)
            throws RehydrationException {
        initExisting(creationTime, lastAccessedTime, maxInactiveInterval, id, body);
    }

    public synchronized void restore(long creationTime, long lastAccessedTime, int maxInactiveInterval, Object id, byte[] body)
            throws RehydrationException {
        initExisting(creationTime, lastAccessedTime, maxInactiveInterval, id, body);
    }

    public synchronized void copy(Motable motable) throws Exception {
        super.copy(motable);
        getAbstractMotableMemento().setId(motable.getId());
        getAbstractMotableMemento().setNewSession(false);
        setBodyAsByteArray(motable.getBodyAsByteArray());
    }

    public synchronized void mote(Motable recipient) throws Exception {
        recipient.copy(this);
        destroyForMotion();
    }

    public synchronized Object getId() {
        return getAbstractMotableMemento().getId();
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
        ReadWriteLock delegate = new ReentrantReadWriteLock(true);
        return new LoggingReadWriteLock(new IdAccessor() {
            public Object getId() {
                return AbstractMotable.this.getId();
            }
        }, delegate);
    }

    protected synchronized void initExisting(long creationTime,
            long lastAccessedTime,
            int maxInactiveInterval,
            Object id,
            byte[] body) throws RehydrationException {
        getAbstractMotableMemento().setNewSession(false);
        init(creationTime, lastAccessedTime, maxInactiveInterval, id);
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


