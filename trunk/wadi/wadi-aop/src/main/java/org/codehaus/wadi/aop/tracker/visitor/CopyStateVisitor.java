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

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import org.codehaus.wadi.aop.tracker.InstanceTracker;
import org.codehaus.wadi.aop.tracker.InstanceTrackerException;
import org.codehaus.wadi.aop.tracker.InstanceTrackerVisitor;
import org.codehaus.wadi.aop.tracker.VisitorContext;
import org.codehaus.wadi.aop.tracker.basic.ValueUpdaterInfo;

/**
 * 
 * @version $Revision: 1538 $
 */
public class CopyStateVisitor extends AbstractVisitor {
    
    private final InstanceTrackerVisitor setInstanceIdVisitor;
    private final InstanceTrackerVisitor resetTrackingVisitor;
    
    public CopyStateVisitor(InstanceTrackerVisitor setInstanceIdVisitor, InstanceTrackerVisitor resetTrackingVisitor) {
        if (null == setInstanceIdVisitor) {
            throw new IllegalArgumentException("setInstanceIdVisitor is required");
        } else if (null == resetTrackingVisitor) {
            throw new IllegalArgumentException("resetTrackingVisitor is required");
        }
        this.setInstanceIdVisitor = setInstanceIdVisitor;
        this.resetTrackingVisitor = resetTrackingVisitor;
    }

    public CopyStateVisitorContext newContext() {
        return new CopyStateVisitorContext();
    }

    public void visit(InstanceTracker instanceTracker, VisitorContext context) {
        if (!(context instanceof CopyStateVisitorContext)) {
            throw new IllegalArgumentException("Context is a [" + context.getClass().getName() + "] expected ["
                    + CopyStateVisitorContext.class.getName() + "]");
        }
        CopyStateVisitorContext copyContext = (CopyStateVisitorContext) context;
        
        instanceTracker.visit(setInstanceIdVisitor, setInstanceIdVisitor.newContext());

        serializeValueUpdaterInfos(copyContext, instanceTracker);
        
        instanceTracker.visit(resetTrackingVisitor, resetTrackingVisitor.newContext());
    }

    protected void serializeValueUpdaterInfos(CopyStateVisitorContext copyContext, InstanceTracker instanceTracker) {
        ValueUpdaterInfo[] valueUpdaterInfos = buildValueUpdaterInfos(instanceTracker);
        
        ByteArrayOutputStream memOut = new ByteArrayOutputStream();
        try {
            ObjectOutputStream out = new ObjectOutputStream(memOut);
            out.writeObject(valueUpdaterInfos);
            out.close();
        } catch (Exception e) {
            throw new InstanceTrackerException(e);
        }
        copyContext.serializedValueUpdaterInfos = memOut.toByteArray();
    }

    protected ValueUpdaterInfo[] buildValueUpdaterInfos(InstanceTracker instanceTracker) {
        return instanceTracker.retrieveValueUpdaterInfos();
    }
    
    public static class CopyStateVisitorContext extends BaseVisitorContext {
        private byte[] serializedValueUpdaterInfos;

        public byte[] getSerializedValueUpdaterInfos() {
            return serializedValueUpdaterInfos;
        }
    }

}
