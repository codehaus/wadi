/**
 *
 * Copyright 2003-2006 Core Developers Network Ltd.
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
package org.codehaus.wadi.test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Invocation;
import org.codehaus.wadi.InvocationException;
import org.codehaus.wadi.PoolableInvocationWrapper;

public class DummyInvocation implements Invocation {
    
    protected final Log _log=LogFactory.getLog(getClass());
    
    protected String _key;
    
    public void init(String key) {
        _key=key;
    }
    
    // Invocation
    
    public void clear() {
        _key=null;
    }

    public String getKey() {
        return _key;
    }

    public void sendError(int code, String message) throws InvocationException {
        throw new UnsupportedOperationException("NYI");
    }

    public boolean getRelocatable() {
        return false;
    }

    public void invoke(PoolableInvocationWrapper wrapper) throws InvocationException {
        _log.info("invoke(PoolableInvocationWrapper)");
    }

    public void invoke() throws InvocationException {
        _log.info("invoke()");
        }

    public boolean isProxiedInvocation() {
        return false;
    }
    
}
