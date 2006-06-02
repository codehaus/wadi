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
package org.codehaus.wadi.impl;

import org.codehaus.wadi.Session;
import org.codehaus.wadi.ContextPool;
import org.codehaus.wadi.web.WebSession;
import org.codehaus.wadi.web.WebSessionConfig;
import org.codehaus.wadi.web.WebSessionPool;


/**
 * Hack - plasters over a difference in the API between the Manager and Contextualiser
 * stacks. Sessions appear in both stacks, and must be pooled by the same object, but with
 * different APIs. To be resolved ASAP.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class SessionToContextPoolAdapter implements ContextPool {

    protected final WebSessionPool _pool;
    
    public SessionToContextPoolAdapter(WebSessionPool pool) {
        super();
        _pool=pool;
    }
    
    public void init(WebSessionConfig config) {
        _pool.init(config);
    }

    public void put(Session context) {
        _pool.put((WebSession)context);
    }

    public Session take() {
        return _pool.take();
    }
}
