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
package org.codehaus.wadi.core.contextualiser;

import org.codehaus.wadi.core.manager.SessionMonitor;
import org.codehaus.wadi.core.motable.BaseEmoter;
import org.codehaus.wadi.core.motable.AbstractImmoter;
import org.codehaus.wadi.core.motable.Emoter;
import org.codehaus.wadi.core.motable.Immoter;
import org.codehaus.wadi.core.motable.Motable;
import org.codehaus.wadi.core.session.Session;
import org.codehaus.wadi.core.store.Store;
import org.codehaus.wadi.core.util.CreateSessionOperation;
import org.codehaus.wadi.core.util.InsertSessionOperation;
import org.codehaus.wadi.core.util.Utils;
import org.codehaus.wadi.location.statemanager.StateManager;
import org.codehaus.wadi.replication.manager.ReplicationManager;

/**
 * A Contextualiser which stores its Contexts in a shared database via JDBC.
 * On shutdown of the cluster's last node, all extant sessions will be demoted to here.
 * On startup of the cluster's first node, all sessions stored here will be promoted upwards.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class SharedStoreContextualiser extends AbstractSharedContextualiser {
    private final Store store;
    private final Immoter immoter;
    private final Emoter emoter;
    private final StateManager stateManager;
    private final ReplicationManager replicationManager;
    private final SessionMonitor sessionMonitor;

	public SharedStoreContextualiser(Contextualiser next,
            Store store,
            StateManager stateManager,
            ReplicationManager replicationManager,
            SessionMonitor sessionMonitor) {
		super(next);
        if (null == store) {
            throw new IllegalArgumentException("store is required");
        } else if (null == stateManager) {
            throw new IllegalArgumentException("stateManager is required");
        } else if (null == replicationManager) {
            throw new IllegalArgumentException("replicationManager is required");
        } else if (null == sessionMonitor) {
            throw new IllegalArgumentException("sessionMonitor is required");
        }
        this.store = store;
        this.stateManager = stateManager;
        this.replicationManager = replicationManager;
        this.sessionMonitor = sessionMonitor;
        
        immoter = new SharedImmoter();
        emoter = new BaseEmoter();
    }

    public Immoter getImmoter() {
        return immoter;
    }

    public Emoter getEmoter() {
        return emoter;
    }

    /**
     * Shared Contextualisers do nothing at runtime. They exist only to load data at startup and store it at shutdown.
     */
    protected Motable get(Object id, boolean exclusiveOnly) {
        return null;
    }

    protected void load(Emoter emoter, Immoter immoter) {
        store.load(new SharedPutter(emoter, immoter));
    }

    /**
     * An Emoter that deals in terms of StoreMotables
     * 
     * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
     * @version $Revision$
     */
    protected class SharedImmoter extends AbstractImmoter {

        public Motable newMotable(Motable emotable) {
            return store.create();
        }

    }

    protected class SharedPutter implements Store.Putter {
        protected final Emoter emoter;
        protected final Immoter immoter;
        protected final InsertSessionOperation insertSessionOperation;

        public SharedPutter(Emoter emoter, Immoter immoter) {
            this.emoter = emoter;
            this.immoter = immoter;
            
            insertSessionOperation = new InsertSessionOperation(replicationManager,
                    sessionMonitor,
                    stateManager);

        }

        public void put(final Object id, final Motable motable) {
            insertSessionOperation.insert(id, new CreateSessionOperation() {
                public Session create() {
                    return (Session) Utils.mote(emoter, immoter, motable);
                }
            });
        }
    }

}
