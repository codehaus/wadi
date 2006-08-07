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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Message;
import org.codehaus.wadi.group.ServiceEndpoint;

/**
 * 
 * @version $Revision: 1603 $
 */
public class ServiceEndpointBuilder {
    private final Collection builtDispatchers = new ArrayList();
    
    public void addSEI(Dispatcher dispatcher, Class[] clazzes, Object target) {
        for (int i = 0; i < clazzes.length; i++) {
            Class clazz = clazzes[i];
            addSEI(dispatcher, clazz, target);
        }
    }

    public void addSEI(Dispatcher dispatcher, Class clazz, Object target) {
        Method[] methods = clazz.getDeclaredMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            buildRPC(dispatcher, method, target);
        }
    }

    public void addCallback(Dispatcher dispatcher, Class type) {
        ServiceEndpoint msgDispatcher = new RendezVousMsgDispatcher(dispatcher, type);
        builtDispatchers.add(msgDispatcher);
        dispatcher.register(msgDispatcher);
    }

    public void dispose(int nbAttemp, long delayMillis) {
        for (Iterator iter = builtDispatchers.iterator(); iter.hasNext();) {
            ServiceEndpoint msgDispatcher = (ServiceEndpoint) iter.next();
            msgDispatcher.dispose(nbAttemp, delayMillis);
        }        
    }
    
    private void buildRPC(Dispatcher dispatcher, Method method, Object target) {
        Class[] parameterTypes = method.getParameterTypes();
        
        if (parameterTypes.length == 1) {
            buildSEIOneParameter(dispatcher, method, parameterTypes[0], target);
        } else if (parameterTypes.length == 2) {
            buildSEITwoParameters(dispatcher, method, parameterTypes, target);
        } else {
            throw new IllegalArgumentException(method + " is not supported.");
        }
    }

    private void buildSEIOneParameter(Dispatcher dispatcher, Method method, Class parameterType, Object target) {
        ServiceEndpoint msgDispatcher = new PayloadMsgDispatcher(dispatcher, target, method, parameterType);
        builtDispatchers.add(msgDispatcher);
        dispatcher.register(msgDispatcher);        
    }

    private void buildSEITwoParameters(Dispatcher dispatcher, Method method, Class[] parameterTypes, Object target) {
        if (parameterTypes[0] != Message.class) {
            throw new IllegalArgumentException("First parameter of " +
                method + " must be " + Message.class.getName());
        } else if (false == Serializable.class.isAssignableFrom(parameterTypes[1])) {
            throw new IllegalArgumentException("Second parameter of " +
                            method + " must be an instance of " +
                            Serializable.class.getName());
        }
        
        ServiceEndpoint msgDispatcher = new MsgAndPayloadMsgDispatcher(dispatcher, target, method, parameterTypes[1]);
        builtDispatchers.add(msgDispatcher);
        dispatcher.register(msgDispatcher);
    }
}