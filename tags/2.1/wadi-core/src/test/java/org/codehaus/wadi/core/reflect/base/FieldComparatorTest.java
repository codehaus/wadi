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

import java.lang.reflect.Field;

import org.codehaus.wadi.core.reflect.base.FieldComparator;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class FieldComparatorTest extends RMockTestCase {

    private FieldComparator comparator;
    private Field fieldName1;
    private Field fieldName2;

    @Override
    protected void setUp() throws Exception {
        comparator = new FieldComparator();
        
        fieldName1 = DummyClass.class.getDeclaredField("name1");
        fieldName2 = DummyClass.class.getDeclaredField("name2");
    }
    
    public void testCompareNames() throws Exception {
        startVerification();
        
        assertTrue(comparator.compare(fieldName1, fieldName2) < 0);
    }

    private static class DummyClass {
        private String name1;
        private String name2;
    } 
    

}
