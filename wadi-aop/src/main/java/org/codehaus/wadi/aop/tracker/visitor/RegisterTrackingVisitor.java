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
package org.codehaus.wadi.aop.tracker.visitor;

import org.codehaus.wadi.aop.tracker.InstanceRegistry;
import org.codehaus.wadi.aop.tracker.InstanceTracker;
import org.codehaus.wadi.aop.tracker.VisitorContext;

/**
 * 
 * @version $Revision: 1538 $
 */
public class RegisterTrackingVisitor extends AbstractVisitor {

    private final InstanceRegistry instanceRegistry;

    public RegisterTrackingVisitor(InstanceRegistry instanceRegistry) {
        if (null == instanceRegistry) {
            throw new IllegalArgumentException("instanceRegistry is required");
        }
        this.instanceRegistry = instanceRegistry;
    }
    
    public void visit(InstanceTracker instanceTracker, VisitorContext context) {
        String instanceId = instanceTracker.getInstanceId();
        instanceRegistry.registerInstance(instanceId, instanceTracker.getInstance());
    }
    
}
