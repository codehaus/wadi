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
import org.codehaus.wadi.core.motable.AbstractChainedEmoter;
import org.codehaus.wadi.core.motable.AbstractImmoter;
import org.codehaus.wadi.core.motable.Emoter;
import org.codehaus.wadi.core.motable.Immoter;
import org.codehaus.wadi.core.motable.Motable;
import org.codehaus.wadi.core.session.Session;
import org.codehaus.wadi.core.store.Store;
import org.codehaus.wadi.core.util.Utils;
import org.codehaus.wadi.location.statemanager.StateManager;

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
    private final SessionMonitor sessionMonitor;

	public SharedStoreContextualiser(Contextualiser next,
            Store store,
            StateManager stateManager,
            SessionMonitor sessionMonitor) {
		super(next);
        if (null == store) {
            throw new IllegalArgumentException("store is required");
        } else if (null == stateManager) {
            throw new IllegalArgumentException("stateManager is required");
        } else if (null == sessionMonitor) {
            throw new IllegalArgumentException("sessionMonitor is required");
        }
        this.store = store;
        this.stateManager = stateManager;
        this.sessionMonitor = sessionMonitor;
        
        immoter = new SharedImmoter();
        emoter = new AbstractChainedEmoter();
    }

    public void start() throws Exception {
        super.start();
    }

    public Immoter getImmoter() {
        return immoter;
    }

    public Emoter getEmoter() {
        return emoter;
    }

    public Immoter getDemoter(String name, Motable motable) {
        // TODO - should check _next... - just remove when we have an evicter sorted
        return new SharedImmoter();
    }
    
    /**
     * Shared Contextualisers do nothing at runtime. They exist only to load data at startup and store it at shutdown.
     */
    protected Motable get(String id, boolean exclusiveOnly) {
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

        public SharedPutter(Emoter emoter, Immoter immoter) {
            this.emoter = emoter;
            this.immoter = immoter;
        }

        public void put(String name, Motable motable) {
            stateManager.insert(name);
            Session session = (Session) Utils.mote(emoter, immoter, motable, name);
            sessionMonitor.notifySessionCreation(session);
        }
    }

}
