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
public class ArrayReplacer extends AbstractReplacer {
    
    public ArrayReplacer(InstanceAndTrackerReplacer parentReplacer) {
        super(parentReplacer);
    }

    public boolean canProcess(Object instance) {
        return null != instance && instance.getClass().isArray();
    }

    protected Object replace(Object instance, Replacer replacer) {
        Object[] instances = (Object[]) instance;
        Object[] actualInstances = new Object[instances.length];
        for (int i = 0; i < instances.length; i++) {
            actualInstances[i] = replacer.replace(instances[i]);
        }
        return actualInstances;
    }
    
}