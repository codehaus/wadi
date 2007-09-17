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
package org.codehaus.wadi.aop.replication;

import java.util.Map;

import org.codehaus.wadi.aop.annotation.ClusteredState;
import org.codehaus.wadi.aop.util.TrackedMap;
import org.codehaus.wadi.core.session.DistributableAttributesMemento;

/**
 * 
 * @version $Revision: 1538 $
 */
@ClusteredState
public class ClusteredStateAttributesMemento extends DistributableAttributesMemento {
    protected Map<Object, Object> attributes;

    public ClusteredStateAttributesMemento() {
        attributes = super.attributes;
    }
    
    @Override
    protected Map<Object,Object> newAttributesMap() {
        TrackedMap trackedMap = new TrackedMap();
        trackedMap.setDelegate(super.newAttributesMap());
        return trackedMap;
    }
    
    public void onRestore() {
        super.attributes = attributes;
    }
    
}
