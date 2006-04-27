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

import org.codehaus.wadi.Collapser;
import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.ContextualiserConfig;
import org.codehaus.wadi.DistributableContextualiserConfig;
import org.codehaus.wadi.Emoter;
import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.Invocation;
import org.codehaus.wadi.InvocationException;
import org.codehaus.wadi.Motable;
import org.codehaus.wadi.Store;
import org.codehaus.wadi.StoreMotable;

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

	protected final DatabaseStore _store;
	protected final Immoter _immoter;
	protected final Emoter _emoter;

	public SharedStoreContextualiser(Contextualiser next, Collapser collapser, boolean clean, DatabaseStore store) {
		super(next, new CollapsingLocker(collapser), clean);
		_store=store;
		_immoter=new SharedJDBCImmoter();
		_emoter=new SharedJDBCEmoter();
	}

	public String getStartInfo() {
		return "["+_store.getLabel()+"/"+_store.getTable()+"]";
	}


	public void init(ContextualiserConfig config) {
		super.init(config);
		if (_clean)
			_store.clean();
	}

	public Immoter getImmoter(){return _immoter;}
	public Emoter getEmoter(){return _emoter;}

	public Immoter getDemoter(String name, Motable motable) {
		// TODO - should check _next... - just remove when we have an evicter sorted
		return new SharedJDBCImmoter();
	}

	public Motable get(String id) {
		throw new UnsupportedOperationException();
	}

	/**
	 * An Emoter that deals in terms of SharedJDBCMotables
	 *
	 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
	 * @version $Revision$
	 */
	public class SharedJDBCImmoter extends AbstractImmoter {

		public Motable nextMotable(String name, Motable emotable) {
			StoreMotable motable=_store.create();
			motable.init(_store);
			return motable; // TODO - Pool, maybe as ThreadLocal
		}

		public String getInfo() {
			return _store.getDescription();
		}
	}

	public class SharedJDBCEmoter extends AbstractChainedEmoter {

		public String getInfo() {
			return _store.getDescription();
		}
	}

	class SharedPutter implements Store.Putter {

		protected final Emoter _emoter;
		protected final Immoter _immoter;

		public SharedPutter(Emoter emoter, Immoter immoter) {
			_emoter=emoter;
			_immoter=immoter;
		}

		public void put(String name, Motable motable) {
			Utils.mote(_emoter, _immoter, motable, name);
		}
	}

	public void load(Emoter emoter, Immoter immoter) {
		// this should only happen when we are the first node in the cluster...
		_store.load(new SharedPutter(emoter, immoter), ((DistributableContextualiserConfig)_config).getAccessOnLoad());
	}

	public Emoter getEvictionEmoter() {throw new UnsupportedOperationException();} // FIXME
	public void expire(Motable motable) {throw new UnsupportedOperationException();} // FIXME

	/**
	 * Shared Contextualisers do nothing at runtime. They exist only to load data at startup and store it at shutdown.
	 */
	public boolean contextualise(Invocation invocation, String id, Immoter immoter, Sync motionLock, boolean exclusiveOnly) throws InvocationException {
		return false;
	}

}
