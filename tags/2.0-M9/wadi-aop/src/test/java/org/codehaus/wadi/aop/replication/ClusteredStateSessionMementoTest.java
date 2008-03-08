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
package org.codehaus.wadi.aop.replication;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import org.codehaus.wadi.aop.ClusteredStateMarker;
import org.codehaus.wadi.aop.aspectj.ClusteredStateAspectUtil;
import org.codehaus.wadi.aop.tracker.InstanceTracker;
import org.codehaus.wadi.aop.tracker.InstanceTrackerFactory;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 2340 $
 */
public class ClusteredStateSessionMementoTest extends RMockTestCase {

    private InstanceTracker instanceTracker;

    @Override
    protected void setUp() throws Exception {
        instanceTracker = (InstanceTracker) mock(InstanceTracker.class);
        
        InstanceTrackerFactory trackerFactory = new InstanceTrackerFactory() {
            public void prepareTrackerForClass(Class clazz) {
            }
            public InstanceTracker newInstanceTracker(ClusteredStateMarker stateMarker) {
                return instanceTracker;
            }    
        };
        ClusteredStateAspectUtil.resetInstanceTrackerFactory();
        ClusteredStateAspectUtil.setInstanceTrackerFactory(trackerFactory);
    }
    
    public void testRestore() throws Exception {
        // ClusteredStateAttributesMemento instance tracking
        instanceTracker.track(1, (Constructor) null, null);
        modify().args(is.ANYTHING, is.ANYTHING, is.ANYTHING);
        
        // ClusteredStateAttributesMemento instance tracking
        instanceTracker.track(1, (Constructor) null, null);
        modify().args(is.ANYTHING, is.ANYTHING, is.ANYTHING);
        instanceTracker.track(1, (Field) null, null);
        modify().args(is.ANYTHING, is.ANYTHING, is.ANYTHING);
        instanceTracker.recordFieldUpdate(null, null);
        modify().args(is.ANYTHING, is.ANYTHING);
        
        // ClusteredStateAttributesMemento.onRestore
        instanceTracker.track(1, (Field) null, null);
        modify().args(is.ANYTHING, is.ANYTHING, is.ANYTHING);
        instanceTracker.recordFieldUpdate(null, null);
        modify().args(is.ANYTHING, is.ANYTHING);
        
        startVerification();
        
        ClusteredStateSessionMemento memento = new ClusteredStateSessionMemento();
        setField(memento, "creationTime", new Long(1));
        setField(memento, "lastAccessedTime", new Long(2));
        setField(memento, "maxInactiveInterval", new Integer(3));
        setField(memento, "name", "name");
        setField(memento, "memento", new ClusteredStateAttributesMemento());
        
        assertEquals(0, memento.getCreationTime());
        assertEquals(0, memento.getLastAccessedTime());
        assertEquals(0, memento.getMaxInactiveInterval());
        assertNull(memento.getName());
        assertTrue(memento.isNewSession());
        
        memento.onRestore();
        
        assertEquals(1, memento.getCreationTime());
        assertEquals(2, memento.getLastAccessedTime());
        assertEquals(3, memento.getMaxInactiveInterval());
        assertEquals("name", memento.getName());
        assertFalse(memento.isNewSession());
        assertNotNull(memento.getAttributesMemento());
    }

    private void setField(ClusteredStateSessionMemento memento, String name, Object value) throws Exception {
        Field field = ClusteredStateSessionMemento.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(memento, value);
    }
    
}
