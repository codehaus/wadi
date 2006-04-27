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
package org.codehaus.wadi.axis2;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Invocation;
import org.codehaus.wadi.InvocationException;
import org.codehaus.wadi.PoolableInvocationWrapper;

public class Axis2Invocation implements Invocation {
    
    protected final static Log _log=LogFactory.getLog(Invocation.class);
    protected final static ThreadLocal _threadLocalInstance=new ThreadLocal() {protected Object initialValue() {return new Axis2Invocation();}};

    public static Axis2Invocation getThreadLocalInstance() {
        return (Axis2Invocation)_threadLocalInstance.get();
    }
    
    protected String _key;
    
    public void init(String key) {
        _key=key;
    }
    
    public void clear() {
        _key=null;
    }
    
    public String getKey() {
        return _key;
    }
    
    public void sendError(int code, String message) {
        _log.error(code+" : "+message);
    }
    
    public boolean getRelocatable() {
        return false;
    }
    
    // Invocation
    
    public void invoke(PoolableInvocationWrapper wrapper) throws InvocationException {
        throw new UnsupportedOperationException("NYI");
    }

    public void invoke() throws InvocationException {
        throw new UnsupportedOperationException("NYI");
    }

    public boolean isProxiedInvocation() {
        throw new UnsupportedOperationException("NYI");
    }
}
