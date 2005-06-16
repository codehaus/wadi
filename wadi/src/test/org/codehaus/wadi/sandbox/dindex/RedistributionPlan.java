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
package org.codehaus.wadi.sandbox.dindex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import org.activecluster.Node;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class RedistributionPlan {
    
    protected final Log _log=LogFactory.getLog(getClass());
    protected final List _producers = new ArrayList();
    protected final List _consumers = new ArrayList();
    
    public RedistributionPlan(Node[] living, Node[] leaving, int totalNumBuckets) {
        int numBucketsPerNode=totalNumBuckets/living.length;

        for (int i=0; i<leaving.length; i++) {
            Node node=leaving[i];
            int numBuckets=DIndex.getBucketKeys(node).size();
//            _log.info("LEAVING: "+numBuckets);
            if (numBuckets>0)
                _producers.add(new BucketOwner(node, numBuckets, true));
        }

        for (int i=0; i<living.length; i++) {
            Node node=living[i];
            int numBuckets=DIndex.getBucketKeys(node).size();
//            _log.info("LIVING: "+numBuckets);
            decide(node, numBuckets, numBucketsPerNode, _producers, _consumers);
        }

        // sort lists...
        Collections.sort(_producers, new BucketOwnerGreaterThanComparator());
        Collections.sort(_consumers, new BucketOwnerLessThanComparator());

        // account for uneven division of buckets...
        int remainingBuckets=totalNumBuckets%living.length;

        for (ListIterator i=_producers.listIterator(); remainingBuckets>0 && i.hasNext(); ) {
            BucketOwner p=(BucketOwner)i.next();
            if (!p._leaving) {
                remainingBuckets--;
                if ((--p._deviation)==0)
                    i.remove();
            }
        }

        for (ListIterator i=_consumers.listIterator(); remainingBuckets>0 && i.hasNext(); ) {
            BucketOwner p=(BucketOwner)i.next();
            remainingBuckets--;
            ++p._deviation;
        }

        assert remainingBuckets==0;
    }

    protected void decide(Node node, int numBuckets, int numBucketsPerNode, Collection producers, Collection consumers) {
        int deviation=numBuckets-numBucketsPerNode;
//        _log.info("DEVIATION: "+deviation);
        if (deviation>0) {
            producers.add(new BucketOwner(node, deviation, false));
            return;
        }
        if (deviation<0) {
            consumers.add(new BucketOwner(node, -deviation, false));
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
