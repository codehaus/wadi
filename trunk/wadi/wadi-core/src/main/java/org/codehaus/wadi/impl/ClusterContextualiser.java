/**
 *
 * Copyright 2003-2005 Core Developers Network Ltd.
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
package org.codehaus.wadi.impl;

import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.Emoter;
import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.Invocation;
import org.codehaus.wadi.InvocationException;
import org.codehaus.wadi.Motable;
import org.codehaus.wadi.Relocater;
import org.codehaus.wadi.location.PartitionManager;
import org.codehaus.wadi.location.StateManager;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class ClusterContextualiser extends AbstractSharedContextualiser {
    private final Relocater relocater;
    private final PartitionManager partitionManager;
    private final StateManager stateManager;
    private final SynchronizedBoolean shuttingDown;
    private final Immoter immoter;
    private final Emoter emoter;

	public ClusterContextualiser(Contextualiser next,
            Relocater relocater,
            PartitionManager partitionManager,
            StateManager stateManager,
            SynchronizedBoolean shuttingDown) {
		super(next);
        if (null == relocater) {
            throw new IllegalArgumentException("relocater is required");
        } else if (null == partitionManager) {
            throw new IllegalArgumentException("partitionManager is required");
        } else if (null == stateManager) {
            throw new IllegalArgumentException("stateManager is required");
        } else if (null == shuttingDown) {
            throw new IllegalArgumentException("shuttingDown is required");
        }
        this.relocater = relocater;
        this.partitionManager = partitionManager;
        this.stateManager = stateManager;
        this.shuttingDown = shuttingDown;
        
        immoter = new EmigrationImmoter();
        emoter = null;
	}

    public Immoter getImmoter() {
        return immoter;
    }

    public Emoter getEmoter() {
        return emoter;
    }

    public Immoter getDemoter(String name, Motable motable) {
        // how many partitions are we responsible for ?
        if (partitionManager.getBalancingInfo().getNumberOfLocalPartitionInfos() == 0) {
            // evacuate sessions to their respective partition masters...
            return getImmoter();
        } else {
            return next.getDemoter(name, motable);
        }
    }

    public Immoter getSharedDemoter() {
        // how many partitions are we responsible for ?
        if (partitionManager.getBalancingInfo().getNumberOfLocalPartitionInfos() == 0) {
            // evacuate sessions to their respective partition masters...
            return getImmoter();
        } else {
            // we must be the last node, push the sessions downwards into shared
            // store...
            return next.getSharedDemoter();
        }
    }

    public boolean handle(Invocation invocation, String id, Immoter immoter, boolean exclusiveOnly)
            throws InvocationException {
        return relocater.relocate(invocation, id, immoter, shuttingDown.get());
    }

    public void load(Emoter emoter, Immoter immoter) {
    }

    protected Motable get(String id, boolean exclusiveOnly) {
        throw new UnsupportedOperationException();
    }

    /**
     * Manage the immotion of a session into the cluster tier from another and
     * its emigration thence to another node.
     */
    class EmigrationImmoter implements Immoter {
        public Motable newMotable() {
            return new SimpleMotable();
        }

        public boolean immote(Motable emotable, Motable immotable) {
            try {
                immotable.copy(emotable);
            } catch (Exception e) {
                _log.warn("problem sending emigration request for [" + emotable + "]", e);
                return false;
            }
            return stateManager.offerEmigrant(immotable);
        }

        public boolean contextualise(Invocation invocation, String id, Motable immotable)
                throws InvocationException {
            return false;
        }

    }

}
