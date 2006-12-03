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
package org.codehaus.wadi.jetty5;

import org.codehaus.wadi.web.WebSession;
import org.codehaus.wadi.web.impl.WebSessionWrapper;
import org.mortbay.jetty.servlet.SessionManager;

/**
 * A SessionWrapper that integrates correctly with Jetty.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class HttpSession extends WebSessionWrapper implements SessionManager.Session {

    HttpSession(WebSession session) {super(session);}
    
    public boolean isValid() {
        return _session.getName()!=null;
    }
    
    public void access() {
        // used by Jetty to update a Session's lastAccessedTime on each request.
        // we want to do this ourselves.
        
        // ignore
    }

}
