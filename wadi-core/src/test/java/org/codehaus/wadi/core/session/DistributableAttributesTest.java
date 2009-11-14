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
package org.codehaus.wadi.core.session;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class DistributableAttributesTest extends RMockTestCase {

    public void testSerialization() throws Exception {
        ValueHelperRegistry valueHelperRegistry = (ValueHelperRegistry) mock(ValueHelperRegistry.class);
        ValueFactory valueFactory = new DistributableValueFactory(valueHelperRegistry);
        DistributableAttributes attributes = new DistributableAttributes(valueFactory);
        
        String key1 = "key1";
        attributes.put(key1, "value1");
        String key2 = "key2";
        attributes.put(key2, "value2");
        
        ByteArrayOutputStream memOut = new ByteArrayOutputStream();
        ObjectOutputStream oo = new ObjectOutputStream(memOut);
        attributes.writeExternal(oo);
        oo.flush();
        
        ByteArrayInputStream memIn = new ByteArrayInputStream(memOut.toByteArray());
        ObjectInputStream oi = new ObjectInputStream(memIn);
        DistributableAttributes deserializedAttributes = new DistributableAttributes(valueFactory);
        deserializedAttributes.readExternal(oi);
        assertEquals(attributes.get(key1), deserializedAttributes.get(key1));
        assertEquals(attributes.get(key2), deserializedAttributes.get(key2));
    }
    
}
