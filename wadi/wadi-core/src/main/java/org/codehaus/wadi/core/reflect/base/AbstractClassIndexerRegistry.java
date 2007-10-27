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
package org.codehaus.wadi.core.reflect.base;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.wadi.core.reflect.ClassIndexer;
import org.codehaus.wadi.core.reflect.ClassIndexerRegistry;
import org.codehaus.wadi.core.reflect.ClassNotIndexedException;
import org.codehaus.wadi.core.reflect.MemberUpdater;




/**
 * 
 * @version $Revision: 1538 $
 */
public abstract class AbstractClassIndexerRegistry implements ClassIndexerRegistry {

    private final MemberFilter memberFilter;
    private final Map<Class, ClassIndexer> classToIndexer;
    
    public AbstractClassIndexerRegistry(MemberFilter memberFilter) {
        if (null == memberFilter) {
            throw new IllegalArgumentException("memberFilter is required");
        }
        this.memberFilter = memberFilter;

        classToIndexer = new HashMap<Class, ClassIndexer>();
    }
    
    public ClassIndexer getClassIndexer(Class clazz) {
        ClassIndexer classIndexer;
        synchronized (classToIndexer) {
            classIndexer = classToIndexer.get(clazz);
        }
        if (null == classIndexer) {
            throw new ClassNotIndexedException("Class [" + clazz + "]");
        }
        return classIndexer;
    }

    public void index(Class clazz) {
        ClassIndexer classIndexer;
        synchronized (classToIndexer) {
            classIndexer = classToIndexer.get(clazz);
        }
        if (null != classIndexer) {
            return;
        }
        
        classIndexer = createIndexer(clazz);
        synchronized (classToIndexer) {
            classToIndexer.put(clazz, classIndexer);    
        }
    }

    protected ClassIndexer createIndexer(Class clazz) {
        Constructor[] constructors = memberFilter.filterConstructor(clazz);
        Method[] methods = memberFilter.filterMethods(clazz);
        Field[] fields = memberFilter.filterFields(clazz);
        return createIndexer(constructors, methods, fields);
    }

    protected ClassIndexer createIndexer(Constructor[] constructors, Method[] methods, Field[] fields) {
        MemberUpdater[] updaters = new MemberUpdater[constructors.length + methods.length + fields.length];
        
        for (int i = 0; i < constructors.length; i++) {
            Constructor constructor = constructors[i];
            updaters[i] = newMemberUpdater(i, constructor);
        }
        
        for (int i = 0; i < methods.length; i++) {
            int index = constructors.length +  i;
            Method method = methods[i];
            updaters[index] = newMemberUpdater(index, method);
        }
        
        for (int i = 0; i < fields.length; i++) {
            int index = constructors.length +  methods.length + i;
            Field field = fields[i];
            updaters[index] = newMemberUpdater(index, field);
        }
        
        return newClassIndexer(updaters);
    }

    protected ClassIndexer newClassIndexer(MemberUpdater[] updaters) {
        return new BasicClassIndexer(updaters);
    }

    protected abstract MemberUpdater newMemberUpdater(int index, Constructor constructor);

    protected abstract MemberUpdater newMemberUpdater(int index, Method method);
    
    protected abstract MemberUpdater newMemberUpdater(int index, Field field);
}
