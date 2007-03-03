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

import java.io.File;

import org.codehaus.wadi.core.ConcurrentMotableMap;
import org.codehaus.wadi.core.eviction.Evicter;
import org.codehaus.wadi.core.motable.AbstractMappedImmoter;
import org.codehaus.wadi.core.motable.BaseMappedEmoter;
import org.codehaus.wadi.core.motable.Emoter;
import org.codehaus.wadi.core.motable.Immoter;
import org.codehaus.wadi.core.motable.Motable;
import org.codehaus.wadi.core.store.DiscStore;
import org.codehaus.wadi.core.store.Store;
import org.codehaus.wadi.core.store.StoreMotable;
import org.codehaus.wadi.core.util.Streamer;

/**
 * Maps id:File where file contains Context content...
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class ExclusiveStoreContextualiser extends AbstractExclusiveContextualiser {
    private final boolean clean;
    private final boolean accessOnLoad;
    protected final Store _store;
    protected final Immoter _immoter;
	protected final Emoter _emoter;

	public ExclusiveStoreContextualiser(Contextualiser next,
            boolean clean,
            Evicter evicter,
            ConcurrentMotableMap map,
            Streamer streamer,
            File dir,
            boolean accessOnLoad) throws Exception {
	    super(next, evicter, map);
        this.clean = clean;
        this.accessOnLoad = accessOnLoad;
        
        _store = new DiscStore(streamer, dir, true, false);
        _immoter = new ExclusiveStoreImmoter(map);
        _emoter = new BaseMappedEmoter(map);
	}

	public Immoter getImmoter() {
        return _immoter;
    }

    public Emoter getEmoter() {
        return _emoter;
    }

    public void start() throws Exception {
        if (clean) {
            _store.clean();
        } else {
            Store.Putter putter = new Store.Putter() {
                public void put(String name, Motable motable) {
                    map.put(name, motable);
                }
            };
            _store.load(putter, accessOnLoad);
        }
        super.start();
    }
    
    /**
     * An Immoter that deals in terms of StoreMotables
     */
    public class ExclusiveStoreImmoter extends AbstractMappedImmoter {

        public ExclusiveStoreImmoter(ConcurrentMotableMap map) {
            super(map);
        }

        public Motable newMotable(Motable emotable) {
            StoreMotable motable = _store.create();
            motable.init(_store);
            return motable;
        }

    }

}
