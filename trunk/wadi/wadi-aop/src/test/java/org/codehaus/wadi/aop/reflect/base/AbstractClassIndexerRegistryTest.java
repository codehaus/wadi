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

import org.codehaus.wadi.aop.reflect.ClassIndexer;
import org.codehaus.wadi.aop.reflect.ClassNotIndexedException;
import org.codehaus.wadi.aop.reflect.MemberUpdater;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class AbstractClassIndexerRegistryTest extends RMockTestCase {

    private AbstractClassIndexerRegistry registry;
    private MemberFilter memberFilter;

    @Override
    protected void setUp() throws Exception {
        memberFilter = (MemberFilter) mock(MemberFilter.class);

        registry = 
            (AbstractClassIndexerRegistry) intercept(AbstractClassIndexerRegistry.class,
                new Object[] { memberFilter },
            "registry");
    }

    public void testGetNotIndexClassFails() throws Exception {
        startVerification();
        
        try {
            registry.getClassIndexer(DummyClass.class);
            fail();
        } catch (ClassNotIndexedException e) {
        }
    }
    
    public void testIndex() throws Exception {
        memberFilter.filterConstructor(DummyClass.class);
        Constructor constructor = DummyClass.class.getDeclaredConstructor();
        modify().returnValue(new Constructor[] {constructor});
        
        memberFilter.filterMethods(DummyClass.class);
        Method method = DummyClass.class.getDeclaredMethod("test");
        modify().returnValue(new Method[] {method});
        
        memberFilter.filterFields(DummyClass.class);
        Field field = DummyClass.class.getDeclaredField("name");
        modify().returnValue(new Field[] {field});
        
        MemberUpdater constructorUpdater = registry.newMemberUpdater(0, constructor);
        constructorUpdater.getMember();
        modify().returnValue(constructor);
        MemberUpdater methodUpdater = registry.newMemberUpdater(1, method);
        methodUpdater.getMember();
        modify().returnValue(method);
        MemberUpdater fieldUpdater = registry.newMemberUpdater(2, field);
        fieldUpdater.getMember();
        modify().returnValue(field);
        
        startVerification();
        
        registry.index(DummyClass.class);
        registry.index(DummyClass.class);
        
        ClassIndexer classIndexer = registry.getClassIndexer(DummyClass.class);
        
        MemberUpdater memberUpdater = classIndexer.getMemberUpdater(0);
        assertEquals(constructorUpdater, memberUpdater);
        
        memberUpdater = classIndexer.getMemberUpdater(1);
        assertEquals(methodUpdater, memberUpdater);

        memberUpdater = classIndexer.getMemberUpdater(2);
        assertEquals(fieldUpdater, memberUpdater);
    }
    
    private static class DummyClass {
        private String name;
        
        public DummyClass() {
        }
        
        public void test() {
        }

    }

}
