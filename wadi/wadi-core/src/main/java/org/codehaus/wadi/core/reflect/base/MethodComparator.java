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
package org.codehaus.wadi.core.reflect.base;

import java.lang.reflect.Method;
import java.util.Comparator;


/**
 * 
 * @version $Revision: 1538 $
 */
public class MethodComparator implements Comparator<Method> {
    private final Comparator<Class[]> classComparator;

    public MethodComparator() {
        this(new ArrayClassComparator());
    }
    
    public MethodComparator(Comparator<Class[]> classComparator) {
        if (null == classComparator) {
            throw new IllegalArgumentException("classComparator is required");
        }
        this.classComparator = classComparator;
    }

    public int compare(Method method1, Method method2) {
        String name1 = method1.getName();
        String name2 = method2.getName();
        if (0 != name1.compareTo(name2)) {
            return name1.compareTo(name2);
        }
        
        Class[] parameterTypes1 = method1.getParameterTypes();
        Class[] parameterTypes2 = method2.getParameterTypes();
        int compared = classComparator.compare(parameterTypes1, parameterTypes2);
        if (0 == compared) {
            throw new AssertionError("Shoud never been thrown");
        }
        return compared;
    }

}