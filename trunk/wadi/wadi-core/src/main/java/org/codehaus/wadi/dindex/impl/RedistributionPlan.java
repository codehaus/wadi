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
package org.codehaus.wadi.dindex.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.group.Peer;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class RedistributionPlan {

    protected final Log _log=LogFactory.getLog(getClass());
    protected final List _producers = new ArrayList();
    protected final List _consumers = new ArrayList();

    public RedistributionPlan(Peer[] living, Peer[] leaving, int totalNumPartitions) {
        int numPartitionsPerNode=totalNumPartitions/living.length;

        for (int i=0; i<leaving.length; i++) {
            Peer node=leaving[i];
            int numPartitions=DIndex.getPartitionKeys(node).size();
//            _log.info("LEAVING: "+numPartitions);
            if (numPartitions>0)
                _producers.add(new PartitionOwner(node, numPartitions, true));
        }

        for (int i=0; i<living.length; i++) {
            Peer node=living[i];
            int numPartitions=DIndex.getPartitionKeys(node).size();
//            _log.info("LIVING: "+numPartitions);
            decide(node, numPartitions, numPartitionsPerNode, _producers, _consumers);
        }

        // sort lists...
        Collections.sort(_producers, new PartitionOwnerGreaterThanComparator());
        Collections.sort(_consumers, new PartitionOwnerLessThanComparator());

        // account for uneven division of partitions...
        int remainingPartitions=totalNumPartitions%living.length;

        for (ListIterator i=_producers.listIterator(); remainingPartitions>0 && i.hasNext(); ) {
            PartitionOwner p=(PartitionOwner)i.next();
            if (!p._leaving) {
                remainingPartitions--;
                if ((--p._deviation)==0)
                    i.remove();
            }
        }

        for (ListIterator i=_consumers.listIterator(); remainingPartitions>0 && i.hasNext(); ) {
            PartitionOwner p=(PartitionOwner)i.next();
            remainingPartitions--;
            ++p._deviation;
        }

        assert (remainingPartitions==0);
    }

    protected void decide(Peer node, int numPartitions, int numPartitionsPerNode, Collection producers, Collection consumers) {
        int deviation=numPartitions-numPartitionsPerNode;
//        _log.info("DEVIATION: "+deviation);
        if (deviation>0) {
            producers.add(new PartitionOwner(node, deviation, false));
            return;
        }
        if (deviation<0) {
            consumers.add(new PartitionOwner(node, -deviation, false));
            return;
        }
    }

    public Collection getProducers() {
        return _producers;
    }

    public Collection getConsumers() {
        return _consumers;
    }


}
