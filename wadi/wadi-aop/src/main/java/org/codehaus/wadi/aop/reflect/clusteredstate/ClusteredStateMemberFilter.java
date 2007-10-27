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
package org.codehaus.wadi.aop.reflect.clusteredstate;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.codehaus.wadi.aop.annotation.ClusteredState;
import org.codehaus.wadi.aop.annotation.TrackedMethod;
import org.codehaus.wadi.aop.annotation.TrackingLevel;
import org.codehaus.wadi.aop.reflect.base.ConstructorComparator;
import org.codehaus.wadi.aop.reflect.base.FieldComparator;
import org.codehaus.wadi.aop.reflect.base.MemberFilter;
import org.codehaus.wadi.aop.reflect.base.MethodComparator;




/**
 * 
 * @version $Revision: 1538 $
 */
public class ClusteredStateMemberFilter implements MemberFilter  {

    public Field[] filterFields(Class clazz) {
        List<Class> classInheritence = identifyClassInheritence(clazz);

        List<Field> allFields = new ArrayList<Field>();
        for (Class annotatedClazz : classInheritence) {
            Field[] fields = annotatedClazz.getDeclaredFields();
            Set<Field> nonAspectJFields = new TreeSet<Field>(new FieldComparator());
            for (Field field : fields) {
                if (field.getName().startsWith("ajc$") || Modifier.isTransient(field.getModifiers())) {
                    continue;
                }
                nonAspectJFields.add(field);
            }
            allFields.addAll(nonAspectJFields);
        }
        return allFields.toArray(new Field[allFields.size()]);
    }

    public Method[] filterMethods(Class clazz) {
        List<Class> classInheritence = identifyClassInheritence(clazz);

        List<Method> allTrackedMethods = new ArrayList<Method>();
        for (Class annotatedClazz : classInheritence) {
            ClusteredState clusteredState = (ClusteredState) annotatedClazz.getAnnotation(ClusteredState.class);
            if (clusteredState.trackingLevel() != TrackingLevel.METHOD && clusteredState.trackingLevel() != TrackingLevel.MIXED) {
                continue;
            }
            Method[] methods = annotatedClazz.getDeclaredMethods();
            Set<Method> trackedMethods = new TreeSet<Method>(new MethodComparator());
            for (Method method : methods) {
                if (method.isAnnotationPresent(TrackedMethod.class)) {
                    trackedMethods.add(method);
                }
            }
            allTrackedMethods.addAll(trackedMethods);
        }
        return allTrackedMethods.toArray(new Method[allTrackedMethods.size()]);
    }

    public Constructor[] filterConstructor(Class clazz) {
        Constructor[] constructors = clazz.getDeclaredConstructors();
        Arrays.sort(constructors, new ConstructorComparator());
        return constructors;
    }

    protected List<Class> identifyClassInheritence(Class clazz) {
        List<Class> annotatedClasses = new ArrayList<Class>();
        while (true) {
            ClusteredState clusteredState = (ClusteredState) clazz.getAnnotation(ClusteredState.class);
            if (null == clusteredState) {
                break;
            }
            annotatedClasses.add(clazz);
            clazz = clazz.getSuperclass();
        }
        return annotatedClasses;
    }
    
}
