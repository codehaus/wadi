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
public class ClusteredStateMarkerReplacerTest extends AbstractReplacerTest {

    private ClusteredStateMarkerReplacer replacer;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        replacer = new ClusteredStateMarkerReplacer();
    }
    
    public void testCanProcessClusteredStateMarker() throws Exception {
        assertTrue(replacer.canProcess(stateMarker));
    }
    
    public void testCannotProcessNull() throws Exception {
        assertFalse(replacer.canProcess(null));
    }
    
    public void testReplaceWithTracker() throws Exception {
        startVerification();
        
        assertSame(tracker, replacer.replaceWithTracker(stateMarker, trackers));
    }
    
    public void testReplaceWithInstance() throws Exception {
        startVerification();
        
        assertSame(stateMarker, replacer.replaceWithInstance(instanceRegistry, tracker));
    }
    
}
