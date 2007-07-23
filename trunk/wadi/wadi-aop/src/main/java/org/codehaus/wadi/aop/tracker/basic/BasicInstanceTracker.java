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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.codehaus.wadi.aop.ClusteredStateMarker;
import org.codehaus.wadi.aop.tracker.InstanceTracker;
import org.codehaus.wadi.aop.tracker.InstanceTrackerVisitor;
import org.codehaus.wadi.aop.tracker.VisitorContext;

/**
 * 
 * @version $Revision: 1538 $
 */
public class BasicInstanceTracker implements InstanceTracker {
    private final transient ClusteredStateMarker stateMarker;
    private String instanceId;
    private Map<Long, ValueUpdaterInfo> indexToValueUpdaterInfo;
    
    public BasicInstanceTracker(ClusteredStateMarker stateMarker) {
        if (null == stateMarker) {
            throw new IllegalArgumentException("stateMarker is required");
        }
        this.stateMarker = stateMarker;
        
        indexToValueUpdaterInfo = new HashMap<Long, ValueUpdaterInfo>();
    }
    
    public ClusteredStateMarker getInstance() {
        return stateMarker;
    }
    
    public String getInstanceId() {
        return instanceId;
    }
    
    public void setInstanceId(String instanceId) {
        if (null != this.instanceId) {
            throw new IllegalStateException("instanceId is already set");
        } else if (null == instanceId) {
            throw new IllegalStateException("instanceId is required");
        }
        this.instanceId = instanceId;
        
        for (ValueUpdaterInfo updaterInfo : indexToValueUpdaterInfo.values()) {
            updaterInfo.setInstanceId(instanceId);
        }
    }
    
    public void visit(InstanceTrackerVisitor visitor, VisitorContext context) {
        if (context.isVisited(this)) {
            return;
        }
        
        visitor.visit(this, context);

        for (ValueUpdaterInfo valueUpdaterInfo : indexToValueUpdaterInfo.values()) {
            for (int i = 0; i < valueUpdaterInfo.getParameters().length; i++) {
                Object parameter = valueUpdaterInfo.getParameters()[i];
                if (parameter instanceof InstanceTracker) {
                    InstanceTracker nestedInstanceTracker = (InstanceTracker) parameter;
                    nestedInstanceTracker.visit(visitor, context);
                }
            }
        }
    }
    
    public void track(long index, Constructor constructor, Object[] parameters) {
        ValueUpdaterInfo valueUpdaterInfo = new ValueUpdaterInfo(new ConstructorInfo(constructor), parameters);
        valueUpdaterInfo.setInstanceId(instanceId);
        indexToValueUpdaterInfo.put(new Long(index), valueUpdaterInfo);
    }

    public void track(long index, Field field, Object value) {
        ValueUpdaterInfo valueUpdaterInfo = new ValueUpdaterInfo(new FieldInfo(field), new Object[] {value});
        valueUpdaterInfo.setInstanceId(instanceId);
        indexToValueUpdaterInfo.put(new Long(index), valueUpdaterInfo);
    }

    public void track(long index, Method method, Object[] parameters) {
        ValueUpdaterInfo valueUpdaterInfo = new ValueUpdaterInfo(new MethodInfo(method), parameters);
        valueUpdaterInfo.setInstanceId(instanceId);
        indexToValueUpdaterInfo.put(new Long(index), valueUpdaterInfo);
    }
    
    
    public List<ValueUpdaterInfo> retrieveValueUpdaterInfos() {
        IdentityHashMap<InstanceTracker, Boolean> visitedTracker = new IdentityHashMap<InstanceTracker, Boolean>();
        SortedMap<Long, ValueUpdaterInfo> overallToValueUpdaterInfo = new TreeMap<Long, ValueUpdaterInfo>();
        addValueUpdaterInfoTo(visitedTracker, overallToValueUpdaterInfo);
        return new ArrayList<ValueUpdaterInfo>(overallToValueUpdaterInfo.values());
    }

    protected void addValueUpdaterInfoTo(IdentityHashMap<InstanceTracker, Boolean> visitedTracker,
        SortedMap<Long, ValueUpdaterInfo> overallToValueUpdaterInfo) {
        if (visitedTracker.containsKey(this)) {
            return;
        }
        visitedTracker.put(this, Boolean.TRUE);
        
        for (Map.Entry<Long, ValueUpdaterInfo> entry : indexToValueUpdaterInfo.entrySet()) {
            ValueUpdaterInfo valueUpdaterInfo = entry.getValue();
            overallToValueUpdaterInfo.put(entry.getKey(), valueUpdaterInfo);
            
            for (int i = 0; i < valueUpdaterInfo.getParameters().length; i++) {
                Object parameter = valueUpdaterInfo.getParameters()[i];
                if (parameter instanceof BasicInstanceTracker) {
                    BasicInstanceTracker nestedInstanceTracker = (BasicInstanceTracker) parameter;
                    nestedInstanceTracker.addValueUpdaterInfoTo(visitedTracker, overallToValueUpdaterInfo);
                }
            }
        }
    }
    
    public void resetTracking() {
        indexToValueUpdaterInfo.clear();
    }

}
