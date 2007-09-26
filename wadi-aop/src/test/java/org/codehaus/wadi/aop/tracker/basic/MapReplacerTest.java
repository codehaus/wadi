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

import java.util.HashMap;
import java.util.Map;


/**
 * 
 * @version $Revision: 1538 $
 */
public class MapReplacerTest extends AbstractReplacerTest {

    private MapReplacer replacer;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        replacer = new MapReplacer(parentReplacer);
    }
    
    public void testCanProcessClusteredStateMarker() throws Exception {
        assertTrue(replacer.canProcess(new HashMap()));
    }
    
    public void testReplaceWithTracker() throws Exception {
        startVerification();
        
        Map map = new HashMap();
        map.put("value", "value");
        map.put(stateMarker, stateMarker);
        
        Map replaced = (Map) replacer.replaceWithTracker(map, trackers);
        assertEquals("value", replaced.get("value"));
        assertEquals(tracker, replaced.get(tracker));
    }
    
    public void testReplaceWithInstance() throws Exception {
        startVerification();

        Map map = new HashMap();
        map.put("value", "value");
        map.put(tracker, tracker);

        Map replaced = (Map) replacer.replaceWithInstance(instanceRegistry, map);
        assertEquals("value", replaced.get("value"));
        assertEquals(stateMarker, replaced.get(stateMarker));
    }
    
}
