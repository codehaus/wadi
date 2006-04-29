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

import java.lang.reflect.Method;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Message;

/**
 * 
 * @version $Revision: 1603 $
 */
class MsgAndPayloadMsgDispatcher extends AbstractMsgMethodDispatcher {
    private final ThreadLocal _pair = new ThreadLocal() {
        protected Object initialValue() {
            return new Object[2];
        }
    };

    public MsgAndPayloadMsgDispatcher(Dispatcher dispatcher, Object target, Method method, Class type) {
        super(dispatcher, target, method, type);
    }

    public void dispatch(Message om) throws Exception {
        Object[] pair = (Object[]) _pair.get();
        pair[0] = om;
        pair[1] = om.getPayload();
        invoke(pair);
    }

    public String toString() {
        return "<MsgAndPayloadMsgDispatcher: " + _method + " dispatched on: " + _target + ">";
    }
}