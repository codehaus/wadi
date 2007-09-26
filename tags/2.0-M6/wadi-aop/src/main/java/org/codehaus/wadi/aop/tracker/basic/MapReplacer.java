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

import java.util.Iterator;
import java.util.Map;

import org.codehaus.wadi.aop.tracker.InstanceTrackerException;

/**
 * 
 * @version $Revision: 1538 $
 */
public class MapReplacer extends AbstractReplacer {
    
    public MapReplacer(InstanceAndTrackerReplacer parentReplacer) {
        super(parentReplacer);
    }
    
    public boolean canProcess(Object instance) {
        return instance instanceof Map;
    }

    @Override
    protected Object replace(Object instance, Replacer replacer) {
        Map map = (Map) instance;
        Map newMap = newMap(map);
        
        for (Iterator iterator = map.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            Object key = entry.getKey();
            Object value = entry.getValue();
            key = replacer.replace(key);
            value = replacer.replace(value);
            newMap.put(key, value);
        }
        return newMap;
    }
    
     protected Map newMap(Map map) {
        Class mapClass = map.getClass();
        try {
            return (Map) mapClass.getConstructor().newInstance();
        } catch (Exception e) {
            throw new InstanceTrackerException(e);
        }
    }

}