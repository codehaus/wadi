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

import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.Emoter;
import org.codehaus.wadi.Evicter;
import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.Invocation;
import org.codehaus.wadi.InvocationException;
import org.codehaus.wadi.Motable;
import org.codehaus.wadi.PoolableInvocationWrapper;
import org.codehaus.wadi.PoolableInvocationWrapperPool;
import org.codehaus.wadi.Session;
import org.codehaus.wadi.SessionFactory;
import org.codehaus.wadi.core.ConcurrentMotableMap;

/**
 * A Contextualiser that stores its state in Memory as Java Objects
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class MemoryContextualiser extends AbstractExclusiveContextualiser {
	private final SessionFactory sessionFactory;
    private final Immoter _immoter;
    private final Emoter _emoter;
    private final Emoter _evictionEmoter;
    private final PoolableInvocationWrapperPool _requestPool;

	public MemoryContextualiser(Contextualiser next,
            Evicter evicter,
            ConcurrentMotableMap map,
            SessionFactory sessionFactory,
            PoolableInvocationWrapperPool requestPool) {
		super(next, evicter, map);
        if (null == sessionFactory) {
            throw new IllegalArgumentException("sessionFactory is required");
        }
        this.sessionFactory = sessionFactory;
        _requestPool = requestPool;
        
        _immoter = new MemoryImmoter(map);
        _emoter = new BaseMappedEmoter(map);
        _evictionEmoter = _emoter;
    }

    protected boolean handleLocally(Invocation invocation, String id, Motable motable) throws InvocationException {
        motable.setLastAccessedTime(System.currentTimeMillis());
        // we need a solution - MemoryContextualiser needs to separate Contexts and Motables cleanly...
        invocation.setSession((Session) motable); 
        if (!invocation.isProxiedInvocation()) {
            // take wrapper from pool...
            PoolableInvocationWrapper wrapper = _requestPool.take();
            wrapper.init(invocation, (Session) motable);
            invocation.invoke(wrapper);
            wrapper.destroy();
            _requestPool.put(wrapper);
        } else {
            invocation.invoke();
        }
        return true;
    }

    class MemoryImmoter extends AbstractMappedImmoter {

        public MemoryImmoter(ConcurrentMotableMap map) {
            super(map);
        }

        public Motable newMotable() {
            return sessionFactory.create();
        }

        public boolean contextualise(Invocation invocation, String id, Motable immotable)
                throws InvocationException {
            return handleLocally(invocation, id, immotable);
        }

    }

    public Immoter getImmoter() {
        return _immoter;
    }

    public Emoter getEmoter() {
        return _emoter;
    }

    public Immoter getPromoter(Immoter immoter) {
        return immoter == null ? _immoter : immoter;
    }

    public Emoter getEvictionEmoter() {
        return _evictionEmoter;
    }

}
