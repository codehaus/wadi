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
package org.codehaus.wadi.aop.reflect.jdk;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.codehaus.wadi.aop.reflect.ClassIndexer;
import org.codehaus.wadi.aop.reflect.MemberUpdater;
import org.codehaus.wadi.aop.reflect.base.AbstractClassIndexerRegistry;
import org.codehaus.wadi.aop.reflect.base.BasicClassIndexer;


/**
 * 
 * @version $Revision: 1538 $
 */
public class JDKClassIndexerRegistry extends AbstractClassIndexerRegistry {

    @Override
    protected ClassIndexer createIndexer(Constructor[] constructors, Method[] methods, Field[] fields) {
        MemberUpdater[] updaters = new MemberUpdater[constructors.length + methods.length + fields.length];
        
        for (int i = 0; i < constructors.length; i++) {
            updaters[i] = new ConstructorUpdater(i, constructors[i]);
        }
        
        for (int i = 0; i < methods.length; i++) {
            int index = constructors.length +  i;
            updaters[index] = new MethodUpdater(index, methods[i]);
        }
        
        for (int i = 0; i < fields.length; i++) {
            int index = constructors.length +  methods.length + i;
            updaters[index] = new FieldUpdater(index, fields[i]);
        }
        
        return newClassIndexer(updaters);
    }

    protected ClassIndexer newClassIndexer(MemberUpdater[] updaters) {
        return new BasicClassIndexer(updaters);
    }
    
}
