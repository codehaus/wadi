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
package org.codehaus.wadi.aop.util;

import org.codehaus.wadi.aop.ClusteredStateMarker;
import org.codehaus.wadi.aop.tracker.InstanceIdFactory;
import org.codehaus.wadi.aop.tracker.InstanceRegistry;
import org.codehaus.wadi.aop.tracker.InstanceTrackerVisitor;
import org.codehaus.wadi.aop.tracker.VisitorContext;
import org.codehaus.wadi.aop.tracker.basic.ValueUpdaterInfo;
import org.codehaus.wadi.aop.tracker.basic.WireMarshaller;
import org.codehaus.wadi.aop.tracker.visitor.CopyFullStateVisitor;
import org.codehaus.wadi.aop.tracker.visitor.CopyStateVisitor;
import org.codehaus.wadi.aop.tracker.visitor.RegisterTrackingVisitor;
import org.codehaus.wadi.aop.tracker.visitor.ResetTrackingVisitor;
import org.codehaus.wadi.aop.tracker.visitor.SetInstanceIdVisitor;
import org.codehaus.wadi.aop.tracker.visitor.UnregisterTrackingVisitor;
import org.codehaus.wadi.aop.tracker.visitor.CopyStateVisitor.CopyStateVisitorContext;


/**
 * 
 * @version $Revision: 1538 $
 */
public final class ClusteredStateHelper {
    
    private ClusteredStateHelper() {
    }

    public static void resetTracker(Object opaque) {
        visit(opaque, ResetTrackingVisitor.SINGLETON, true);
    }

    public static void unregisterTracker(InstanceRegistry instanceRegistry, Object opaque) {
        visit(opaque, new UnregisterTrackingVisitor(instanceRegistry), true);
    }

    public static void registerTracker(InstanceRegistry instanceRegistry, Object opaque) {
        visit(opaque, new RegisterTrackingVisitor(instanceRegistry), true);
    }
    
    public static byte[] serializeFully(InstanceIdFactory instanceIdFactory, WireMarshaller marshaller, Object opaque) {
        CopyFullStateVisitor visitor = new CopyFullStateVisitor(marshaller, new SetInstanceIdVisitor(instanceIdFactory));
        CopyStateVisitorContext context = (CopyStateVisitorContext) visit(opaque, visitor, false);
        return context.getSerializedValueUpdaterInfos();
    }

    public static byte[] serialize(InstanceIdFactory instanceIdFactory, WireMarshaller marshaller, Object opaque) {
        CopyStateVisitor visitor = new CopyStateVisitor(marshaller,
            new SetInstanceIdVisitor(instanceIdFactory),
            ResetTrackingVisitor.SINGLETON);
        CopyStateVisitorContext context = (CopyStateVisitorContext) visit(opaque, visitor, false);
        return context.getSerializedValueUpdaterInfos();
    }
    
    protected static VisitorContext visit(Object opaque, InstanceTrackerVisitor visitor, boolean visitTracker) {
        ClusteredStateMarker stateMarker = castAndEnsureType(opaque);

        VisitorContext context = visitor.newContext();
        if (visitTracker) {
            stateMarker.$wadiGetTracker().visit(visitor, context);
        } else {
            visitor.visit(stateMarker.$wadiGetTracker(), context);
        }
        return context;
    }

    public static void deserialize(InstanceRegistry instanceRegistry, WireMarshaller marshaller, byte[] serialized) {
        ValueUpdaterInfo[] valueUpdaterInfos = marshaller.unmarshall(serialized);
        ValueUpdaterInfo.applyTo(instanceRegistry, valueUpdaterInfos);
    }
    
    public static ClusteredStateMarker castAndEnsureType(Object opaque) {
        if (!(opaque instanceof ClusteredStateMarker)) {
            throw new IllegalArgumentException(opaque.getClass() + " is not a ClusteredStateMarker. " +
                    "The class may not have been weaved.");
        }
        return (ClusteredStateMarker) opaque;
    }

}
