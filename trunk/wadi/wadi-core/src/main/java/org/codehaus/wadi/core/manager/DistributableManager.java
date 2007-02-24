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
package org.codehaus.wadi.core.manager;

import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.Router;
import org.codehaus.wadi.SessionFactory;
import org.codehaus.wadi.SessionIdFactory;
import org.codehaus.wadi.SessionMonitor;
import org.codehaus.wadi.core.ConcurrentMotableMap;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * A StandardManager that knows how to Serialise HttpSessions.
 */
public class DistributableManager extends StandardManager {

    public DistributableManager(SessionFactory sessionFactory,
            SessionIdFactory sessionIdFactory,
            Contextualiser contextualiser,
            ConcurrentMotableMap map,
            Router router,
            SessionMonitor sessionMonitor,
            boolean errorIfSessionNotAcquired) {
        super(sessionFactory,
                sessionIdFactory,
                contextualiser,
                map,
                router,
                sessionMonitor,
                errorIfSessionNotAcquired);
    }

}
