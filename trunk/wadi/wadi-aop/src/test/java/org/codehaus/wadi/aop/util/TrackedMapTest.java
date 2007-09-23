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
package org.codehaus.wadi.aop.util;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.wadi.aop.ClusteredStateMarker;
import org.codehaus.wadi.aop.aspectj.ClusteredStateAspectUtil;
import org.codehaus.wadi.aop.tracker.InstanceIdFactory;
import org.codehaus.wadi.aop.tracker.InstanceRegistry;
import org.codehaus.wadi.aop.tracker.basic.BasicInstanceIdFactory;
import org.codehaus.wadi.aop.tracker.basic.BasicInstanceRegistry;
import org.codehaus.wadi.aop.tracker.basic.BasicInstanceTrackerFactory;
import org.codehaus.wadi.core.util.SimpleStreamer;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class TrackedMapTest extends RMockTestCase {

    private InstanceIdFactory instanceIdFactory;
    private InstanceRegistry instanceRegistry;
    private TrackedMap trackedMap;
    private TrackedMap replicatedMap;

    @Override
    protected void setUp() throws Exception {
        instanceIdFactory = new BasicInstanceIdFactory();
        ClusteredStateAspectUtil.resetInstanceTrackerFactory();
        ClusteredStateAspectUtil.setInstanceTrackerFactory(new BasicInstanceTrackerFactory());
        
        trackedMap = new TrackedMap();
        trackedMap.setDelegate(new HashMap());
        
        byte[] serializeFully = ClusteredStateHelper.serializeFully(instanceIdFactory, trackedMap);
        ClusteredStateHelper.resetTracker(trackedMap);
        ClusteredStateMarker stateMarker = ClusteredStateHelper.castAndEnsureType(trackedMap);
        
        instanceRegistry = new BasicInstanceRegistry();
        ClusteredStateHelper.deserialize(instanceRegistry, new SimpleStreamer(), serializeFully);
        
        replicatedMap = (TrackedMap) instanceRegistry.getInstance(stateMarker.$wadiGetTracker().getInstanceId());
    }
    
    public void testDelegate() throws Exception {
        Map delegate = (Map) mock(Map.class);
        
        delegate.clear();
        delegate.containsKey("key");
        delegate.containsValue("value");
        delegate.entrySet();
        delegate.equals(null);
        delegate.get("key");
        delegate.hashCode();
        delegate.isEmpty();
        delegate.keySet();
        delegate.put("key", "value");
        delegate.putAll(null);
        delegate.remove("key");
        delegate.size();
        delegate.values();
        startVerification();
        
        trackedMap.setDelegate(delegate);

        trackedMap.clear();
        trackedMap.containsKey("key");
        trackedMap.containsValue("value");
        trackedMap.entrySet();
        trackedMap.equals(null);
        trackedMap.get("key");
        trackedMap.hashCode();
        trackedMap.isEmpty();
        trackedMap.keySet();
        trackedMap.put("key", "value");
        trackedMap.putAll(null);
        trackedMap.remove("key");
        trackedMap.size();
        trackedMap.values();
    }
    
    public void testClearIsTracked() throws Exception {
        executePut();
        
        trackedMap.clear();
        
        byte[] serialize = ClusteredStateHelper.serialize(instanceIdFactory, trackedMap);
        ClusteredStateHelper.deserialize(instanceRegistry, new SimpleStreamer(), serialize);
        
        assertTrue(replicatedMap.isEmpty());
    }
    
    public void testPutIsTracked() throws Exception {
        trackedMap.put("key", "value");
        
        byte[] serialize = ClusteredStateHelper.serialize(instanceIdFactory, trackedMap);
        ClusteredStateHelper.deserialize(instanceRegistry, new SimpleStreamer(), serialize);
        
        assertEquals(trackedMap, replicatedMap);
    }
    
    public void testPutAllIsTracked() throws Exception {
        Map all = new HashMap();
        all.put("key1", "value");
        all.put("key2", "value");
        trackedMap.putAll(all);
        
        byte[] serialize = ClusteredStateHelper.serialize(instanceIdFactory, trackedMap);
        ClusteredStateHelper.deserialize(instanceRegistry, new SimpleStreamer(), serialize);
        
        assertEquals(trackedMap, replicatedMap);
    }
    
    public void testRemoveIsTracked() throws Exception {
        executePut();

        trackedMap.remove("key");

        byte[] serialize = ClusteredStateHelper.serialize(instanceIdFactory, trackedMap);
        ClusteredStateHelper.deserialize(instanceRegistry, new SimpleStreamer(), serialize);
        
        assertTrue(replicatedMap.isEmpty());
    }
    
    protected void executePut() throws Exception {
        trackedMap.put("key", "value");
        
        byte[] serialize = ClusteredStateHelper.serialize(instanceIdFactory, trackedMap);
        ClusteredStateHelper.deserialize(instanceRegistry, new SimpleStreamer(), serialize);
        
        assertEquals(trackedMap, replicatedMap);
    }

}