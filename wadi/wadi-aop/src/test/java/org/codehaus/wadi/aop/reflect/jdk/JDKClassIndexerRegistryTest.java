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

import junit.framework.TestCase;

import org.codehaus.wadi.aop.reflect.ClassIndexer;
import org.codehaus.wadi.aop.reflect.MemberUpdater;
import org.codehaus.wadi.aop.reflect.base.DeclaredMemberFilter;

/**
 * 
 * @version $Revision: 1538 $
 */
public class JDKClassIndexerRegistryTest extends TestCase {

    public void testIndex() throws Exception {
        JDKClassIndexerRegistry registry = new JDKClassIndexerRegistry(new DeclaredMemberFilter());
        registry.index(DummyClass.class);
        
        ClassIndexer classIndexer = registry.getClassIndexer(DummyClass.class);
        
        MemberUpdater memberUpdater = classIndexer.getMemberUpdater(0);
        assertEquals(memberUpdater.getIndex(), 0);
        assertEquals(DummyClass.class.getDeclaredConstructor(), memberUpdater.getMember());
        Object instance = memberUpdater.executeWithParameters(null, null);
        assertTrue(instance instanceof DummyClass);
        DummyClass dummyInstance = (DummyClass) instance;
        
        memberUpdater = classIndexer.getMemberUpdater(3);
        assertEquals(memberUpdater.getIndex(), 3);
        assertEquals(DummyClass.class.getDeclaredMethod("test"), memberUpdater.getMember());
        memberUpdater.executeWithParameters(dummyInstance, null);
        assertTrue(dummyInstance.testExecuted);

        memberUpdater = classIndexer.getMemberUpdater(4);
        assertEquals(memberUpdater.getIndex(), 4);
        assertEquals(DummyClass.class.getDeclaredField("name"), memberUpdater.getMember());
        memberUpdater.executeWithParameters(dummyInstance, new Object[] {"test"});
        assertEquals("test", dummyInstance.name);
    }
    
    public static class DummyClass {
        private String name;
        private boolean testExecuted;

        public void test() {
            testExecuted =true;
        }

    }
}
