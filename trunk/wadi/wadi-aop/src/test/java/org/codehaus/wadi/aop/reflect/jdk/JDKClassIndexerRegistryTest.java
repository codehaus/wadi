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

/**
 * 
 * @version $Revision: 1538 $
 */
public class JDKClassIndexerRegistryTest extends TestCase {

    public void testIndex() throws Exception {
        JDKClassIndexerRegistry registry = new JDKClassIndexerRegistry();
        registry.index(DummyClass.class);
        
        ClassIndexer classIndexer = registry.getClassIndexer(DummyClass.class);
        
        MemberUpdater memberUpdater = classIndexer.getMemberUpdater(0);
        assertEquals(memberUpdater.getMember(), DummyClass.class.getDeclaredConstructor());
        
        memberUpdater = classIndexer.getMemberUpdater(1);
        assertEquals(memberUpdater.getIndex(), 1);
        assertEquals(memberUpdater.getMember(), DummyClass.class.getDeclaredConstructor(String.class));

        memberUpdater = classIndexer.getMemberUpdater(2);
        assertEquals(memberUpdater.getIndex(), 2);
        assertEquals(memberUpdater.getMember(), DummyClass.class.getDeclaredMethod("test1"));

        memberUpdater = classIndexer.getMemberUpdater(3);
        assertEquals(memberUpdater.getIndex(), 3);
        assertEquals(memberUpdater.getMember(), DummyClass.class.getDeclaredMethod("test2"));

        memberUpdater = classIndexer.getMemberUpdater(4);
        assertEquals(memberUpdater.getIndex(), 4);
        assertEquals(memberUpdater.getMember(), DummyClass.class.getDeclaredField("name1"));
        
        memberUpdater = classIndexer.getMemberUpdater(5);
        assertEquals(memberUpdater.getIndex(), 5);
        assertEquals(memberUpdater.getMember(), DummyClass.class.getDeclaredField("name2"));
    }
    
    private static class DummyClass {
        private String name1;
        private String name2;
        
        public DummyClass() {
        }
        
        public DummyClass(String string) {
        }
        
        public void test1() {
        }

        public void test2() {
        }
    }
}
