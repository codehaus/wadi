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
package org.codehaus.wadi.aop.tracker;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 
 * @version $Revision: 1538 $
 */
public interface InstanceTracker extends Serializable {
    String getInstanceId();
    
    void setInstanceId(String instanceId);
    
    void visit(InstanceTrackerVisitor visitor, VisitorContext context);
    
    void applyTo(InstanceRegistry instanceRegistry);
    
    void track(long index, Constructor constructor, Object[] parameters);
    
    void track(long index, Field field, Object value);
    
    void track(long index, Method method, Object[] parameters);
    
    void resetTracking();
}
