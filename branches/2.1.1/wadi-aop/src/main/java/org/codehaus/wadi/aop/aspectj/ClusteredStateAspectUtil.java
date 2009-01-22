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

import java.lang.reflect.Field;

import org.codehaus.wadi.aop.tracker.InstanceTrackerFactory;


/**
 * 
 * @version $Revision: 1538 $
 */
public class ClusteredStateAspectUtil {

    public static synchronized void setInstanceTrackerFactory(InstanceTrackerFactory trackerFactory) {
        setInstanceTrackerFactory(ClusteredStateAspectUtil.class.getClassLoader(), trackerFactory);
    }
    
    public static synchronized void setInstanceTrackerFactory(ClassLoader classLoader, InstanceTrackerFactory trackerFactory) {
        Field trackerFactoryField = getTrackerFactoryField(classLoader);
        try {
            Object isSet = trackerFactoryField.get(null);
            if (null != isSet) {
                return;
            }
            
            trackerFactoryField.set(null, trackerFactory);
        } catch (Exception e) {
            throw (AssertionError) new AssertionError("See nested").initCause(e);
        }
    }
    
    public static synchronized void resetInstanceTrackerFactory() {
        resetInstanceTrackerFactory(ClusteredStateAspectUtil.class.getClassLoader());
    }

    public static synchronized void resetInstanceTrackerFactory(ClassLoader classLoader) {
        Field trackerFactoryField = getTrackerFactoryField(classLoader);
        try {
            trackerFactoryField.set(null, null);
            
            Class aspectClass = getAspectClass(classLoader);
            Field indexField = aspectClass.getField("index");
            indexField.set(null, 0);
        } catch (Exception e) {
            throw (AssertionError) new AssertionError("See nested").initCause(e);
        }
    }
    
    protected static Field getTrackerFactoryField(ClassLoader classLoader) {
        Class aspectClass = getAspectClass(classLoader);
        try {
            return aspectClass.getField("trackerFactory");
        } catch (Exception e) {
            throw (AssertionError) new AssertionError("See nested").initCause(e);
        }
    }
    
    protected static Class getAspectClass(ClassLoader classLoader) {
        try {
            return classLoader.loadClass("org.codehaus.wadi.aop.aspectj.ClusteredStateAspect");
        } catch (Exception e) {
            throw (AssertionError) new AssertionError("See nested").initCause(e);
        }
    }
    
}
