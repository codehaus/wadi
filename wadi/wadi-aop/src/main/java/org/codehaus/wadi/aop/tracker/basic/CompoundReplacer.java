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
import java.util.List;
import java.util.Set;

import org.codehaus.wadi.aop.tracker.InstanceRegistry;
import org.codehaus.wadi.aop.tracker.InstanceTracker;

/**
 * 
 * @version $Revision: 1538 $
 */
public class CompoundReplacer implements InstanceAndTrackerReplacer {

    protected final List<InstanceAndTrackerReplacer> instanceToTrackers;
    
    public CompoundReplacer() {
        instanceToTrackers = new ArrayList<InstanceAndTrackerReplacer>();
        initReplacers();
    }

    protected void initReplacers() {
        instanceToTrackers.add(new ClusteredStateMarkerReplacer());
        instanceToTrackers.add(new ArrayReplacer(this));
        instanceToTrackers.add(new CollectionReplacer(this));
        instanceToTrackers.add(new MapReplacer(this));
    }
    
    public boolean canProcess(Object instance) {
        return true;
    }

    public Object replaceWithTracker(Object instance, Set<InstanceTracker> trackers) {
        for (InstanceAndTrackerReplacer instanceToTracker : instanceToTrackers) {
            if (instanceToTracker.canProcess(instance)) {
                return instanceToTracker.replaceWithTracker(instance, trackers);
            }
        }
        return instance;
    }
    
    public Object replaceWithInstance(InstanceRegistry instanceRegistry, Object instance) {
        for (InstanceAndTrackerReplacer instanceToTracker : instanceToTrackers) {
            if (instanceToTracker.canProcess(instance)) {
                return instanceToTracker.replaceWithInstance(instanceRegistry, instance);
            }
        }
        return instance;
    }
    
}