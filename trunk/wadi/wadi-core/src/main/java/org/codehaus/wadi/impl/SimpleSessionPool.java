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

import org.codehaus.wadi.WebSession;
import org.codehaus.wadi.SessionConfig;
import org.codehaus.wadi.SessionFactory;
import org.codehaus.wadi.SessionPool;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class SimpleSessionPool implements SessionPool {

    protected final SessionFactory _factory;
    protected SessionConfig _config;

    public SimpleSessionPool(SessionFactory factory) {
        super();
        _factory=factory;
    }

    public void init(SessionConfig config) {
      assert (_config==null);
        _config=config;
    }

    public void destroy() {
      assert (_config!=null);
        _config=null;
    }

    public WebSession take() {
        return _factory.create(_config);
    }

    public void put(WebSession session) {
        // just drop the session - truly pool later
    }

}
