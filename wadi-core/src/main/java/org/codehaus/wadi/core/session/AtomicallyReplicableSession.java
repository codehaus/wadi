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

package org.codehaus.wadi.core.session;

import org.codehaus.wadi.core.manager.Manager;
import org.codehaus.wadi.core.motable.RehydrationException;
import org.codehaus.wadi.core.util.Streamer;
import org.codehaus.wadi.replication.manager.ReplicationManager;


/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1497 $
 */
public class AtomicallyReplicableSession extends AbstractReplicableSession {

	interface Semantics {
        boolean getAttributeDirties();
    }

    class ByReferenceSemantics implements Semantics {
        public boolean getAttributeDirties() {
            return true;
        }
    }

    class ByValueSemantics implements Semantics {
        public boolean getAttributeDirties() {
            return false;
        }
    }

    protected transient boolean dirty = false;
    protected transient Semantics semantics = new ByReferenceSemantics();

    public AtomicallyReplicableSession(Attributes attributes,
            Manager manager,
            Streamer streamer,
            ReplicationManager replicationManager) {
        super(attributes, manager, streamer, replicationManager);
    }

    public synchronized void rehydrate(long creationTime, long lastAccessedTime, int maxInactiveInterval, String name, byte[] body)
            throws RehydrationException {
        super.rehydrate(creationTime, lastAccessedTime, maxInactiveInterval, name, body);
        replicationManager.acquirePrimary(getName());
    }

    public synchronized void onEndProcessing() {
        if (getAbstractMotableMemento().isNewSession()) {
            replicationManager.create(getName(), this);
            getAbstractMotableMemento().setNewSession(false);
            dirty = false;
        } else if (dirty) {
            replicationManager.update(getName(), this);
            dirty = false;
        }
    }

    /**
     * if MII changes - dirties the session metadata - might this be distributed
     * separately ? we could probably distribute this as a delta, since there
     * are no object reference issues - it would be crazy to send the whole
     * session to update this.
     */
    public synchronized void setMaxInactiveInterval(int maxInactiveInterval) {
        if (memento.getMaxInactiveInterval() != maxInactiveInterval) {
            super.setMaxInactiveInterval(maxInactiveInterval);
            dirty = true;
        }
    }

    /**
     * this will sometimes dirty the session, since we are giving away
     * a ref to something inside the session which may then be
     * modified without our knowledge - strictly speaking, if we are
     * using ByReference semantics, this dirties. If we are using
     * ByValue semantics, it does not.
     */
    public synchronized Object getState(Object key) {
        Object tmp = super.getState(key);
        dirty = (tmp != null) && semantics.getAttributeDirties();
        return tmp;
    };

    protected void onAddSate(Object key, Object oldValue, Object newValue) {
        dirty = true;
    }

    protected void onRemoveState(Object key, Object oldValue) {
        dirty = true;
    }

    protected void onDestroy() {
        dirty = false;
    }
    
}
