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
import java.util.Comparator;

import org.codehaus.wadi.core.reflect.base.ConstructorComparator;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class ConstructorComparatorTest extends RMockTestCase {

    private ConstructorComparator comparator;
    private Comparator<Class[]> nestedComparator;
    private Constructor constructorString;
    private Constructor constructorInteger;

    @Override
    protected void setUp() throws Exception {
        nestedComparator = (Comparator<Class[]>) mock(Comparator.class);
        
        comparator = new ConstructorComparator(nestedComparator);
        
        constructorString = DummyClass.class.getDeclaredConstructor(String.class);
        constructorInteger = DummyClass.class.getDeclaredConstructor(Integer.class);
    }
    
    public void testCompareParameters() throws Exception {
        nestedComparator.compare(constructorString.getParameterTypes(), constructorInteger.getParameterTypes());
        modify().returnValue(1);
        
        startVerification();
        
        assertEquals(1, comparator.compare(constructorString, constructorInteger));
    }

    private static class DummyClass {
        
        public DummyClass(String arg) {
        }
        
        public DummyClass(Integer arg) {
        }
    } 
    

}
