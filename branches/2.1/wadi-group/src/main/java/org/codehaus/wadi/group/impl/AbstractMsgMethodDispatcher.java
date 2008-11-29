/**
 * Copyright 2006 The Apache Software Foundation
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
package org.codehaus.wadi.group.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.codehaus.wadi.group.Dispatcher;

/**
 * 
 * @version $Revision: 1603 $
 */
abstract class AbstractMsgMethodDispatcher extends AbstractMsgDispatcher {
    protected final Object _target;
    protected final Method _method;

    public AbstractMsgMethodDispatcher(Dispatcher dispatcher, Object target, Method method, Class type) {
        super(dispatcher, type);
        _target = target;
        _method = method;
    }
    
    protected Object invoke(Object[] args) throws Exception {
        try {
            return _method.invoke(_target, args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Runnable) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Exception) {
                throw (Exception) cause;
            } else {
                throw e;
            }
        }
    }
}