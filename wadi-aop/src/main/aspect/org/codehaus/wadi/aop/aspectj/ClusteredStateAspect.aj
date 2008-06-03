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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
    private static final Log log = LogFactory.getLog(ClusteredStateAspect.class);
    
    // ITD
    declare parents:
        (@ClusteredState *) implements ClusteredStateMarker;

    public static InstanceTrackerFactory trackerFactory;
    public static long index = 0;
    
    private transient InstanceTracker ClusteredStateMarker.tracker;
    private static Map<Signature, Field> signatureToFieldCache = new HashMap<Signature, Field>();

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

    // Track static initialization of ITD types
    after() :
        staticinitialization(ClusteredStateMarker+) {
        Class type = thisJoinPoint.getSignature().getDeclaringType();
        if (null == trackerFactory) {
            log.warn("trackerFactory is not set. Class [" + type + "] will not be indexed.");
            return;
        }
        trackerFactory.prepareTrackerForClass(type);
    }
  
    // Track instance construction
    pointcut instanceConstruction(ClusteredStateMarker stateMarker):
        execution(ClusteredStateMarker+.new(..)) && target(stateMarker);

    before(ClusteredStateMarker stateMarker):
        instanceConstruction(stateMarker) {
        Signature signature = thisJoinPointStaticPart.getSignature();
        Constructor constructor = ((ConstructorSignature) signature).getConstructor();
        
        if (null == trackerFactory) {
            throw new IllegalStateException("trackerFactory not set.");
        }
        if (null == stateMarker.tracker) {
            stateMarker.tracker =  trackerFactory.newInstanceTracker(stateMarker);
        }

        if (constructor.getDeclaringClass().equals(stateMarker.getClass())) {
            stateMarker.tracker.track(nextIndex(), constructor, thisJoinPoint.getArgs());
        }
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
        if (null == field) {
            field = deriveField(stateMarker, signature);
        }
        
        stateMarker.tracker.track(nextIndex(), field, value);
    }
    
    after(ClusteredState clusteredState, ClusteredStateMarker stateMarker, Object value):
        targetAnnotatedWithClusteredState(clusteredState, stateMarker) &&
        set(!transient * ClusteredStateMarker+.*) &&
        args(value) {
        Signature signature = thisJoinPointStaticPart.getSignature();
        Field field = ((FieldSignature) signature).getField();
        if (null == field) {
            field = deriveField(stateMarker, signature);
        }

        stateMarker.tracker.recordFieldUpdate(field, value);
    }

    private Field deriveField(ClusteredStateMarker stateMarker, Signature signature) {
        Field field;
        synchronized (signatureToFieldCache) {
            field = signatureToFieldCache.get(signature);
        }
        if (null != field) {
            return field;
        }
        Class declaringType = signature.getDeclaringType().getSuperclass();
        while (null != declaringType && null == field) {
            try {
                field = declaringType.getDeclaredField(signature.getName());
            } catch (Exception e) {
            }
        }
        if (null == field) {
            throw new AssertionError("Cannot identify updated Field. Signature [" + signature + "].");
        }
        synchronized (signatureToFieldCache) {
            signatureToFieldCache.put(signature, field);
        }
        return field;
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
