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
import org.codehaus.wadi.aop.tracker.InstanceTrackerException;
import org.codehaus.wadi.aop.tracker.InstanceTrackerVisitor;
import org.codehaus.wadi.aop.tracker.VisitorContext;
import org.codehaus.wadi.aop.tracker.visitor.AbstractVisitor;

/**
 * 
 * @version $Revision: 1538 $
 */
public class BasicInstanceTracker implements InstanceTracker {
    private transient final ClusteredStateMarker stateMarker;
    private transient final Map<Long, ValueUpdaterInfo> indexToValueUpdaterInfo;
    private transient final Map<Field, ValueUpdaterInfo> fieldToValueUpdaterInfo;
    private String instanceId;
    
    public BasicInstanceTracker(ClusteredStateMarker stateMarker) {
        if (null == stateMarker) {
            throw new IllegalArgumentException("stateMarker is required");
        }
        this.stateMarker = stateMarker;
        
        indexToValueUpdaterInfo = new HashMap<Long, ValueUpdaterInfo>();
        fieldToValueUpdaterInfo = new HashMap<Field, ValueUpdaterInfo>();
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
        for (ValueUpdaterInfo updaterInfo : fieldToValueUpdaterInfo.values()) {
            updaterInfo.setInstanceId(instanceId);
        }
    }
    
    public void visit(final InstanceTrackerVisitor visitor, final VisitorContext context) {
        if (context.isVisited(this)) {
            return;
        } else {
            context.registerAsVisited(this);
        }
        visitor.visit(this, context);

        VisitAction action = new VisitAction() {
            public void visit(BasicInstanceTracker nestedInstanceTracker) {
                nestedInstanceTracker.visit(visitor, context);
            }
            public void visit(ValueUpdaterInfo valueUpdaterInfo) {
            }
        };
        visitFieldValueUpdaterInfos(action);
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
    
    public void recordFieldUpdate(Field field, Object value) {
        ValueUpdaterInfo valueUpdaterInfo = new ValueUpdaterInfo(new FieldInfo(field), new Object[] {value});
        valueUpdaterInfo.setInstanceId(instanceId);
        fieldToValueUpdaterInfo.put(field, valueUpdaterInfo);
    }
    
    public List<ValueUpdaterInfo> retrieveInstantiationValueUpdaterInfos() {
        ensureInstanceIdIsSet();
        
        IdentityHashMap<InstanceTracker, Boolean> visitedTracker = new IdentityHashMap<InstanceTracker, Boolean>();
        List<ValueUpdaterInfo> valueUpdaterInfos = new ArrayList<ValueUpdaterInfo>();
        addValueUpdaterInfoTo(visitedTracker, valueUpdaterInfos);
        return valueUpdaterInfos;
    }

    public List<ValueUpdaterInfo> retrieveValueUpdaterInfos() {
        ensureInstanceIdIsSet();

        final SortedMap<Long, ValueUpdaterInfo> overallToValueUpdaterInfo = new TreeMap<Long, ValueUpdaterInfo>();
        InstanceTrackerVisitor visitor = new AbstractVisitor() {
            public void visit(InstanceTracker instanceTracker, VisitorContext context) {
                BasicInstanceTracker nestedTracker = (BasicInstanceTracker) instanceTracker;
                for (Map.Entry<Long, ValueUpdaterInfo> entry : nestedTracker.indexToValueUpdaterInfo.entrySet()) {
                    ValueUpdaterInfo valueUpdaterInfo = entry.getValue();
                    valueUpdaterInfo = valueUpdaterInfo.snapshotForSerialization();
                    overallToValueUpdaterInfo.put(entry.getKey(), valueUpdaterInfo);
                }
            }
        };
        visit(visitor, visitor.newContext());
        return new ArrayList<ValueUpdaterInfo>(overallToValueUpdaterInfo.values());
    }

    public void resetTracking() {
        indexToValueUpdaterInfo.clear();
    }

    protected void visitFieldValueUpdaterInfos(VisitAction action) {
        for (ValueUpdaterInfo valueUpdaterInfo : fieldToValueUpdaterInfo.values()) {
            for (InstanceTracker instanceTracker : valueUpdaterInfo.getInstanceTrackers()) {
                if (instanceTracker instanceof BasicInstanceTracker) {
                    BasicInstanceTracker nestedInstanceTracker = (BasicInstanceTracker) instanceTracker;
                    action.visit(nestedInstanceTracker);
                } else {
                    throw new InstanceTrackerException("Not a " + BasicInstanceTracker.class.getName());
                }
            }
            
            action.visit(valueUpdaterInfo);
        }
    }

    protected void addValueUpdaterInfoTo(final IdentityHashMap<InstanceTracker, Boolean> visitedTracker,
        final List<ValueUpdaterInfo> valueUpdaterInfos) {
        if (visitedTracker.containsKey(this)) {
            return;
        }
        visitedTracker.put(this, Boolean.TRUE);
        
        Constructor constructor;
        try {
            constructor = stateMarker.$wadiGetInstanceClass().getDeclaredConstructor();
        } catch (Exception e) {
            throw new InstanceTrackerException(e);
        }
        ValueUpdaterInfo constructorInfo = new ValueUpdaterInfo(new ConstructorInfo(constructor), new Object[0]);
        constructorInfo.setInstanceId(instanceId);
        valueUpdaterInfos.add(constructorInfo);

        VisitAction action = new VisitAction() {
            public void visit(BasicInstanceTracker nestedInstanceTracker) {
                nestedInstanceTracker.addValueUpdaterInfoTo(visitedTracker, valueUpdaterInfos);
            }  
            public void visit(ValueUpdaterInfo valueUpdaterInfo) {
                valueUpdaterInfo = valueUpdaterInfo.snapshotForSerialization();
                valueUpdaterInfos.add(valueUpdaterInfo);
            }
        };
        visitFieldValueUpdaterInfos(action);
    }
    
    protected void ensureInstanceIdIsSet() {
        if (null == instanceId) {
            throw new IllegalStateException("instanceId not set. Cannot retrieve ValueUpdaterInfos.");
        }
    }
    
    protected interface VisitAction {
        void visit(BasicInstanceTracker nestedInstanceTracker);

        void visit(ValueUpdaterInfo valueUpdaterInfo);
    }

}