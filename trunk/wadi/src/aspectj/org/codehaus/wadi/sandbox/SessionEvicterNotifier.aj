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
package org.codehaus.wadi.sandbox;

import org.codehaus.wadi.sandbox.Session;
import org.codehaus.wadi.sandbox.impl.Manager;

public aspect SessionEvicterNotifier {
    
    pointcut setLastAccessedTime(Session session, long newTime) : execution(void Session+.setLastAccessedTime(long)) && args(newTime) && target(session);
    
    void around(Session session, long newTime) : setLastAccessedTime(session, newTime) {
        long oldTime=session.getLastAccessedTime();
        proceed(session, newTime);
        session.getConfig().setLastAccessedTime(session, oldTime, newTime);
    }
    
    pointcut setMaxInactiveInterval(Session session, int newInterval) : execution(void Session+.setMaxInactiveInterval(int)) && args(newInterval) && target(session);
    
    void around(Session session, int newInterval) : setMaxInactiveInterval(session, newInterval) {
        int oldInterval=session.getMaxInactiveInterval();
        proceed(session, newInterval);
        session.getConfig().setMaxInactiveInterval(session, oldInterval, newInterval);
    }
    
    pointcut createSession(Manager manager) : execution(Session Manager+.createSession()) && target(manager);
    
    Session around(Manager manager) : createSession(manager) {
        Session session=(Session)proceed(manager);
        // notify Evicter of insertion
        return session;
    }
    
    pointcut destroySession(Manager manager, Session session) : execution(void Manager+.destroySession(Session)) && args(session) && target(manager);
    
    before(Manager manager, Session session) : destroySession(manager, session) {
        // notify Evicter of removal
    }
}
