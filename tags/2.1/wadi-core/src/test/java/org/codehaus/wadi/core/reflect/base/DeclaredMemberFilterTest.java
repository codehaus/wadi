/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.codehaus.wadi.core.reflect.base;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import junit.framework.TestCase;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class DeclaredMemberFilterTest extends TestCase {

    public void testFieldsAreSorted() throws Exception {
        DeclaredMemberFilter filter = new DeclaredMemberFilter();
        Field[] sortedFields = filter.filterFields(DummyClass.class);
        assertEquals(2, sortedFields.length);
        assertEquals("field1", sortedFields[0].getName());
        assertEquals("field2", sortedFields[1].getName());
    }
    
    public void testMethodsAreSorted() throws Exception {
        DeclaredMemberFilter filter = new DeclaredMemberFilter();
        Method[] sortedMethods = filter.filterMethods(DummyClass.class);
        assertEquals(2, sortedMethods.length);
        assertEquals("method1", sortedMethods[0].getName());
        assertEquals("method2", sortedMethods[1].getName());
    }
    
    public void testConstructorsAreSorted() throws Exception {
        DeclaredMemberFilter filter = new DeclaredMemberFilter();
        Constructor[] sortedConstructors = filter.filterConstructor(DummyClass.class);
        assertEquals(2, sortedConstructors.length);
        assertEquals(0, sortedConstructors[0].getParameterTypes().length);
        assertEquals(1, sortedConstructors[1].getParameterTypes().length);
    }
    
    public static class DummyClass {
        private int field2;
        private int field1;

        public DummyClass() {
        }

        public DummyClass(int param) {
        }
        
        public void method2() {
        }
        
        public void method1() {
        }
    }
    
}
