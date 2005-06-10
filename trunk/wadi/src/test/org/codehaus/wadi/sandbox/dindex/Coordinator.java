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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;

import org.activecluster.Cluster;
import org.activecluster.Node;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.impl.TimeoutableInt;

import EDU.oswego.cs.dl.util.concurrent.Rendezvous;
import EDU.oswego.cs.dl.util.concurrent.Slot;
import EDU.oswego.cs.dl.util.concurrent.TimeoutException;
import EDU.oswego.cs.dl.util.concurrent.WaitableBoolean;
import EDU.oswego.cs.dl.util.concurrent.WaitableInt;

//it's important that the Plan is constructed from snapshotted resources (i.e. the ground doesn't
//shift under its feet), and that it is made and executed as quickly as possible - as a node could 
//leave the Cluster in the meantime...

public class Coordinator implements Runnable {
    
    protected final Log _log=LogFactory.getLog(getClass());
    
    protected final Slot _flag=new Slot();
    protected final Accumulator _leavers=new Accumulator();
    
    protected final CoordinatorConfig _config;
    protected final Cluster _cluster;
    protected final Node _localNode;
    protected final int _numItems;
    
    public Coordinator(CoordinatorConfig config) {
        _config=config;
        _cluster=_config.getCluster();
        _localNode=_cluster.getLocalNode();
        _numItems=_config.getNumItems();
    }
    
    protected Thread _thread;
    protected Node[] _remoteNodes;
    
    
    public synchronized void start() throws Exception {
        _log.info("starting...");
        _thread=new Thread(this, "WADI Coordinator");
        _thread.start();        
        _log.info("...started");
    }
    
    public synchronized void stop() throws Exception {
        // somehow wake up thread
        _log.info("stopping...");
        _flag.put(Boolean.FALSE);
        _thread.join();
        _thread=null;
        _log.info("...stopped");
    }
    
    public synchronized void queueRebalancing() {
        _log.info("queueing rebalancing...");
        try {
            _flag.offer(Boolean.TRUE, 0);
        } catch (InterruptedException e) {
            _log.warn("unexpected interruption");
        }
        _log.info("...rebalancing queued");
    }
    
    public synchronized void queueLeaving(Node node) {
        _leavers.put(node);
        queueRebalancing();
    }
    
    public void run() {
        try {
            while (_flag.take()==Boolean.TRUE) {
                rebalanceClusterState();
            }
        } catch (InterruptedException e) {
            Thread.interrupted();
            _log.info("interrupted"); // hmmm.... - TODO
        }
    }
    
    // should loop until it gets a successful outcome - emptying _flag each time...
    public void rebalanceClusterState() {
        Collection excludedNodes=_leavers.take();
        int numParticipants=0;
        String correlationId;
        
        Plan plan=null;
        if (excludedNodes.size()>0) {
            // a node wants to leave - evacuate it
            Node leaver=(Node)excludedNodes.iterator().next(); // FIXME - should be leavers...
            plan=new EvacuationPlan(leaver, _localNode, _remoteNodes, _numItems);
            numParticipants=_remoteNodes.length; // TODO - I think
            correlationId=_localNode.getName()+"-rebalance-"+System.currentTimeMillis();
        } else {
            // standard rebalance...
            _remoteNodes=_config.getRemoteNodes(); // snapshot this at last possible moment...
            plan=new RebalancingPlan(_localNode, _remoteNodes, _numItems);
            numParticipants=_remoteNodes.length+1;
            correlationId=_localNode.getName()+"-leaving-"+System.currentTimeMillis();
        }
        
        Map rvMap=_config.getRendezVousMap();
        TimeoutableInt rv=new TimeoutableInt(numParticipants-1);
        _log.info("SETTING UP RV FOR "+numParticipants+" PARTICIPANTS");
        rvMap.put(correlationId, rv);
        
        execute(plan, correlationId);
        
        boolean success=false;
        try {
            rv.whenEqual(0, 5000); // unify with cluster heartbeat or parameterise - TODO
            _log.info("RENDEZVOUS SUCCESSFUL");
            Collection results=rv.getResults();
            success=true;
        } catch (TimeoutException e) {
            _log.warn("timed out waiting for response", e);
        } catch (InterruptedException e) {
            _log.warn("unexpected interruption", e);
        } finally {
            rvMap.remove(correlationId);
            // somehow check all returned success..
        }
        
        // TODO - loop
        printNodes(_localNode, _remoteNodes);
        
    }
    
    protected void printNodes(Node localNode, Node[] remoteNodes) {
        _log.info(DIndexNode.getNodeName(localNode)+" : "+DIndexNode.getNumIndexPartitions(localNode));
        
        int n=remoteNodes.length;
        for (int i=0; i<n; i++) {
            Node remoteNode=remoteNodes[i];
            _log.info(DIndexNode.getNodeName(remoteNode)+" : "+DIndexNode.getNumIndexPartitions(remoteNode));
        }
    }
    
    protected void execute(Plan plan, String correlationId) {
        Iterator p=plan.getProducers().iterator();
        Iterator c=plan.getConsumers().iterator();
        
        Pair consumer=null;
        while (p.hasNext()) {
            Pair producer=(Pair)p.next();
            while (producer._deviation>0) {
                if (consumer==null)
                    consumer=c.hasNext()?(Pair)c.next():null;
                    if (producer._deviation>=consumer._deviation) {
                        balance(producer._node, consumer._node, consumer._deviation, correlationId);
                        producer._deviation-=consumer._deviation;
                        consumer._deviation=0;
                        consumer=null;
                    } else {
                        balance(producer._node, consumer._node, producer._deviation, correlationId);
                        consumer._deviation-=producer._deviation;
                        producer._deviation=0;
                    }
            }
        }
    }
    
    public boolean balance(Node src, Node tgt, int amount, String correlationId) {
        /*if (src==_cluster.getLocalNode()) {
         // do something clever...
          } else*/ {
              _log.info("commanding "+DIndexNode.getNodeName(src)+" to give "+amount+" to "+DIndexNode.getNodeName(tgt));
              IndexPartitionsTransferCommand command=new IndexPartitionsTransferCommand(amount, tgt.getDestination());
              try {
                  ObjectMessage om=_cluster.createObjectMessage();
                  om.setJMSReplyTo(_cluster.getLocalNode().getDestination());
                  om.setJMSDestination(src.getDestination());
                  om.setJMSCorrelationID(correlationId);
                  om.setObject(command);
                  _cluster.send(src.getDestination(), om);
              } catch (JMSException e) {
                  _log.error("problem sending share command", e);
              }
          }
          return true;
    }
    
}
