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
package org.codehaus.wadi.aop.tracker.basic;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.codehaus.wadi.aop.tracker.InstanceRegistry;

import com.agical.rmock.core.describe.ExpressionDescriber;
import com.agical.rmock.core.match.operator.AbstractExpression;
import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class ConstructorInfoTest extends RMockTestCase {

    private ConstructorInfo constructorInfo;
    private InstanceRegistry instanceRegistry;

    @Override
    protected void setUp() throws Exception {
        instanceRegistry = (InstanceRegistry) mock(InstanceRegistry.class);
        constructorInfo = new ConstructorInfo(DummyClass.class.getConstructor(new Class[] {int.class}));
    }
    
    public void testSerialization() throws Exception {
        ByteArrayOutputStream memOut = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(memOut);
        out.writeObject(constructorInfo);
        out.close();
        
        ByteArrayInputStream memIn = new ByteArrayInputStream(memOut.toByteArray());
        ObjectInputStream in = new ObjectInputStream(memIn);
        ConstructorInfo serializedConstructorInfo = (ConstructorInfo) in.readObject();
        assertEquals(serializedConstructorInfo, constructorInfo);
    }
    
    public void testExecuteWithParameters() throws Exception {
        final int testValue = 123;
        String instanceId = "instanceId";
        instanceRegistry.registerInstance(instanceId, null);
        modify().args(is.AS_RECORDED, new AbstractExpression() {

            public void describeWith(ExpressionDescriber arg0) throws IOException {
            }

            public boolean passes(Object arg0) {
                DummyClass instance = (DummyClass) arg0;
                assertEquals(testValue, instance.test);
                return true;
            }
            
        });
        startVerification();
        
        constructorInfo.executeWithParameters(instanceRegistry, instanceId, new Object[] {new Integer(testValue)});
    }
    
    public static class DummyClass {
        private final int test;

        public DummyClass(int test) {
            this.test = test;
        }
        
    }
    
}
