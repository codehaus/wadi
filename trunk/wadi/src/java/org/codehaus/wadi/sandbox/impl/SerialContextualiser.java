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
package org.codehaus.wadi.sandbox.impl;

import java.io.IOException;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.sandbox.Collapser;
import org.codehaus.wadi.sandbox.Context;
import org.codehaus.wadi.sandbox.Contextualiser;
import org.codehaus.wadi.sandbox.Immoter;

import EDU.oswego.cs.dl.util.concurrent.NullSync;
import EDU.oswego.cs.dl.util.concurrent.Sync;
import EDU.oswego.cs.dl.util.concurrent.TimeoutException;

/**
 * Ensure that any Contextualisations that pass through are serialised according to the strategy imposed by our Collapser.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class SerialContextualiser extends AbstractThinContextualiser {
    protected static final Log _log = LogFactory.getLog(AbstractImmoter.class);
    
    protected final Collapser _collapser;
    protected final Sync _dummyLock=new NullSync();
    protected final Map _map;
    
    public SerialContextualiser(Contextualiser next, Collapser collapser, Map map) {
        super(next);
        _collapser=collapser;
        _map=map;
    }
    
    public boolean contextualise(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Immoter immoter, Sync promotionLock, boolean localOnly) throws IOException, ServletException {
        promotionLock=_collapser.getLock(id);
        boolean promotionAcquired=false;
        Sync sharedLock=null;
        boolean sharedAcquired=false;
        try {
            try {
                Utils.acquireUninterrupted(promotionLock);
                promotionAcquired=true;
            } catch (TimeoutException e) {
                _log.error("unexpected timeout - proceding without lock", e);
            }
            
            // whilst we were waiting for the promotionLock, the session in question may have been promoted into memory.
            // before we proceed, confirm that this has not happened.
            Context context=(Context)_map.get(id);
            if (null!=context) {
                // oops - it HAS happened...
                _log.debug("session was promoted whilst we were waiting: "+id); // TODO - downgrade..
                // overlap two locking systems until we have secured the session in memory, then run the request
                // and release the lock.
                sharedLock=context.getSharedLock();
                try {
                    Utils.acquireUninterrupted(sharedLock);
                    sharedAcquired=true;
                } catch (TimeoutException e) {
                    _log.error("unexpected timeout - proceding without lock", e);
                }
                     
                promotionLock.release(); // release as soon as we know Context is available to other threads
                promotionAcquired=false;
                // TODO - we really need to take a read lock before we release the promotionLock...
                immoter.contextualise(hreq, hres, chain, id, context);
                return true;
            } else {
                // session was not promoted whilst we were waiting for promotionLock. Continue down Contextualiser stack
                // it may be below us...
                // lock is to be released as soon as context is available to subsequent contextualisations...
                return (promotionAcquired=!_next.contextualise(hreq, hres, chain, id, immoter, promotionAcquired?promotionLock:_dummyLock, localOnly));
            }
        } finally {
            if (promotionAcquired) promotionLock.release();
            if (sharedAcquired) sharedLock.release();
        }
    }
}
