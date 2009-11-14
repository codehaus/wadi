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

package org.codehaus.wadi.core.eviction;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import junit.framework.TestCase;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class SimpleEvictableMementoTest extends TestCase {

    public void testExternalizable() throws Exception {
        SimpleEvictableMemento memento = new SimpleEvictableMemento();
        memento.setCreationTime(1);
        memento.setLastAccessedTime(2);
        memento.setMaxInactiveInterval(3);
        memento.setNeverEvict(true);
        
        ByteArrayOutputStream memOut = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(memOut);
        memento.writeExternal(oos);
        oos.close();
        
        SimpleEvictableMemento cloned = new SimpleEvictableMemento();
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(memOut.toByteArray()));
        cloned.readExternal(ois);
        
        assertEquals(memento.getCreationTime(), cloned.getCreationTime());
        assertEquals(memento.getLastAccessedTime(), cloned.getLastAccessedTime());
        assertEquals(memento.getMaxInactiveInterval(), cloned.getMaxInactiveInterval());
        assertTrue(memento.isNeverEvict());
    }
    
}
