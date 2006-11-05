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

package org.codehaus.wadi.web.impl;

import org.codehaus.wadi.RehydrationException;
import org.codehaus.wadi.Replicater;
import org.codehaus.wadi.web.ReplicableSessionConfig;


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

    protected transient boolean _dirty;
    protected transient Semantics _semantics = new ByReferenceSemantics();
    protected transient Replicater _replicater;

    public AtomicallyReplicableSession(ReplicableSessionConfig config) {
        super(config);
        _dirty = false;
        // take ownership of the Replicater - it may carry per-Session state
        _replicater = ((ReplicableSessionConfig) _config).getReplicater();
    }

    public void rehydrate(long creationTime, long lastAccessedTime, int maxInactiveInterval, String name, byte[] body)
            throws RehydrationException {
        super.rehydrate(creationTime, lastAccessedTime, maxInactiveInterval, name, body);
        _replicater.acquireFromOtherReplicater(this);
    }

    public void readEnded() {
        // N.B. this is called from our RWLock inside an implicit exclusive lock, so we should not need to worry
        // about synchronisation.
        if (newSession) {
            _replicater.create(this);
            newSession = false;
            _dirty = false;
        }
        if (_dirty) {
            _replicater.update(this);
            _dirty = false;
        }
    }

    /**
     * if MII changes - dirties the session metadata - might this be distributed
     * separately ? we could probably distribute this as a delta, since there
     * are no object reference issues - it would be crazy to send the whole
     * session to update this.
     */
    public void setMaxInactiveInterval(int maxInactiveInterval) {
        if (maxInactiveInterval != maxInactiveInterval) {
            super.setMaxInactiveInterval(maxInactiveInterval);
            _dirty = true;
        }
    }

    /**
     * I don't think that the container can efficiently check to see whether the
     * new value is the same as the old one. We would be second guessing
     * application code. I think it is up to the developer to make this check.
     */
    public Object setAttribute(String name, Object value) {
        Object tmp = super.setAttribute(name, value);
        _dirty = true;
        return tmp;
    }

    public Object removeAttribute(String name) {
        Object tmp = super.removeAttribute(name);
        _dirty = tmp != null;
        return tmp;
    }

    /**
     * this will sometimes dirty the session, since we are giving away
     * a ref to something inside the session which may then be
     * modified without our knowledge - strictly speaking, if we are
     * using ByReference semantics, this dirties. If we are using
     * ByValue semantics, it does not.
     */
    public Object getAttribute(String name) {
        Object tmp = super.getAttribute(name);
        _dirty = (tmp != null) && _semantics.getAttributeDirties();
        return _attributes.get(name);
    }

}
