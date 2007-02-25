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

import junit.framework.TestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class StandardAttributesTest extends TestCase {

    public void testGetAttributes() throws Exception {
        StandardAttributes attributes = new StandardAttributes(new StandardValueFactory());
        
        String key1 = "key1";
        attributes.put(key1, "value1");
        String key2 = "key2";
        String value2 = "value2";
        attributes.put(key2, value2);
        
        Map attributesMap = attributes.getAttributes();
        assertEquals(2, attributesMap.size());
        assertFalse(attributesMap.isEmpty());
        assertSame(attributes.get(key1), attributesMap.get(key1));

        String key3 = "key3";
        attributesMap.put(key3, "value3");
        assertSame(attributesMap.get(key3), attributes.get(key3));
        
        assertSame(value2, attributesMap.remove(key2));
        assertNull(attributes.get(key2));
    }
    
}
