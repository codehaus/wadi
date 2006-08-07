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
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Message;
import org.codehaus.wadi.group.ServiceEndpoint;

/**
 * 
 * @version $Revision: 1603 $
 */
public abstract class AbstractMsgDispatcher implements ServiceEndpoint {
    protected final Dispatcher _dispatcher;
    private final Class _type;

    public AbstractMsgDispatcher(Dispatcher dispatcher, Class type) {
        _dispatcher = dispatcher;
        _type = type;
    }

    public boolean testDispatchMessage(Message om) {
        Serializable payload = om.getPayload();
        if (null == payload) {
            return false;
        }
        return canAcceptClass(payload.getClass());
    }

    public void dispose(int nbAttemp, long delayMillis) {
        _dispatcher.unregister(this, nbAttemp, delayMillis);
    }
    
    private boolean canAcceptClass(Class clazz) {
        do {
            if (clazz == _type) {
                return true;    
            }
            Class[] interfaces = clazz.getInterfaces();
            for (int i = 0; i < interfaces.length; i++) {
                if (interfaces[i] == _type) {
                    return true;
                }
            }
        } while (null != (clazz = clazz.getSuperclass()));

        return false;
    }
}