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
import java.lang.reflect.Constructor;

import org.codehaus.wadi.aop.ClusteredStateMarker;
import org.codehaus.wadi.aop.tracker.InstanceRegistry;
import org.codehaus.wadi.aop.tracker.InstanceTrackerException;

/**
 * 
 * @version $Revision: 1538 $
 */
public class ConstructorInfo implements ValueUpdater {
    private final Class declaringType;
    private final Class[] parameterTypes;
    private final transient Constructor constructor;
    
    public ConstructorInfo(Constructor constructor) {
        if (null == constructor) {
            throw new IllegalArgumentException("constructor is required");
        }
        this.constructor = constructor;
        constructor.setAccessible(true);
        
        declaringType = constructor.getDeclaringClass();
        parameterTypes = constructor.getParameterTypes();
    }
    
    public void executeWithParameters(InstanceRegistry instanceRegistry, String instanceId, Object[] parameters) {
        ClusteredStateMarker instance;
        try {
            instance = (ClusteredStateMarker) constructor.newInstance(parameters);
        } catch (Exception e) {
            throw new InstanceTrackerException(e);
        }
        instance.$wadiGetTracker().setInstanceId(instanceId);
        instanceRegistry.registerInstance(instanceId, instance);
    }
    
    private Object readResolve() throws ObjectStreamException {
        Constructor constructor;
        try {
            constructor = declaringType.getDeclaredConstructor(parameterTypes);
        } catch (Exception e) {
            throw (InvalidClassException) new InvalidClassException("See nested").initCause(e);
        }
        return new ConstructorInfo(constructor);    
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ConstructorInfo)) {
            return false;
        }
        ConstructorInfo other = (ConstructorInfo) obj;
        return constructor.equals(other.constructor);
    }
    
    @Override
    public int hashCode() {
        return constructor.hashCode();
    }
    
    @Override
    public String toString() {
        return constructor.toString();
    }

}
