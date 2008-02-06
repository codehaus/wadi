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
package org.codehaus.wadi.core.session;

import java.util.Map;

import org.codehaus.wadi.core.manager.Manager;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class StandardSessionTest extends RMockTestCase {

    public void testGetAttributes() throws Exception {
        Manager manager = (Manager) mock(Manager.class);
        StandardSession session = new StandardSession(new StandardAttributes(new StandardValueFactory()), manager);
        
        String key1 = "key1";
        session.addState(key1, "value1");
        String key2 = "key2";
        String value2 = "value2";
        session.addState(key2, value2);
        
        Map stateMap = session.getState();
        assertEquals(2, stateMap.size());
        assertFalse(stateMap.isEmpty());
        assertSame(session.getState(key1), stateMap.get(key1));

        String key3 = "key3";
        stateMap.put(key3, "value3");
        assertSame(stateMap.get(key3), session.getState(key3));
        
        assertSame(value2, stateMap.remove(key2));
        assertNull(session.getState(key2));
    }
    
}
