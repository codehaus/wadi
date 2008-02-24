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
package org.codehaus.wadi.servicespace;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import junit.framework.TestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class InvocationInfoTest extends TestCase {

    public void testExternalizable() throws Exception {
        InvocationMetaData invocationMetaData = new InvocationMetaData();
        invocationMetaData.setOneWay(true);
        InvocationInfo info = new InvocationInfo(String.class, 123, new Object[] {"123"}, invocationMetaData);
        
        ByteArrayOutputStream memOut = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(memOut);
        out.writeObject(info);
        out.close();
        
        ByteArrayInputStream memIn = new ByteArrayInputStream(memOut.toByteArray());
        ObjectInputStream in = new ObjectInputStream(memIn);
        InvocationInfo newInfo = (InvocationInfo) in.readObject();
        
        assertEquals(info.getTargetClass(), newInfo.getTargetClass());
        assertEquals(info.getMemberUpdaterIndex(), newInfo.getMemberUpdaterIndex());
        assertEquals(1, newInfo.getParams().length);
        assertEquals(info.getParams()[0], newInfo.getParams()[0]);
        
        assertTrue(newInfo.getMetaData().isOneWay());
    }
    
}
