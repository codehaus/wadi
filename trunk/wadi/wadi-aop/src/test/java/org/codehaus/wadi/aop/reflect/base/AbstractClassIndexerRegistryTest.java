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

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.codehaus.wadi.aop.reflect.ClassIndexer;
import org.codehaus.wadi.aop.reflect.ClassNotIndexedException;

import com.agical.rmock.core.describe.ExpressionDescriber;
import com.agical.rmock.core.match.operator.AbstractExpression;
import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class AbstractClassIndexerRegistryTest extends RMockTestCase {

    private AbstractClassIndexerRegistry registry;

    @Override
    protected void setUp() throws Exception {
        registry = (AbstractClassIndexerRegistry) intercept(AbstractClassIndexerRegistry.class, "id");
    }

    public void testIndex() throws Exception {
        registry.createIndexer(null, null, null);
        modify().args(new AbstractExpression() {
            public void describeWith(ExpressionDescriber arg0) throws IOException {
            }
            public boolean passes(Object arg0) {
                Constructor[] constructors = (Constructor[]) arg0;
                assertEquals(1, constructors.length);
                return true;
            }
        }, new AbstractExpression() {
            public void describeWith(ExpressionDescriber arg0) throws IOException {
            }
            public boolean passes(Object arg0) {
                Method[] methods = (Method[]) arg0;
                assertEquals(1, methods.length);
                return true;
            }
        }, new AbstractExpression() {
            public void describeWith(ExpressionDescriber arg0) throws IOException {
            }
            public boolean passes(Object arg0) {
                Field[] fields = (Field[]) arg0;
                assertEquals(1, fields.length);
                return true;
            }
        });
        
        startVerification();
        
        registry.index(DummyClass.class);
        registry.index(DummyClass.class);
    }
    
    public void testIndexIsCached() throws Exception {
        registry.createIndexer(null, null, null);
        modify().args(is.ANYTHING, is.ANYTHING, is.ANYTHING);
        startVerification();
        
        registry.index(DummyClass.class);
        registry.index(DummyClass.class);
    }

    public void testGetClassIndexer() throws Exception {
        ClassIndexer classIndexer = (ClassIndexer) mock(ClassIndexer.class);
        registry.createIndexer(null, null, null);
        modify().args(is.ANYTHING, is.ANYTHING, is.ANYTHING).returnValue(classIndexer);
        startVerification();
        
        registry.index(DummyClass.class);
        assertSame(classIndexer, registry.getClassIndexer(DummyClass.class));
    }

    public void testGetNotIndexClassFails() throws Exception {
        startVerification();
        
        try {
            registry.getClassIndexer(DummyClass.class);
            fail();
        } catch (ClassNotIndexedException e) {
        }
    }
    
    private static class DummyClass {
        private String name1;
        
        public DummyClass() {
        }
        
        public void name1() {
        }
    }
    
}
