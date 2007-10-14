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


/**
 * 
 * @version $Revision: 1538 $
 */
public class ArrayReplacerTest extends AbstractReplacerTest {

    private ArrayReplacer replacer;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        replacer = new ArrayReplacer(parentReplacer);
    }
    
    public void testCanProcessClusteredStateMarker() throws Exception {
        assertTrue(replacer.canProcess(new Object[] {"value", stateMarker}));
    }
    
    public void testCannotProcessNull() throws Exception {
        assertFalse(replacer.canProcess(null));
    }
    
    public void testReplaceWithTracker() throws Exception {
        startVerification();
        
        Object[] replaced = (Object[]) replacer.replaceWithTracker(new Object[] {"value", stateMarker}, trackers);
        assertEquals("value", replaced[0]);
        assertEquals(tracker, replaced[1]);
    }
    
    public void testReplaceWithInstance() throws Exception {
        startVerification();
        
        Object[] replaced = (Object[]) replacer.replaceWithInstance(instanceRegistry, new Object[] {"value", tracker});
        assertEquals("value", replaced[0]);
        assertEquals(stateMarker, replaced[1]);
    }
    
}
