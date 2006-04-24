/**
 * Copyright 2006 The Apache Software Foundation
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
package org.codehaus.wadi.replication.storage.remoting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.jms.ObjectMessage;

import org.apache.activecluster.ClusterEvent;
import org.apache.activecluster.ClusterListener;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.replication.common.ComponentEventType;
import org.codehaus.wadi.replication.common.NodeInfo;


/**
 *
 * @version $Revision$
 */
public class BasicReplicaStorageMonitor implements ReplicaStorageMonitor, ClusterListener {
    private final Set storageNodes;
    private final Set listeners;
    private final Dispatcher dispatcher;

    public BasicReplicaStorageMonitor(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;

        storageNodes = new HashSet();
        listeners = new HashSet();
    }

    public void start() {
        dispatcher.register(this, "onReplicaStorageEvent", ReplicaStorageEvent.class);
        dispatcher.setClusterListener(this);

        String localNodeName = dispatcher.getLocalNodeName();
        NodeInfo localNode = new NodeInfo(localNodeName);
        ReplicaStorageMonitorEvent event = new ReplicaStorageMonitorEvent(ComponentEventType.JOIN, localNode);
        try {
            dispatcher.send(dispatcher.getClusterDestination(), event);
        } catch (Exception e) {
            // TODO
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        // TODO refactor WADI to get ride of this timeout.
        dispatcher.deregister("onReplicaStorageEvent", ReplicaStorageEvent.class, 2);
        // TODO Enhance WADI to support unregistration of ClusterListener.

        storageNodes.clear();
    }

    public void onReplicaStorageEvent(ObjectMessage message, ReplicaStorageEvent event) {
        ComponentEventType type = event.getType();
        NodeInfo hostingNode = event.getHostingNode();
        synchronized (storageNodes) {
            if (type == ComponentEventType.JOIN) {
                boolean found = storageNodes.add(hostingNode);
                if (false == found) {
                    throw new IllegalStateException(hostingNode +
                            " is already registered.");
                }
            } else if (type == ComponentEventType.LEAVE) {
                boolean found = storageNodes.remove(hostingNode);
                if (false == found) {
                    throw new IllegalStateException(hostingNode +
                            " is not registered.");
                }
            } else {
                throw new AssertionError(type + " is not supported.");
            }
        }
        fireEvent(event);
    }

    public void addReplicaStorageListener(ReplicaStorageListener listener) {
        synchronized (listeners) {
            synchronized (storageNodes) {
                NodeInfo[] storages = (NodeInfo[]) storageNodes.toArray(new NodeInfo[0]);
                listener.initNodes(storages);
            }
            listeners.add(listener);
        }
    }

    public void removeReplicaStorageListener(ReplicaStorageListener listener) {
        synchronized (listeners) {
            boolean found = listeners.remove(listener);
            if (false == found) {
                throw new IllegalArgumentException("Specified listener is not " +
                        "registered.");
            }
        }
    }

    public void onNodeAdd(ClusterEvent event) {
    }

    public void onNodeUpdate(ClusterEvent event) {
    }

    public void onNodeRemoved(ClusterEvent event) {
        fireLeaveEvent(event);
    }

    public void onNodeFailed(ClusterEvent event) {
        fireLeaveEvent(event);
    }

    public void onCoordinatorChanged(ClusterEvent event) {
    }

    private void fireEvent(ReplicaStorageEvent event) {
        Collection copyListeners;
        synchronized (listeners) {
            copyListeners = new ArrayList(listeners);
        }
        ComponentEventType type = event.getType();
        NodeInfo hostingNode = event.getHostingNode();
        for (Iterator iter = copyListeners.iterator(); iter.hasNext();) {
            ReplicaStorageListener listener = (ReplicaStorageListener) iter.next();
            if (type == ComponentEventType.JOIN) {
                listener.fireJoin(hostingNode);
            } else if (type == ComponentEventType.LEAVE) {
                listener.fireLeave(hostingNode);
            } else {
                throw new AssertionError(type + " is not supported.");
            }
        }
    }

    private void fireLeaveEvent(ClusterEvent event) {
        NodeInfo nodeInfo = new NodeInfo(event.getNode().getName());
        synchronized (storageNodes) {
            boolean found = storageNodes.remove(nodeInfo);
            if (false == found) {
                return;
            }
        }
        fireEvent(new ReplicaStorageEvent(ComponentEventType.LEAVE, nodeInfo));
    }
}
