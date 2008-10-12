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

package org.codehaus.wadi.cache.basic;

import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.codehaus.wadi.core.util.Streamer;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class ObjectInfoTest extends RMockTestCase {

    public void testSetObjectResetUndefined() throws Exception {
        ObjectInfo objectInfo = new ObjectInfo();
        assertTrue(objectInfo.isUndefined());
        objectInfo.setObject(new Object());
        assertFalse(objectInfo.isUndefined());
    }
    
    public void testSetObjectNullThrowsIAE() throws Exception {
        ObjectInfo objectInfo = new ObjectInfo(1, new Object());
        try {
            objectInfo.setObject(null);
            fail();
        } catch (IllegalArgumentException e) {
        }
    }
    
    public void testCanMerge() throws Exception {
        ObjectInfo baselineObjectInfo = new ObjectInfo(1, new Object());
        ObjectInfo toMergeObjectInfo = new ObjectInfo(2, new Object());
        
        assertTrue(baselineObjectInfo.canMerge(toMergeObjectInfo));
    }
    
    public void testCanNotMerge() throws Exception {
        ObjectInfo baselineObjectInfo = new ObjectInfo(2, new Object());
        ObjectInfo toMergeObjectInfo = new ObjectInfo(2, new Object());
        
        assertFalse(baselineObjectInfo.canMerge(toMergeObjectInfo));
    }
    
    public void testMerge() throws Exception {
        ObjectInfo baselineObjectInfo = new ObjectInfo(1, new Object());
        ObjectInfo toMergeObjectInfo = new ObjectInfo(2, new Object());
        
        baselineObjectInfo.merge(toMergeObjectInfo);
        assertSame(toMergeObjectInfo.getObject(), baselineObjectInfo.getObject());
        assertEquals(toMergeObjectInfo.getVersion(), baselineObjectInfo.getVersion());
    }
    
    public void testIncrementVersion() throws Exception {
        ObjectInfo baselineObjectInfo = new ObjectInfo(1, new Object());
        
        Streamer streamer = (Streamer) mock(Streamer.class);

        ObjectOutput oo = streamer.getOutputStream(null);
        modify().args(is.NOT_NULL);
        oo.writeObject(baselineObjectInfo.getObject());
        oo.close();
        
        ObjectInput oi = streamer.getInputStream(null);
        modify().args(is.NOT_NULL);
        oi.readObject();
        Object objectCopy = new Object();
        modify().returnValue(objectCopy);
        
        startVerification();
        
        ObjectInfo incrementedObjectInfo = baselineObjectInfo.incrementVersion(streamer);
        assertEquals(baselineObjectInfo.getVersion() + 1, incrementedObjectInfo.getVersion());
        assertSame(objectCopy, incrementedObjectInfo.getObject());
    }
 
    public void testIncrementVersionForUndefinedObjectInfo() throws Exception {
        Streamer streamer = (Streamer) mock(Streamer.class);

        ObjectInfo baselineObjectInfo = new ObjectInfo();
        
        ObjectInfo incrementedObjectInfo = baselineObjectInfo.incrementVersion(streamer);
        assertSame(baselineObjectInfo, incrementedObjectInfo);
    }
    
}
