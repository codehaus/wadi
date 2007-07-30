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
package org.codehaus.wadi.aop.aspectj;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.ConstructorSignature;
import org.aspectj.lang.reflect.FieldSignature;
import org.aspectj.lang.reflect.MethodSignature;
import org.codehaus.wadi.aop.ClusteredStateMarker;
import org.codehaus.wadi.aop.annotation.ClusteredState;
import org.codehaus.wadi.aop.annotation.TrackedMethod;
import org.codehaus.wadi.aop.annotation.TrackingLevel;
import org.codehaus.wadi.aop.tracker.InstanceTracker;
import org.codehaus.wadi.aop.tracker.InstanceTrackerFactory;


/**
 * 
 * @version $Revision: 1538 $
 */
public aspect ClusteredStateAspect {

    // ITD
    declare parents:
        (@ClusteredState *) implements ClusteredStateMarker;

    public static InstanceTrackerFactory trackerFactory;
    public static long index = 0;
    
    private transient InstanceTracker ClusteredStateMarker.tracker;

    public InstanceTracker ClusteredStateMarker.$wadiGetTracker() {
        return tracker;
    }

    public Class ClusteredStateMarker.$wadiGetInstanceClass() {
        return this.getClass();
    }

    pointcut withinClusteredStateAspectAdviceExecution():
        adviceexecution() && within(ClusteredStateAspect);
    
    pointcut targetAnnotatedWithClusteredState(ClusteredState clusteredState, ClusteredStateMarker stateMarker):
        @target(clusteredState) && target(stateMarker); 

    // Track instance construction
    pointcut instanceConstruction(ClusteredStateMarker stateMarker):
        execution(ClusteredStateMarker+.new(..)) && target(stateMarker);

    before(ClusteredStateMarker stateMarker):
        instanceConstruction(stateMarker) {
        stateMarker.tracker =  trackerFactory.newInstanceTracker(stateMarker);
        
        Signature signature = thisJoinPointStaticPart.getSignature();
        Constructor constructor = ((ConstructorSignature) signature).getConstructor();

        stateMarker.tracker.track(nextIndex(), constructor, thisJoinPoint.getArgs());
    }
    
    // Track field updates
    pointcut setTrackedField(ClusteredState clusteredState, ClusteredStateMarker stateMarker, Object value):
        targetAnnotatedWithClusteredState(clusteredState, stateMarker) &&
        set(!transient * ClusteredStateMarker+.*) &&
        args(value) &&
        (
            if(clusteredState.trackingLevel() == TrackingLevel.FIELD) ||
            if(clusteredState.trackingLevel() == TrackingLevel.MIXED)
        );
    
    after(ClusteredState clusteredState, ClusteredStateMarker stateMarker, Object value):
        setTrackedField(clusteredState, stateMarker, value)  && !cflow(withinClusteredStateAspectAdviceExecution()) {
        Signature signature = thisJoinPointStaticPart.getSignature();
        Field field = ((FieldSignature) signature).getField();
        
        stateMarker.tracker.track(nextIndex(), field, value);
    }
    
    after(ClusteredState clusteredState, ClusteredStateMarker stateMarker, Object value):
        targetAnnotatedWithClusteredState(clusteredState, stateMarker) &&
        set(!transient * ClusteredStateMarker+.*) &&
        args(value) {
        Signature signature = thisJoinPointStaticPart.getSignature();
        Field field = ((FieldSignature) signature).getField();

        stateMarker.tracker.recordFieldUpdate(field, value);
    }
    
    // Track method executions
    pointcut executionTrackedMethod(ClusteredState clusteredState, ClusteredStateMarker stateMarker):
        targetAnnotatedWithClusteredState(clusteredState, stateMarker) &&
        execution(@TrackedMethod * ClusteredStateMarker+.*(..)) &&
        (
            if(clusteredState.trackingLevel() == TrackingLevel.METHOD) ||
            if(clusteredState.trackingLevel() == TrackingLevel.MIXED)
        );

    Object around(ClusteredState clusteredState, ClusteredStateMarker stateMarker):
        executionTrackedMethod(clusteredState, stateMarker) && !cflow(withinClusteredStateAspectAdviceExecution()) {
        Object result = proceed(clusteredState, stateMarker);
        Signature signature = thisJoinPointStaticPart.getSignature();
        Method method = ((MethodSignature)signature).getMethod();
        
        stateMarker.tracker.track(nextIndex(), method, thisJoinPoint.getArgs());
        
        return result;
    }
    
    private static synchronized long nextIndex() {
        return index++;
    }

}
