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

package org.codehaus.wadi.cache.store;

import org.codehaus.wadi.cache.basic.ObjectInfo;
import org.codehaus.wadi.cache.basic.ObjectInfoEntry;
import org.codehaus.wadi.core.contextualiser.Contextualiser;
import org.codehaus.wadi.core.motable.Immoter;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class ObjectStoreContextualiserTest extends RMockTestCase {

    private Contextualiser next;
    private ObjectLoader loader;
    private ObjectStoreContextualiser contextualiser;

    @Override
    protected void setUp() throws Exception {
        next = (Contextualiser) mock(Contextualiser.class);
        loader = (ObjectLoader) mock(ObjectLoader.class);
        contextualiser = new ObjectStoreContextualiser(next, loader);
    }

    public void testGetSharedDemoterReturnsNextSharedDemoter() throws Exception {
        Immoter expectedImmoter = next.getSharedDemoter();
        
        startVerification();
        
        Immoter immoter = contextualiser.getSharedDemoter();
        assertSame(expectedImmoter, immoter);
    }
    
    public void testGetLoadObject() throws Exception {
        String key = "key";
        
        loader.load(key);
        Object expectedObject = new Object();
        modify().returnValue(expectedObject);
        
        startVerification();
        
        ObjectMotable motable = contextualiser.get(key, false);
        ObjectInfoEntry objectInfoEntry = motable.getObjectInfoEntry();
        ObjectInfo objectInfo = objectInfoEntry.getObjectInfo();
        assertSame(expectedObject, objectInfo.getObject());
    }
    
    public void testGetLoadNullObject() throws Exception {
        String key = "key";
        
        loader.load(key);
        
        startVerification();
        
        ObjectMotable motable = contextualiser.get(key, false);
        ObjectInfoEntry objectInfoEntry = motable.getObjectInfoEntry();
        ObjectInfo objectInfo = objectInfoEntry.getObjectInfo();
        assertSame(true, objectInfo.isUndefined());
    }
    
}
