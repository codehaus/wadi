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
import org.codehaus.wadi.core.reflect.ClassIndexer;

/**
 * 
 * @version $Revision: 1538 $
 */
public class BasicInstanceTracker implements InstanceTracker {
    private transient final InstanceAndTrackerReplacer replacer;
    private transient final ClassIndexer classIndexer;
    private transient final ClusteredStateMarker stateMarker;
    private transient final Map<Long, ValueUpdaterInfo> indexToValueUpdaterInfo;
    private transient final Map<Field, ValueUpdaterInfo> fieldToValueUpdaterInfo;
    private String instanceId;
    
    public BasicInstanceTracker(InstanceAndTrackerReplacer replacer,
            ClassIndexer classIndexer,
            ClusteredStateMarker stateMarker) {
        if (null == replacer) {
            throw new IllegalArgumentException("replacer is required");
        } else if (null == classIndexer) {
            throw new IllegalArgumentException("classIndexer is required");
        } else if (null == stateMarker) {
            throw new IllegalArgumentException("stateMarker is required");
        }
        this.replacer = replacer;
        this.classIndexer = classIndexer;
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
        int memberIndex = classIndexer.getIndex(constructor);
        Class targetClass = stateMarker.$wadiGetInstanceClass();
        ValueUpdaterInfo valueUpdaterInfo = new ValueUpdaterInfo(replacer, targetClass, memberIndex, parameters);
        valueUpdaterInfo.setInstanceId(instanceId);
        indexToValueUpdaterInfo.put(new Long(index), valueUpdaterInfo);
    }

    public void track(long index, Field field, Object value) {
        int memberIndex = classIndexer.getIndex(field);
        ValueUpdaterInfo valueUpdaterInfo = new ValueUpdaterInfo(replacer, memberIndex, new Object[] {value});
        valueUpdaterInfo.setInstanceId(instanceId);
        indexToValueUpdaterInfo.put(new Long(index), valueUpdaterInfo);
    }

    public void track(long index, Method method, Object[] parameters) {
        int memberIndex = classIndexer.getIndex(method);
        ValueUpdaterInfo valueUpdaterInfo = new ValueUpdaterInfo(replacer, memberIndex, parameters);
        valueUpdaterInfo.setInstanceId(instanceId);
        indexToValueUpdaterInfo.put(new Long(index), valueUpdaterInfo);
    }
    
    public void recordFieldUpdate(Field field, Object value) {
        int memberIndex = classIndexer.getIndex(field);
        ValueUpdaterInfo valueUpdaterInfo = new ValueUpdaterInfo(replacer, memberIndex, new Object[] {value});
        valueUpdaterInfo.setInstanceId(instanceId);
        fieldToValueUpdaterInfo.put(field, valueUpdaterInfo);
    }
    
    public ValueUpdaterInfo[] retrieveInstantiationValueUpdaterInfos(InstanceTrackerVisitor preVisitor,
        InstanceTrackerVisitor postVisitor) {
        IdentityHashMap<InstanceTracker, Boolean> visitedTracker = new IdentityHashMap<InstanceTracker, Boolean>();
        List<ValueUpdaterInfo> valueUpdaterInfos = new ArrayList<ValueUpdaterInfo>();
        addValueUpdaterInfoTo(visitedTracker, valueUpdaterInfos, preVisitor, postVisitor);
        return valueUpdaterInfos.toArray(new ValueUpdaterInfo[valueUpdaterInfos.size()]);
    }

    public ValueUpdaterInfo[] retrieveValueUpdaterInfos(final InstanceTrackerVisitor preVisitor,
        final InstanceTrackerVisitor postVisitor) {

        final SortedMap<Long, ValueUpdaterInfo> overallToValueUpdaterInfo = new TreeMap<Long, ValueUpdaterInfo>();
        InstanceTrackerVisitor visitor = new AbstractVisitor() {
            public void visit(InstanceTracker instanceTracker, VisitorContext context) {
                preVisitor.visit(instanceTracker, null);
                
                BasicInstanceTracker nestedTracker = (BasicInstanceTracker) instanceTracker;
                for (Map.Entry<Long, ValueUpdaterInfo> entry : nestedTracker.indexToValueUpdaterInfo.entrySet()) {
                    ValueUpdaterInfo valueUpdaterInfo = entry.getValue();
                    valueUpdaterInfo = valueUpdaterInfo.snapshotForSerialization();
                    overallToValueUpdaterInfo.put(entry.getKey(), valueUpdaterInfo);
                }
                
                preVisitor.visit(instanceTracker, null);
            }
        };
        visit(visitor, visitor.newContext());
        return overallToValueUpdaterInfo.values().toArray(new ValueUpdaterInfo[overallToValueUpdaterInfo.size()]);
    }

    public void resetTracking() {
        indexToValueUpdaterInfo.clear();
    }
    
    public ClassIndexer getClassIndexer() {
        return classIndexer;
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
        final List<ValueUpdaterInfo> valueUpdaterInfos,
        final InstanceTrackerVisitor preVisitor,
        final InstanceTrackerVisitor postVisitor) {
        if (visitedTracker.containsKey(this)) {
            return;
        }
        visitedTracker.put(this, Boolean.TRUE);
        preVisitor.visit(this, null);
        
        Class targetClass = stateMarker.$wadiGetInstanceClass();
        Constructor constructor;
        try {
            constructor = targetClass.getDeclaredConstructor();
        } catch (Exception e) {
            throw new InstanceTrackerException(e);
        }
        int memberIndex = classIndexer.getIndex(constructor);
        ValueUpdaterInfo constructorInfo = new ValueUpdaterInfo(replacer, targetClass, memberIndex, new Object[0]);
        constructorInfo.setInstanceId(instanceId);
        valueUpdaterInfos.add(constructorInfo);

        postVisitor.visit(this, null);

        VisitAction action = new VisitAction() {
            public void visit(BasicInstanceTracker nestedInstanceTracker) {
                preVisitor.visit(nestedInstanceTracker, null);
                nestedInstanceTracker.addValueUpdaterInfoTo(visitedTracker, valueUpdaterInfos, preVisitor, postVisitor);
                postVisitor.visit(nestedInstanceTracker, null);
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
