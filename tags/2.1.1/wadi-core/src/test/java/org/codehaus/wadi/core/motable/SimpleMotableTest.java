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
package org.codehaus.wadi.core.motable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import junit.framework.TestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class SimpleMotableTest extends TestCase {

    public void testExternalizableContracts() throws Exception {
        SimpleMotable motable = new SimpleMotable();
        motable.init(1, 2, 3, "name");
        motable.setBodyAsByteArray(new byte[] {'1'});
        
        ByteArrayOutputStream memOut = new ByteArrayOutputStream();
        ObjectOutputStream oo = new ObjectOutputStream(memOut);
        
        oo.writeObject(motable);
        oo.flush();
        
        ByteArrayInputStream memIn = new ByteArrayInputStream(memOut.toByteArray());
        ObjectInputStream oi = new ObjectInputStream(memIn);
        SimpleMotable deserializedMotable = (SimpleMotable) oi.readObject();
        byte[] actualBody = deserializedMotable.getBodyAsByteArray();
        assertNotNull(actualBody);
        assertEquals(1, actualBody.length);
        assertEquals('1', actualBody[0]);
    }
    
}
