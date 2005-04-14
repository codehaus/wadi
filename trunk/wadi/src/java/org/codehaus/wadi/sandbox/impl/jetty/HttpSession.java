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
package org.codehaus.wadi.sandbox.impl.jetty;

import org.codehaus.wadi.sandbox.Session;
import org.codehaus.wadi.sandbox.impl.SessionWrapper;
import org.mortbay.jetty.servlet.SessionManager;

/**
 * A SessionWrapper that integrates correctly with Jetty.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class HttpSession extends SessionWrapper implements SessionManager.Session {

    HttpSession(Session session) {super(session);}
    
    public boolean isValid() {
        // NYI
        return false;
    }
    
    public void access() {
        // TODO - this will do it - but not sure that we want to ?
        _session.setLastAccessedTime(System.currentTimeMillis());
    }

}
