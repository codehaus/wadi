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
import org.codehaus.wadi.core.manager.SessionMonitor;
import org.codehaus.wadi.core.motable.AbstractMappedImmoter;
import org.codehaus.wadi.core.motable.BaseMappedEmoter;
import org.codehaus.wadi.core.motable.Emoter;
import org.codehaus.wadi.core.motable.Immoter;
import org.codehaus.wadi.core.motable.Motable;
import org.codehaus.wadi.core.session.Session;
import org.codehaus.wadi.core.session.SessionFactory;

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
    private final SessionMonitor sessionMonitor;

	public MemoryContextualiser(Contextualiser next,
            Evicter evicter,
            ConcurrentMotableMap map,
            SessionFactory sessionFactory,
            SessionMonitor sessionMonitor) {
		super(next, evicter, map);
        if (null == sessionFactory) {
            throw new IllegalArgumentException("sessionFactory is required");
        } else if (null == sessionMonitor) {
            throw new IllegalArgumentException("sessionMonitor is required");
        }
        this.sessionFactory = sessionFactory;
        this.sessionMonitor = sessionMonitor;
        
        _immoter = new MemoryImmoter(map);
        _emoter = newEmoter(map);
    }

    protected MemoryEmoter newEmoter(ConcurrentMotableMap map) {
        return new MemoryEmoter(map);
    }

    protected boolean handleLocally(Invocation invocation, Object id, Motable motable) throws InvocationException {
        motable.setLastAccessedTime(System.currentTimeMillis());
        invocation.setSession((Session) motable); 
        if (invocation.isProxiedInvocation()) {
            invocation.invoke();
        } else {
            InvocationContext context = invocation.newContext((Session) motable);
            invocation.invoke(context);
        }
        return true;
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

    protected class MemoryImmoter extends AbstractMappedImmoter {

        public MemoryImmoter(ConcurrentMotableMap map) {
            super(map);
        }

        public Motable newMotable(Motable emotable) {
            return sessionFactory.create();
        }

        public boolean immote(Motable emotable, Motable immotable) {
            boolean success = super.immote(emotable, immotable);
            if (success) {
                sessionMonitor.notifyInboundSessionMigration((Session) immotable);
            }
            return success;
        }
        
        public boolean contextualise(Invocation invocation, Object id, Motable immotable)
                throws InvocationException {
            return handleLocally(invocation, id, immotable);
        }

    }

    protected class MemoryEmoter extends BaseMappedEmoter {

        public MemoryEmoter(ConcurrentMotableMap map) {
            super(map);
        }
        
        public boolean emote(Motable emotable, Motable immotable) {
            boolean success = super.emote(emotable, immotable);
            if (success) {
                sessionMonitor.notifyOutboundSessionMigration((Session) emotable);
            }
            return success;
        }
        
    }
}
