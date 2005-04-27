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

public aspect SessionEvicterNotifier {

    pointcut setLastAccessedTime(Session session, long time) : execution(void Session+.setLastAccessedTime(long)) && args(time) && target(session);
    
    after(Session session, long time) : setLastAccessedTime(session, time) {
      session.getConfig().setLastAccessedTime(session, time);
    }

    pointcut setMaxInactiveInterval(Session session, int interval) : execution(void Session+.setMaxInactiveInterval(int)) && args(interval) && target(session);
    
    after(Session session, int interval) : setMaxInactiveInterval(session, interval) {
      session.getConfig().setMaxInactiveInterval(session, interval);
    }

}
