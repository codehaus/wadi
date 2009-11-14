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

import java.lang.reflect.Constructor;
import java.util.Comparator;


/**
 * 
 * @version $Revision: 1538 $
 */
public class ConstructorComparator implements Comparator<Constructor> {
    private final Comparator classComparator;

    public ConstructorComparator() {
        this(new ArrayClassComparator());
    }

    public ConstructorComparator(Comparator<Class[]> classComparator) {
        if (null == classComparator) {
            throw new IllegalArgumentException("classComparator is required");
        }
        this.classComparator = classComparator;
    }
    
    public int compare(Constructor constructor1, Constructor constructor2) {
        Class[] parameterTypes1 = constructor1.getParameterTypes();
        Class[] parameterTypes2 = constructor2.getParameterTypes();
        return classComparator.compare(parameterTypes1, parameterTypes2);
    }

}