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
package org.codehaus.wadi.aop.reflect.base;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.wadi.aop.reflect.ClassIndexer;
import org.codehaus.wadi.aop.reflect.ClassIndexerRegistry;
import org.codehaus.wadi.aop.reflect.ClassNotIndexedException;



/**
 * 
 * @version $Revision: 1538 $
 */
public abstract class AbstractClassIndexerRegistry implements ClassIndexerRegistry {

    private final Map<Class, ClassIndexer> classToIndexer;
    
    public AbstractClassIndexerRegistry() {
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
        // TODO inheritence.
        Constructor[] constructors = identifyConstructor(clazz);
        Method[] methods = identifyMethods(clazz);
        Field[] fields = identifyFields(clazz);
        return createIndexer(constructors, methods, fields);
    }

    protected Field[] identifyFields(Class clazz) {
        Field[] fields = clazz.getDeclaredFields();
        Arrays.sort(fields, new FieldComparator());
        return fields;
    }

    protected Method[] identifyMethods(Class clazz) {
        Method[] methods = clazz.getDeclaredMethods();
        Arrays.sort(methods, new MethodComparator());
        return methods;
    }

    protected Constructor[] identifyConstructor(Class clazz) {
        Constructor[] constructors = clazz.getDeclaredConstructors();
        Arrays.sort(constructors, new ConstructorComparator());
        return constructors;
    }

    protected abstract ClassIndexer createIndexer(Constructor[] constructors, Method[] methods, Field[] fields);
}
