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

import org.codehaus.wadi.core.ConcurrentMotableMap;
import org.codehaus.wadi.core.eviction.Evicter;
import org.codehaus.wadi.core.motable.AbstractMappedImmoter;
import org.codehaus.wadi.core.motable.BaseMappedEmoter;
import org.codehaus.wadi.core.motable.Emoter;
import org.codehaus.wadi.core.motable.Immoter;
import org.codehaus.wadi.core.motable.Motable;
import org.codehaus.wadi.core.store.Store;

/**
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class ExclusiveStoreContextualiser extends AbstractExclusiveContextualiser {
    private final boolean clean;
    private final Store store;
    private final Immoter immoter;
    private final Emoter emoter;

	public ExclusiveStoreContextualiser(Contextualiser next,
            boolean clean,
            Evicter evicter,
            ConcurrentMotableMap map,
            Store store) throws Exception {
	    super(next, evicter, map);
        this.clean = clean;
        this.store = store;
        
        immoter = new ExclusiveStoreImmoter(map);
        emoter = new BaseMappedEmoter(map);
	}

	@Override
    public void promoteToExclusive(Immoter immoter) {
        next.promoteToExclusive(immoter);
    }
	
	public Immoter getImmoter() {
        return immoter;
    }

    public Emoter getEmoter() {
        return emoter;
    }

    protected void doStart() throws Exception {
        super.doStart();

        if (clean) {
            store.clean();
        } else {
            Store.Putter putter = new Store.Putter() {
                public void put(String name, Motable motable) {
                    map.put(name, motable);
                }
            };
            store.load(putter);
        }
    }
    
    /**
     * An Immoter that deals in terms of StoreMotables
     */
    public class ExclusiveStoreImmoter extends AbstractMappedImmoter {

        public ExclusiveStoreImmoter(ConcurrentMotableMap map) {
            super(map);
        }

        public Motable newMotable(Motable emotable) {
            return store.create();
        }

    }

}
