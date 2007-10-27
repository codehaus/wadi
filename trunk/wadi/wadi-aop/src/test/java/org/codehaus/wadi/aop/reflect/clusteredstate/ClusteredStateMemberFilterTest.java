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
package org.codehaus.wadi.aop.reflect.clusteredstate;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import junit.framework.TestCase;

import org.codehaus.wadi.aop.annotation.ClusteredState;
import org.codehaus.wadi.aop.annotation.TrackedMethod;
import org.codehaus.wadi.aop.annotation.TrackingLevel;

/**
 * 
 * @version $Revision: 1538 $
 */
public class ClusteredStateMemberFilterTest extends TestCase {

    public void testIndex() throws Exception {
        ClusteredStateMemberFilter filter = new ClusteredStateMemberFilter();
        
        Constructor[] constructors = filter.filterConstructor(DummyClass.class);
        assertEquals(2, constructors.length);
        assertEquals(DummyClass.class.getDeclaredConstructor(), constructors[0]);
        assertEquals(DummyClass.class.getDeclaredConstructor(String.class), constructors[1]);
        
        Method[] methods = filter.filterMethods(DummyClass.class);
        assertEquals(2, methods.length);
        assertEquals(DummyClass.class.getDeclaredMethod("test1"), methods[0]);
        assertEquals(DummyClass.class.getDeclaredMethod("test2"), methods[1]);
        
        Field[] fields = filter.filterFields(DummyClass.class);
        assertEquals(2, fields.length);
        assertEquals(DummyClass.class.getDeclaredField("name1"), fields[0]);
        assertEquals(DummyClass.class.getDeclaredField("name2"), fields[1]);
    }
    
    @ClusteredState(trackingLevel=TrackingLevel.MIXED)
    private static class DummyClass {
        private transient String name0;
        private String name1;
        private String name2;
        
        public DummyClass() {
        }
        
        public DummyClass(String string) {
        }

        public void test0() {
        }

        @TrackedMethod
        public void test1() {
        }

        @TrackedMethod
        public void test2() {
        }
    }
}
