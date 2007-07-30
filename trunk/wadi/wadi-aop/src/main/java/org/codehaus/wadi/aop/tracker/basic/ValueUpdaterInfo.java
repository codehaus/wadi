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

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.codehaus.wadi.aop.tracker.InstanceRegistry;
import org.codehaus.wadi.aop.tracker.InstanceTracker;

/**
 * 
 * @version $Revision: 1538 $
 */
public class ValueUpdaterInfo implements Serializable {
    protected final ValueUpdater valueUpdater;
    protected final Object[] parameters;
    private final InstanceAndTrackerReplacer replacer;
    protected String instanceId;
    
    public static void applyTo(InstanceRegistry instanceRegistry, List<ValueUpdaterInfo> valueUpdaterInfos) {
        for (ValueUpdaterInfo valueUpdaterInfo : valueUpdaterInfos) {
            valueUpdaterInfo.execute(instanceRegistry);
        }
    }

    public ValueUpdaterInfo(ValueUpdater valueUpdater, Object[] parameters) {
        if (null == valueUpdater) {
            throw new IllegalArgumentException("valueUpdater is required");
        } else if (null == parameters) {
            throw new IllegalArgumentException("parameters is required");
        }
        this.valueUpdater = valueUpdater;
        this.parameters = parameters;
        
        replacer = new CompoundReplacer();
    }
    
    protected ValueUpdaterInfo(ValueUpdaterInfo prototype) {
        if (null == prototype) {
            throw new IllegalArgumentException("prototype is required");
        } else if (null == prototype.instanceId) {
            throw new IllegalArgumentException("prototype does not have an instanceId");
        }
        
        valueUpdater = prototype.valueUpdater;
        instanceId = prototype.instanceId;
        replacer = prototype.replacer;
        
        parameters = (Object[]) replacer.replaceWithTracker(prototype.parameters, new HashSet<InstanceTracker>());
    }
    
    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        if (null != this.instanceId) {
            throw new IllegalStateException("instanceId is already set");
        }
        this.instanceId = instanceId;
    }

    public Set<InstanceTracker> getInstanceTrackers() {
        Set<InstanceTracker> trackers = new HashSet<InstanceTracker>();
        replacer.replaceWithTracker(parameters, trackers);
        return trackers;
    }
    
    public Object[] getParameters() {
        return parameters;
    }
    
    public void execute(InstanceRegistry instanceRegistry) {
        Object[] newParameters =  (Object[]) replacer.replaceWithInstance(instanceRegistry, parameters);
        valueUpdater.executeWithParameters(instanceRegistry, instanceId, newParameters);
    }

    public ValueUpdaterInfo snapshotForSerialization() {
        return new ValueUpdaterInfo(this);
    }

}