/**
 * Copyright 2006 The Apache Software Foundation
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
package org.codehaus.wadi.core.session;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.codehaus.wadi.ValueHelper;
import org.codehaus.wadi.ValueHelperRegistry;
import org.codehaus.wadi.core.session.DistributableValue;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class DistributableValueTest extends RMockTestCase {

    public void testCustomSerization() throws Exception {
        ValueHelperRegistry valueHelperRegistry = (ValueHelperRegistry) mock(ValueHelperRegistry.class);
        valueHelperRegistry.findHelper(NotSerializable.class);
        modify().multiplicity(expect.exactly(2)).returnValue(new NotSerializableHelper());
        
        startVerification();
        
        String content = "foo";
        NotSerializable expectedValue = new NotSerializable(content);
        
        ByteArrayOutputStream memOut = new ByteArrayOutputStream();
        ObjectOutputStream oo = new ObjectOutputStream(memOut);
        
        DistributableValue value1  = new DistributableValue(valueHelperRegistry);
        value1.setValue(expectedValue);
        value1.writeExternal(oo);
        
        ByteArrayInputStream memIn = new ByteArrayInputStream(memOut.toByteArray());
        ObjectInputStream oi = new ObjectInputStream(memIn);
        
        DistributableValue value2 = new DistributableValue(valueHelperRegistry);
        value2.readExternal(oi);
        
        Object value = value2.getValue();
        assertTrue(value instanceof NotSerializable);
        NotSerializable actualValue = (NotSerializable) value;
        assertTrue(expectedValue._content.equals(actualValue._content));
    }
    
    static class NotSerializable {
        final String _content;

        public NotSerializable(String content) {
            _content = content;
        }
    }

    static class IsSerializable implements Serializable {
        String _content;

        public IsSerializable() {
        }

        public IsSerializable(String content) {
            _content = content;
        }

        private Object readResolve() {
            return new NotSerializable(_content);
        }
    }

    static class NotSerializableHelper implements ValueHelper {
        public Serializable replace(Object object) {
            return new IsSerializable(((NotSerializable) object)._content);
        }
    }

}
