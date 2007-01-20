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

import org.codehaus.wadi.Manager;
import org.codehaus.wadi.RehydrationException;
import org.codehaus.wadi.Replicater;
import org.codehaus.wadi.Streamer;
import org.codehaus.wadi.ValuePool;
import org.codehaus.wadi.web.AttributesFactory;
import org.codehaus.wadi.web.Router;
import org.codehaus.wadi.web.ValueHelperRegistry;
import org.codehaus.wadi.web.WebSessionConfig;
import org.codehaus.wadi.web.WebSessionWrapperFactory;


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

    public AtomicallyReplicableSession(WebSessionConfig config,
            AttributesFactory attributesFactory,
            WebSessionWrapperFactory wrapperFactory,
            ValuePool valuePool,
            Router router,
            Manager manager,
            Streamer streamer,
            ValueHelperRegistry valueHelperRegistry,
            Replicater replicater) {
        super(config,
                attributesFactory,
                wrapperFactory,
                valuePool,
                router,
                manager,
                streamer,
                valueHelperRegistry,
                replicater);
    }

    public synchronized void rehydrate(long creationTime, long lastAccessedTime, int maxInactiveInterval, String name, byte[] body)
            throws RehydrationException {
        super.rehydrate(creationTime, lastAccessedTime, maxInactiveInterval, name, body);
        replicater.acquireFromOtherReplicater(this);
    }

    public synchronized void onEndProcessing() {
        if (newSession) {
            replicater.create(this);
            newSession = false;
            dirty = false;
        } else if (dirty) {
            replicater.update(this);
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
        if (maxInactiveInterval != maxInactiveInterval) {
            super.setMaxInactiveInterval(maxInactiveInterval);
            dirty = true;
        }
    }

    /**
     * I don't think that the container can efficiently check to see whether the
     * new value is the same as the old one. We would be second guessing
     * application code. I think it is up to the developer to make this check.
     */
    public synchronized Object setAttribute(String name, Object value) {
        Object tmp = super.setAttribute(name, value);
        dirty = true;
        return tmp;
    }

    public synchronized Object removeAttribute(String name) {
        Object tmp = super.removeAttribute(name);
        dirty = tmp != null;
        return tmp;
    }

    /**
     * this will sometimes dirty the session, since we are giving away
     * a ref to something inside the session which may then be
     * modified without our knowledge - strictly speaking, if we are
     * using ByReference semantics, this dirties. If we are using
     * ByValue semantics, it does not.
     */
    public synchronized Object getAttribute(String name) {
        Object tmp = super.getAttribute(name);
        dirty = (tmp != null) && semantics.getAttributeDirties();
        return tmp;
    }

    public synchronized void destroy() throws Exception {
        super.destroy();
        dirty = false;
    }
    
}
