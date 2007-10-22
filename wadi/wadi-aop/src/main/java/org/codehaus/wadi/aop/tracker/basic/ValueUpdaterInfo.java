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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashSet;
import java.util.Set;

import org.codehaus.wadi.aop.ClusteredStateMarker;
import org.codehaus.wadi.aop.reflect.ClassIndexer;
import org.codehaus.wadi.aop.reflect.ClassIndexerRegistry;
import org.codehaus.wadi.aop.reflect.MemberUpdater;
import org.codehaus.wadi.aop.tracker.InstanceRegistry;
import org.codehaus.wadi.aop.tracker.InstanceTracker;

/**
 * 
 * @version $Revision: 1538 $
 */
public class ValueUpdaterInfo implements Externalizable {
    private final InstanceAndTrackerReplacer replacer;
    private final ClassIndexerRegistry classIndexerRegistry;

    private Class targetClass;
    private int memberUpdaterIndex;
    protected Object[] parameters;
    protected Object[] parametersReplacedWithTrackers;
    protected Set<InstanceTracker> instanceTrackers;
    protected String instanceId;
    
    public static void applyTo(InstanceRegistry instanceRegistry, ValueUpdaterInfo[] valueUpdaterInfos) {
        for (ValueUpdaterInfo valueUpdaterInfo : valueUpdaterInfos) {
            valueUpdaterInfo.execute(instanceRegistry);
        }
    }

    public ValueUpdaterInfo(InstanceAndTrackerReplacer replacer, ClassIndexerRegistry classIndexerRegistry) {
        if (null == replacer) {
            throw new IllegalArgumentException("replacer is required");
        } else if (null == classIndexerRegistry) {
            throw new IllegalArgumentException("classIndexerRegistry is required");
        }
        this.replacer = replacer;
        this.classIndexerRegistry = classIndexerRegistry;
    }

    public ValueUpdaterInfo(InstanceAndTrackerReplacer replacer, int memberUpdaterIndex, Object[] parameters) {
        if (null == replacer) {
            throw new IllegalArgumentException("replacer is required");
        } else if (null == parameters) {
            throw new IllegalArgumentException("parameters is required");
        }
        this.replacer = replacer;
        this.memberUpdaterIndex = memberUpdaterIndex;
        this.parameters = parameters;
        
        classIndexerRegistry = null;
    }

    public ValueUpdaterInfo(InstanceAndTrackerReplacer replacer,
            Class targetClass,
            int memberUpdaterIndex,
            Object[] parameters) {
        if (null == replacer) {
            throw new IllegalArgumentException("replacer is required");
        } else if (null == targetClass) {
            throw new IllegalArgumentException("targetClass is required");
        } else if (null == parameters) {
            throw new IllegalArgumentException("parameters is required");
        }
        this.replacer = replacer;
        this.targetClass = targetClass;
        this.memberUpdaterIndex = memberUpdaterIndex;
        this.parameters = parameters;
        
        classIndexerRegistry = null;
    }
    
    protected ValueUpdaterInfo(ValueUpdaterInfo prototype) {
        if (null == prototype) {
            throw new IllegalArgumentException("prototype is required");
        } else if (null == prototype.instanceId) {
            throw new IllegalArgumentException("prototype does not have an instanceId");
        }
        
        memberUpdaterIndex = prototype.memberUpdaterIndex;
        instanceId = prototype.instanceId;
        replacer = prototype.replacer;
        parameters = null;
        if (null != prototype.parametersReplacedWithTrackers) {
            parametersReplacedWithTrackers = prototype.parametersReplacedWithTrackers;
        } else {
            parametersReplacedWithTrackers = 
                (Object[]) replacer.replaceWithTracker(prototype.parameters, new HashSet<InstanceTracker>());
        }
        
        classIndexerRegistry = null;
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
        if (null != instanceTrackers) {
            return instanceTrackers;
        }
        instanceTrackers = new HashSet<InstanceTracker>();
        parametersReplacedWithTrackers = (Object[]) replacer.replaceWithTracker(parameters, instanceTrackers);
        return instanceTrackers;
    }
    
    public Object[] getParametersReplacedWithTrackers() {
        if (null == parametersReplacedWithTrackers) {
            throw new IllegalStateException("parametersReplacedWithTrackers not set.");
        }
        return parametersReplacedWithTrackers;
    }

    public Object[] getParameters() {
        if (null == parameters) {
            throw new IllegalStateException("parameters not set. This is a snapshotForSerialization instance.");
        }
        return parameters;
    }
    
    public void execute(InstanceRegistry instanceRegistry) {
        Object[] newParameters =
            (Object[]) replacer.replaceWithInstance(instanceRegistry, parametersReplacedWithTrackers);

        ClassIndexer classIndexer;
        if (null != targetClass) {
            classIndexer = classIndexerRegistry.getClassIndexer(targetClass);
        } else {
            ClusteredStateMarker instance = (ClusteredStateMarker) instanceRegistry.getInstance(instanceId);
            classIndexer = instance.$wadiGetTracker().getClassIndexer();
        }

        MemberUpdater memberUpdater = classIndexer.getMemberUpdater(memberUpdaterIndex);
        ValueUpdater valueUpdater = memberUpdater.getValueUpdater();
        valueUpdater.executeWithParameters(instanceRegistry, instanceId, newParameters);
    }
    
    public ValueUpdaterInfo snapshotForSerialization() {
        return new ValueUpdaterInfo(this);
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        boolean constructor = in.readBoolean();
        if (constructor) {
            targetClass = (Class) in.readObject();
        }
        
        memberUpdaterIndex = in.readInt();
        parametersReplacedWithTrackers = (Object[]) in.readObject();
        instanceId = in.readUTF();
    }
    
    public void writeExternal(ObjectOutput out) throws IOException {
        if (null != targetClass) {
            out.writeBoolean(true);
            out.writeObject(targetClass);
        } else {
            out.writeBoolean(false);
        }
        
        out.writeInt(memberUpdaterIndex);
        out.writeObject(parametersReplacedWithTrackers);
        out.writeUTF(instanceId);
    }
    
    @Override
    public String toString() {
        return "ValueUpdaterInfo for [" + instanceId + "]; Index [" + memberUpdaterIndex + "]";
    }

}