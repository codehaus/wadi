/**
 * Copyright 2006 The Apache Software Foundation
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
package org.codehaus.wadi.core.manager;

import java.util.Iterator;

import org.codehaus.wadi.core.session.Session;

import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArrayList;


/**
 * 
 * @version $Revision: 1538 $
 */
public class BasicSessionMonitor implements SessionMonitor {
    private final CopyOnWriteArrayList sessionListeners;
    
    public BasicSessionMonitor() {
        sessionListeners = new CopyOnWriteArrayList();
    }
    
    public void addSessionListener(SessionListener sessionListener) {
        sessionListeners.add(sessionListener);
    }
    
    public void removeSessionListener(SessionListener sessionListener) {
        sessionListeners.remove(sessionListener);
    }
    
    public void notifyInboundSessionMigration(Session session) {
        for (Iterator iter = sessionListeners.iterator(); iter.hasNext();) {
            SessionListener listener = (SessionListener) iter.next();
            listener.onInboundSessionMigration(session);
        }
    }
    
    public void notifyOutbountSessionMigration(Session session) {
        for (Iterator iter = sessionListeners.iterator(); iter.hasNext();) {
            SessionListener listener = (SessionListener) iter.next();
            listener.onOutbountSessionMigration(session);
        }
    }

    public void notifySessionCreation(Session session) {
        for (Iterator iter = sessionListeners.iterator(); iter.hasNext();) {
            SessionListener listener = (SessionListener) iter.next();
            listener.onSessionCreation(session);
        }
    }

    public void notifySessionDestruction(Session session) {
        for (Iterator iter = sessionListeners.iterator(); iter.hasNext();) {
            SessionListener listener = (SessionListener) iter.next();
            listener.onSessionDestruction(session);
        }
    }
    
}
