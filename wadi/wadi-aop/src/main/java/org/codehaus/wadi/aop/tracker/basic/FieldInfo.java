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
import java.lang.reflect.Field;

import org.codehaus.wadi.aop.ClusteredStateMarker;
import org.codehaus.wadi.aop.tracker.InstanceRegistry;
import org.codehaus.wadi.aop.tracker.InstanceTrackerException;

/**
 * 
 * @version $Revision: 1538 $
 */
public class FieldInfo implements ValueUpdater {
    private Class declaringType;
    private String name;
    private transient Field field;

    public FieldInfo() {
    }
    
    public FieldInfo(Field field) {
        if (null == field) {
            throw new IllegalArgumentException("field is required");
        }
        this.field = field;
        field.setAccessible(true);
        
        declaringType = field.getDeclaringClass();
        name = field.getName();
    }

    public void executeWithParameters(InstanceRegistry instanceRegistry, String instanceId, Object[] parameters) {
        ClusteredStateMarker instance = (ClusteredStateMarker) instanceRegistry.getInstance(instanceId);
        try {
            field.set(instance, parameters[0]);
        } catch (Exception e) {
            throw new InstanceTrackerException(e);
        }
        instance.$wadiGetTracker().recordFieldUpdate(field, parameters[0]);
    }
    
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        declaringType = (Class) in.readObject();
        name = in.readUTF();

        try {
            field = declaringType.getDeclaredField(name);
        } catch (Exception e) {
            throw (IOException) new IOException("See nested").initCause(e);
        }
        field.setAccessible(true);
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(declaringType);
        out.writeUTF(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FieldInfo)) {
            return false;
        }
        FieldInfo other = (FieldInfo) obj;
        return field.equals(other.field);
    }
    
    @Override
    public int hashCode() {
        return field.hashCode();
    }
    
    @Override
    public String toString() {
        return field.toString();
    }

}
