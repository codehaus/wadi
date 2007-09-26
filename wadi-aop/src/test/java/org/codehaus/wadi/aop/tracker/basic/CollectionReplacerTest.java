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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * 
 * @version $Revision: 1538 $
 */
public class CollectionReplacerTest extends AbstractReplacerTest {

    private CollectionReplacer replacer;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        replacer = new CollectionReplacer(parentReplacer);
    }
    
    public void testCanProcessClusteredStateMarker() throws Exception {
        assertTrue(replacer.canProcess(Arrays.asList(new Object[] {"value", stateMarker})));
    }
    
    public void testReplaceWithTracker() throws Exception {
        startVerification();
        
        List list = new ArrayList();
        list.add("value");
        list.add(stateMarker);
        
        List replaced = (List) replacer.replaceWithTracker(list, trackers);
        assertEquals("value", replaced.get(0));
        assertEquals(tracker, replaced.get(1));
    }
    
    public void testReplaceWithInstance() throws Exception {
        startVerification();

        List list = new ArrayList();
        list.add("value");
        list.add(tracker);

        List replaced = (List) replacer.replaceWithInstance(instanceRegistry, list);
        assertEquals("value", replaced.get(0));
        assertEquals(stateMarker, replaced.get(1));
    }
    
}
