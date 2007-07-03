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
import java.lang.reflect.Field;

import org.codehaus.wadi.aop.tracker.InstanceRegistry;
import org.codehaus.wadi.aop.tracker.InstanceTrackerException;

/**
 * 
 * @version $Revision: 1538 $
 */
public class FieldInfo implements ValueUpdater {
    private final Class declaringType;
    private final String name;
    private transient final Field field;
    
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
        Object instance = instanceRegistry.getInstance(instanceId);
        try {
            field.set(instance, parameters[0]);
        } catch (Exception e) {
            throw new InstanceTrackerException(e);
        }
    }
    
    private Object readResolve() throws ObjectStreamException {
        Field field;
        try {
            field = declaringType.getDeclaredField(name);
        } catch (Exception e) {
            throw (InvalidClassException) new InvalidClassException("See nested").initCause(e);
        }
        return new FieldInfo(field);    
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
