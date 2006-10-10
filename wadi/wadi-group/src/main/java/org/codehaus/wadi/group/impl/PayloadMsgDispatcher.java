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

import java.io.Serializable;
import java.lang.reflect.Method;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Envelope;

/**
 * 
 * @version $Revision: 1603 $
 */
class PayloadMsgDispatcher extends AbstractMsgMethodDispatcher {
    private final ThreadLocal _singleton = new ThreadLocal() {
        protected Object initialValue() {
            return new Object[1];
        }
    };

    public PayloadMsgDispatcher(Dispatcher dispatcher, Object target, Method method, Class type) {
        super(dispatcher, target, method, type);
    }

    public void dispatch(Envelope om) throws Exception {
        Object[] singleton = (Object[]) _singleton.get();
        singleton[0] = om.getPayload();
        Object response = invoke(singleton);
        _dispatcher.reply(om, (Serializable) response);
    }

    public String toString() {
        return "<PayloadMsgDispatcher: " + _method + " dispatched on: " + _target + ">";
    }
}