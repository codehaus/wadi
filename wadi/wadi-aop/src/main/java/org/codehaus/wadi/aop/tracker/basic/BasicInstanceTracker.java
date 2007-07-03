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
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.codehaus.wadi.aop.ClusteredStateMarker;
import org.codehaus.wadi.aop.tracker.InstanceRegistry;
import org.codehaus.wadi.aop.tracker.InstanceTracker;
import org.codehaus.wadi.aop.tracker.InstanceTrackerVisitor;
import org.codehaus.wadi.aop.tracker.VisitorContext;

/**
 * 
 * @version $Revision: 1538 $
 */
public class BasicInstanceTracker implements InstanceTracker {
    private String instanceId;
    private Map<Long, ValueUpdaterInfo> indexToValueUpdaterInfo;
    
    public BasicInstanceTracker() {
        indexToValueUpdaterInfo = new HashMap<Long, ValueUpdaterInfo>();
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
            updaterInfo.instanceId = instanceId;
        }
    }
    
    public void visit(InstanceTrackerVisitor visitor, VisitorContext context) {
        if (context.isVisited(this)) {
            return;
        }
        
        visitor.visit(this, context);

        for (ValueUpdaterInfo valueUpdaterInfo : indexToValueUpdaterInfo.values()) {
            for (int i = 0; i < valueUpdaterInfo.parameters.length; i++) {
                Object parameter = valueUpdaterInfo.parameters[i];
                if (parameter instanceof InstanceTracker) {
                    InstanceTracker nestedInstanceTracker = (InstanceTracker) parameter;
                    nestedInstanceTracker.visit(visitor, context);
                }
            }
        }
    }
    
    public void track(long index, Constructor constructor, Object[] parameters) {
        ValueUpdaterInfo valueUpdaterInfo = new ValueUpdaterInfo(new ConstructorInfo(constructor), parameters);
        valueUpdaterInfo.instanceId = instanceId;
        indexToValueUpdaterInfo.put(new Long(index), valueUpdaterInfo);
    }

    public void track(long index, Field field, Object value) {
        ValueUpdaterInfo valueUpdaterInfo = new ValueUpdaterInfo(new FieldInfo(field), new Object[] {value});
        valueUpdaterInfo.instanceId = instanceId;
        indexToValueUpdaterInfo.put(new Long(index), valueUpdaterInfo);
    }

    public void track(long index, Method method, Object[] parameters) {
        ValueUpdaterInfo valueUpdaterInfo = new ValueUpdaterInfo(new MethodInfo(method), parameters);
        valueUpdaterInfo.instanceId = instanceId;
        indexToValueUpdaterInfo.put(new Long(index), valueUpdaterInfo);
    }

    public void applyTo(InstanceRegistry instanceRegistry) {
        IdentityHashMap<InstanceTracker, Boolean> visitedTracker = new IdentityHashMap<InstanceTracker, Boolean>();
        SortedMap<Long, ValueUpdaterInfo> overallToValueUpdaterInfo = new TreeMap<Long, ValueUpdaterInfo>();
        addValueUpdaterInfoTo(visitedTracker, overallToValueUpdaterInfo);
        
        for (ValueUpdaterInfo valueUpdaterInfo : overallToValueUpdaterInfo.values()) {
            valueUpdaterInfo.execute(instanceRegistry);
        }
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
            
            for (int i = 0; i < valueUpdaterInfo.parameters.length; i++) {
                Object parameter = valueUpdaterInfo.parameters[i];
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
    
    protected static class ValueUpdaterInfo implements Serializable {
        private final ValueUpdater valueUpdater;
        private final Object[] parameters;
        private String instanceId;
        
        protected ValueUpdaterInfo(ValueUpdater valueUpdater, Object[] parameters) {
            this.valueUpdater = valueUpdater;
            this.parameters = replaceInstanceWithItsTracker(parameters);
        }
        
        protected void execute(InstanceRegistry instanceRegistry) {
            Object[] newParameters = replaceTrackerWithItsInstance(instanceRegistry, parameters);
            valueUpdater.executeWithParameters(instanceRegistry, instanceId, newParameters);
        }
        
        protected Object[] replaceInstanceWithItsTracker(Object[] parameters) {
            Object[] actualParameters = new Object[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                Object value = parameters[i];
                value = replaceInstanceWithItsTracker(value);
                actualParameters[i] = value;
            }
            return actualParameters;
        }
        
        protected Object replaceInstanceWithItsTracker(Object parameter) {
            if (parameter instanceof ClusteredStateMarker) {
                parameter = ((ClusteredStateMarker) parameter).$wadiGetTracker();
            }
            return parameter;
        }
        
        protected Object[] replaceTrackerWithItsInstance(InstanceRegistry instanceRegistry, Object[] parameters) {
            Object[] actualParameters = new Object[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                Object parameter = parameters[i];
                parameter = replaceTrackerWithItsInstance(instanceRegistry, parameter);
                actualParameters[i] = parameter;
            }
            return actualParameters;
        }
        
        protected Object replaceTrackerWithItsInstance(InstanceRegistry instanceRegistry, Object parameter) {
            if (parameter instanceof InstanceTracker) {
                String instanceId = ((InstanceTracker) parameter).getInstanceId();
                return instanceRegistry.getInstance(instanceId);
            }
            return parameter;
        }
        
    }

}
