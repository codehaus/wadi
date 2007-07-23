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
import java.util.List;

import org.codehaus.wadi.aop.ClusteredStateMarker;
import org.codehaus.wadi.aop.tracker.InstanceRegistry;
import org.codehaus.wadi.aop.tracker.InstanceTracker;

/**
 * 
 * @version $Revision: 1538 $
 */
public class ValueUpdaterInfo implements Serializable {
    private final ValueUpdater valueUpdater;
    private final Object[] parameters;
    private String instanceId;
    
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
        this.parameters = replaceInstanceWithItsTracker(parameters);
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

    public Object[] getParameters() {
        return parameters;
    }
    
    public void execute(InstanceRegistry instanceRegistry) {
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