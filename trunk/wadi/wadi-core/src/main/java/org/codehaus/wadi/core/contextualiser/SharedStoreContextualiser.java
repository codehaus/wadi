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
import org.codehaus.wadi.core.store.Store;
import org.codehaus.wadi.core.util.Utils;

/**
 * A Contextualiser which stores its Contexts in a shared database via JDBC.
 * On shutdown of the cluster's last node, all extant sessions will be demoted to here.
 * On startup of the cluster's first node, all sessions stored here will be promoted upwards.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class SharedStoreContextualiser extends AbstractSharedContextualiser {
    private final boolean cleanOnStart;
    private final Store store;
    private final Immoter immoter;
    private final Emoter emoter;

	public SharedStoreContextualiser(Contextualiser next,
            boolean cleanOnStart,
            Store store,
            boolean accessOnLoad) {
		super(next);
        this.cleanOnStart = cleanOnStart;
        this.store = store;
        
        immoter = new SharedImmoter();
        emoter = new AbstractChainedEmoter();
    }

    public void start() throws Exception {
        if (cleanOnStart) {
            store.clean();
        }
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
        // TODO - load only if we are the first.
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
            Utils.mote(emoter, immoter, motable, name);
        }
    }

}
