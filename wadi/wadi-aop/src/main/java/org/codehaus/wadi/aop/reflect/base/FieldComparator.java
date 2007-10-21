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
package org.codehaus.wadi.aop.reflect.base;

import java.lang.reflect.Field;
import java.util.Comparator;

/**
 * 
 * @version $Revision: 1538 $
 */
public class FieldComparator implements Comparator<Field> {
    
    public int compare(Field field1, Field field2) {
        return field1.getName().compareTo(field2.getName());
    }
    
}