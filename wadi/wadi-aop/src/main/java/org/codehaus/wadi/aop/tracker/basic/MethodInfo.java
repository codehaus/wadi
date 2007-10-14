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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Method;

import org.codehaus.wadi.aop.tracker.InstanceRegistry;
import org.codehaus.wadi.aop.tracker.InstanceTrackerException;

/**
 * 
 * @version $Revision: 1538 $
 */
public class MethodInfo implements ValueUpdater {
    private Class declaringType;
    private String name;
    private Class[] parameterTypes;
    private transient Method method;

    public MethodInfo() {
    }
    
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
    
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        declaringType = (Class) in.readObject();
        name = in.readUTF();
        parameterTypes = (Class[]) in.readObject();

        try {
            method = declaringType.getDeclaredMethod(name, parameterTypes);
        } catch (Exception e) {
            throw (IOException) new IOException("See nested").initCause(e);
        }
        method.setAccessible(true);
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(declaringType);
        out.writeUTF(name);
        out.writeObject(parameterTypes);
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
