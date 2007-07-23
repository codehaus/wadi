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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.codehaus.wadi.aop.ClusteredStateMarker;
import org.codehaus.wadi.aop.tracker.InstanceTracker;
import org.codehaus.wadi.aop.tracker.InstanceTrackerVisitor;
import org.codehaus.wadi.aop.tracker.NoOpInstanceTrackerVisitor;
import org.codehaus.wadi.aop.tracker.basic.FieldInfo;
import org.codehaus.wadi.aop.tracker.basic.ValueUpdaterInfo;

/**
 * 
 * @version $Revision: 1538 $
 */
public class CopyFullStateVisitor extends CopyStateVisitor {
    
    public CopyFullStateVisitor(InstanceTrackerVisitor setInstanceIdVisitor) {
        super(setInstanceIdVisitor, NoOpInstanceTrackerVisitor.SINGLETON);
    }

    protected List<ValueUpdaterInfo> buildValueUpdaterInfos(InstanceTracker instanceTracker) {
        ClusteredStateMarker instance = instanceTracker.getInstance();
        Map<Field, Object> fieldValues = instance.$wadiGetFieldValues();
        
        List<ValueUpdaterInfo> valueUpdaterInfos = new ArrayList<ValueUpdaterInfo>();
        for (Map.Entry<Field, Object> entry : fieldValues.entrySet()) {
            valueUpdaterInfos.add(new ValueUpdaterInfo(new FieldInfo(entry.getKey()), new Object[] {entry.getValue()}));
        }
        
        return valueUpdaterInfos;
    }
    
}
