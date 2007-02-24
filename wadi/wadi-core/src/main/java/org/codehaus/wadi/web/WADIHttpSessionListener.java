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
package org.codehaus.wadi.web;

import java.util.ArrayList;
import java.util.Iterator;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.codehaus.wadi.core.manager.NoOpSessionListener;
import org.codehaus.wadi.core.session.Session;

/**
 * 
 * @version $Revision: 1538 $
 */
public class WADIHttpSessionListener extends NoOpSessionListener {
    protected HttpSessionListener[] listeners;
    
    public WADIHttpSessionListener(HttpSessionListener[] listeners) {
        if (null == listeners) {
            throw new IllegalArgumentException("listeners is required");
        }
        this.listeners = listeners;
    }

    public void onSessionCreation(Session session) {
        WADIHttpSession httpSession = ensureTypeAndCast(session);
        int l = listeners.length;
        HttpSessionEvent hse = httpSession.getHttpSessionEvent();
        for (int i = 0; i < l; i++) {
            listeners[i].sessionCreated(hse);
        }
    }

    public void onSessionDestruction(Session session) {
        WADIHttpSession httpSession = ensureTypeAndCast(session);
        for (Iterator i = new ArrayList(httpSession.getAttributeNameSet()).iterator(); i.hasNext();) {
            httpSession.removeAttribute((String) i.next());
        }
        int l = listeners.length;
        HttpSessionEvent hse = httpSession.getHttpSessionEvent();
        for (int i = 0; i < l; i++) {
            listeners[i].sessionDestroyed(hse);
        }
    }

    protected WADIHttpSession ensureTypeAndCast(Session session) {
        if (!(session instanceof WADIHttpSession)) {
            throw new IllegalArgumentException(WADIHttpSession.class + " expected. Was ["
                    + session.getClass().getName() + "]");
        }
        WADIHttpSession httpSession = (WADIHttpSession) session;
        return httpSession;
    }

}
