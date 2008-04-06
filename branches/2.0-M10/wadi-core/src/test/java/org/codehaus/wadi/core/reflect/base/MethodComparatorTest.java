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

import java.lang.reflect.Method;
import java.util.Comparator;

import org.codehaus.wadi.core.reflect.base.MethodComparator;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class MethodComparatorTest extends RMockTestCase {

    private Comparator<Class[]> nestedComparator;
    private MethodComparator comparator;
    private Method methodName1String;
    private Method methodName1Integer;
    private Method methodName2;

    @Override
    protected void setUp() throws Exception {
        nestedComparator = (Comparator<Class[]>) mock(Comparator.class);
        comparator = new MethodComparator(nestedComparator);
        
        methodName1String = DummyClass.class.getDeclaredMethod("name1", String.class);
        methodName1Integer = DummyClass.class.getDeclaredMethod("name1", Integer.class);
        methodName2 = DummyClass.class.getDeclaredMethod("name2", Integer.class);
    }
    
    public void testCompareNames() throws Exception {
        startVerification();
        
        assertTrue(comparator.compare(methodName1String, methodName2) < 0);
    }

    public void testCompareParameters() throws Exception {
        nestedComparator.compare(methodName1String.getParameterTypes(), methodName1Integer.getParameterTypes());
        modify().returnValue(1);

        startVerification();
        
        assertEquals(1, comparator.compare(methodName1String, methodName1Integer));
    }
    
    private static class DummyClass {
        
        public void name1(String arg) {
        }
        
        public void name1(Integer arg) {
        }
        
        public void name2(Integer arg) {
        }
    } 

}
