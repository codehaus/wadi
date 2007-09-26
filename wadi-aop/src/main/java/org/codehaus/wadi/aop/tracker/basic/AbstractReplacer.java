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

import java.util.Set;

import org.codehaus.wadi.aop.tracker.InstanceRegistry;
import org.codehaus.wadi.aop.tracker.InstanceTracker;

/**
 * 
 * @version $Revision: 1538 $
 */
public abstract class AbstractReplacer implements InstanceAndTrackerReplacer {

    private final InstanceAndTrackerReplacer parentReplacer;
    
    public AbstractReplacer(InstanceAndTrackerReplacer parentReplacer) {
        if (null == parentReplacer) {
            throw new IllegalArgumentException("parentReplacer is required");
        }
        this.parentReplacer = parentReplacer;
    }

    public Object replaceWithTracker(Object instance, final Set<InstanceTracker> trackers) {
        return replace(instance, new Replacer() {
            public Object replace(Object instance) {
                return parentReplacer.replaceWithTracker(instance, trackers);
            }
        });
    }
    
    public Object replaceWithInstance(final InstanceRegistry instanceRegistry, Object instance) {
        return replace(instance, new Replacer() {
            public Object replace(Object instance) {
                return parentReplacer.replaceWithInstance(instanceRegistry, instance);
            }
        });
    }

    protected abstract Object replace(Object instance, Replacer replacer);
    
    protected interface Replacer {
        Object replace(Object instance);
    }
    
}