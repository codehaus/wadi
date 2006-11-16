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
package org.codehaus.wadi.impl;

import java.io.File;
import java.util.Map;

import org.codehaus.wadi.Collapser;
import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.ContextualiserConfig;
import org.codehaus.wadi.DistributableContextualiserConfig;
import org.codehaus.wadi.Emoter;
import org.codehaus.wadi.Evicter;
import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.Motable;
import org.codehaus.wadi.Store;
import org.codehaus.wadi.StoreMotable;
import org.codehaus.wadi.Streamer;

// TODO - a JDBC-based equivalent

/**
 * Maps id:File where file contains Context content...
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class ExclusiveStoreContextualiser extends AbstractExclusiveContextualiser {

    protected final Store _store;
    protected final Immoter _immoter;
	protected final Emoter _emoter;

	public ExclusiveStoreContextualiser(Contextualiser next, Collapser collapser, boolean clean, Evicter evicter, Map map, Streamer streamer, File dir) throws Exception {
	    super(next, new CollapsingLocker(collapser), clean, evicter, map);
        _store=new DiscStore(streamer, dir, true, false);
	    _immoter=new ExclusiveStoreImmoter(_map);
	    _emoter=new ExclusiveStoreEmoter(_map);
	}

    public void init(ContextualiserConfig config) {
        super.init(config);
        // perhaps this should be done in start() ?
        if (_clean)
            _store.clean();
    }

    public String getStartInfo() {
        return "["+_store.getStartInfo()+"]";
    }

	public boolean isExclusive(){return true;}

	public Immoter getImmoter(){return _immoter;}
	public Emoter getEmoter(){return _emoter;}

	/**
	 * An Immoter that deals in terms of StoreMotables
	 */
	public class ExclusiveStoreImmoter extends AbstractMappedImmoter {

	    public ExclusiveStoreImmoter(Map map) {
	        super(map);
	    }

		public Motable nextMotable(String name, Motable emotable) {
			StoreMotable motable=_store.create();
			motable.init(_store);
            return motable; // TODO - Pool, maybe as ThreadLocal
		}

		public String getInfo() {
			return _store.getDescription();
		}
	}

	/**
	 * An Emmoter that deals in terms of StoreMotables
	 *
	 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
	 * @version $Revision$
	 */
	class ExclusiveStoreEmoter extends AbstractMappedEmoter {

		public ExclusiveStoreEmoter(Map map) {super(map);}

		public String getInfo(){return _store.getDescription();}
	}

    public void start() throws Exception {
        Store.Putter putter=new Store.Putter() {
            public void put(String name, Motable motable) {
                _map.put(name, motable);
            }
        };
        _store.load(putter, ((DistributableContextualiserConfig)_config).getAccessOnLoad());
        super.start(); // continue down chain...
    }

    // this should move up.....
    public void expire(Motable motable) {
        // decide whether session needs promotion
        boolean needsLoading=true; // FIXME
        // if so promote to top and expire there
        String id=motable.getName();
        if (_log.isTraceEnabled()) _log.trace("expiring from disc: "+id);
        if (needsLoading) {
            _map.remove(id);
            Motable loaded=_config.getSessionPool().take();
            try {
                loaded.copy(motable);
                motable=null;
                _config.expire(loaded);
            } catch (Exception e) {
	      _log.error("unexpected problem expiring from disc", e);
            }
            loaded=null;
        } else {
            // else, just drop it off the disc here...
            throw new UnsupportedOperationException(); // FIXME
        }
    }

}
