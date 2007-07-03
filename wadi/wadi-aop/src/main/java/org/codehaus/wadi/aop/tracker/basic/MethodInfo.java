/**
 * Copyright 2007 The Apache Software Foundation
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
package org.codehaus.wadi.aop.tracker.basic;

import java.io.InvalidClassException;
import java.io.ObjectStreamException;
import java.lang.reflect.Method;

import org.codehaus.wadi.aop.tracker.InstanceRegistry;
import org.codehaus.wadi.aop.tracker.InstanceTrackerException;

/**
 * 
 * @version $Revision: 1538 $
 */
public class MethodInfo implements ValueUpdater {
    private final Class declaringType;
    private final String name;
    private final Class[] parameterTypes;
    private transient final Method method;
    
    public MethodInfo(Method method) {
        if (null == method) {
            throw new IllegalArgumentException("method is required");
        }
        this.method = method;
        method.setAccessible(true);
        
        declaringType = method.getDeclaringClass();
        name = method.getName();
        parameterTypes = method.getParameterTypes();
    }
    
    public void executeWithParameters(InstanceRegistry instanceRegistry, String instanceId, Object[] parameters) {
        Object instance = instanceRegistry.getInstance(instanceId);
        try {
            method.invoke(instance, parameters);
        } catch (Exception e) {
            throw new InstanceTrackerException(e);
        }
    }
    
    private Object readResolve() throws ObjectStreamException {
        Method method;
        try {
            method = declaringType.getDeclaredMethod(name, parameterTypes);
        } catch (Exception e) {
            throw (InvalidClassException) new InvalidClassException("See nested").initCause(e);
        }
        return new MethodInfo(method);    
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MethodInfo)) {
            return false;
        }
        MethodInfo other = (MethodInfo) obj;
        return method.equals(other.method);
    }
    
    @Override
    public int hashCode() {
        return method.hashCode();
    }
    
    @Override
    public String toString() {
        return method.toString();
    }

}
