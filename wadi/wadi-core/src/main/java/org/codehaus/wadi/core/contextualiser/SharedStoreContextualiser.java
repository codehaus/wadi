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

import org.codehaus.wadi.core.motable.AbstractChainedEmoter;
import org.codehaus.wadi.core.motable.AbstractImmoter;
import org.codehaus.wadi.core.motable.Emoter;
import org.codehaus.wadi.core.motable.Immoter;
import org.codehaus.wadi.core.motable.Motable;
import org.codehaus.wadi.core.store.DatabaseStore;
import org.codehaus.wadi.core.store.Store;
import org.codehaus.wadi.core.store.StoreMotable;
import org.codehaus.wadi.core.util.Utils;

import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * A Contextualiser which stores its Contexts in a shared database via JDBC.
 * On shutdown of the cluster's last node, all extant sessions will be demoted to here.
 * On startup of the cluster's first node, all sessions stored here will be promoted upwards.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class SharedStoreContextualiser extends AbstractSharedContextualiser {
    private final boolean clean;
    private final DatabaseStore _store;
    private final Immoter _immoter;
    private final Emoter _emoter;
    private final boolean accessOnLoad;

	public SharedStoreContextualiser(Contextualiser next,
            boolean clean,
            DatabaseStore store,
            boolean accessOnLoad) {
		super(next);
        this.clean = clean;
        _store = store;
        this.accessOnLoad = accessOnLoad;
        
        _immoter = new SharedJDBCImmoter();
        _emoter = new AbstractChainedEmoter();
    }

    public void start() throws Exception {
        if (clean) {
            _store.clean();
        }
        super.start();
    }

    public Immoter getImmoter() {
        return _immoter;
    }

    public Emoter getEmoter() {
        return _emoter;
    }

    public Immoter getDemoter(String name, Motable motable) {
        // TODO - should check _next... - just remove when we have an evicter sorted
        return new SharedJDBCImmoter();
    }

    protected Motable get(String id, boolean exclusiveOnly) {
        throw new UnsupportedOperationException();
    }

    /**
     * An Emoter that deals in terms of SharedJDBCMotables
     * 
     * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
     * @version $Revision$
     */
    public class SharedJDBCImmoter extends AbstractImmoter {

        public Motable newMotable() {
            StoreMotable motable = _store.create();
            motable.init(_store);
            return motable;
        }

    }

    class SharedPutter implements Store.Putter {

        protected final Emoter _emoter;

        protected final Immoter _immoter;

        public SharedPutter(Emoter emoter, Immoter immoter) {
            _emoter = emoter;
            _immoter = immoter;
        }

        public void put(String name, Motable motable) {
            Utils.mote(_emoter, _immoter, motable, name);
        }
    }

    protected void load(Emoter emoter, Immoter immoter) {
        // this should only happen when we are the first node in the cluster...
        _store.load(new SharedPutter(emoter, immoter), accessOnLoad);
    }

    public Emoter getEvictionEmoter() {
        // FIXME
        throw new UnsupportedOperationException();
    }

    public void expire(Motable motable) {
        // FIXME
        throw new UnsupportedOperationException();
    }

    /**
     * Shared Contextualisers do nothing at runtime. They exist only to load
     * data at startup and store it at shutdown.
     */
    public boolean contextualise(Invocation invocation, String key, Immoter immoter, Sync invocationLock,
            boolean exclusiveOnly) throws InvocationException {
        return false;
    }

}
