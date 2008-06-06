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

import java.util.Arrays;
import java.util.Collection;

import org.codehaus.wadi.aop.tracker.InstanceTrackerException;

/**
 * 
 * @version $Revision: 1538 $
 */
public class CollectionReplacer extends AbstractReplacer {
    
    public CollectionReplacer(InstanceAndTrackerReplacer parentReplacer) {
        super(parentReplacer);
    }
    
    public boolean canProcess(Object instance) {
        return instance instanceof Collection;
    }
    
    protected Object replace(Object instance, Replacer replacer) {
        Collection collection = (Collection) instance;
        Collection newCollection = newCollection(collection);
        
        Object[] instances = collection.toArray();
        instances = (Object[]) replacer.replace(instances);
        newCollection.addAll(Arrays.asList(instances));
        return newCollection;
    }

    protected Collection newCollection(Collection collection) {
        Collection newCollection;
        Class collClass = collection.getClass();
        try {
            newCollection = (Collection) collClass.getConstructor().newInstance();
        } catch (Exception e) {
            throw new InstanceTrackerException(e);
        }
        return newCollection;
    }

}