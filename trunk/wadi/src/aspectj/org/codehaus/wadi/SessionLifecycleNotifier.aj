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
package org.codehaus.wadi;

import java.util.List;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.codehaus.wadi.impl.Manager;

// TODO - do we really want to use aspects for all notifications ?

public aspect SessionLifecycleNotifier {
    
    pointcut create(Manager manager) : execution(Session Manager+.create()) && target(manager);
    
    Session around(Manager manager) : create(manager) {
        Session session=(Session)proceed(manager);
        List l=manager.getSessionListeners();
        int s=l.size();
        HttpSessionEvent hse=session.getHttpSessionEvent();
        for (int i=0; i<s; i++) {
            HttpSessionListener hsl=(HttpSessionListener)l.get(i);
            hsl.sessionCreated(hse);
        }
        return session;
    }
    
    pointcut destroy(Manager manager, Session session) : execution(void Manager+.destroy(Session)) && args(session) && target(manager);
    
    void around(Manager manager, Session session) : destroy(manager, session) {
        proceed(manager, session);
        List l=manager.getSessionListeners();
        int s=l.size();
        HttpSessionEvent hse=session.getHttpSessionEvent();
        for (int i=0; i<s; i++) {
            HttpSessionListener hsl=(HttpSessionListener)l.get(i);
            hsl.sessionDestroyed(hse); // actually - about-to-be-destroyed - hasn't happened yet - see SRV.15.1.14.1
        }
    }
}
