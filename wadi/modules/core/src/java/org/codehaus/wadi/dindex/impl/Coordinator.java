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
import java.util.Iterator;
import java.util.Map;

import javax.jms.Destination;

import org.activecluster.Cluster;
import org.activecluster.Node;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.dindex.CoordinatorConfig;
import org.codehaus.wadi.gridstate.Dispatcher;
import org.codehaus.wadi.impl.Quipu;

import EDU.oswego.cs.dl.util.concurrent.Slot;
import EDU.oswego.cs.dl.util.concurrent.TimeoutException;

//it's important that the Plan is constructed from snapshotted resources (i.e. the ground doesn't
//shift under its feet), and that it is made and executed as quickly as possible - as a node could
//leave the Cluster in the meantime...

public class Coordinator implements Runnable {

    protected final Log _log=LogFactory.getLog(getClass());

    protected final Slot _flag=new Slot();

    protected final CoordinatorConfig _config;
    protected final Cluster _cluster;
    protected final Dispatcher _dispatcher;
    protected final Node _localNode;
    protected final int _numItems;
    protected final long _inactiveTime;

    public Coordinator(CoordinatorConfig config) {
        _config=config;
        _cluster=_config.getCluster();
        _dispatcher=_config.getDispatcher();
        _localNode=_cluster.getLocalNode();
        _numItems=_config.getNumPartitions();
        _inactiveTime=_config.getInactiveTime();
    }

    protected Thread _thread;
    protected Node[] _remoteNodes;


    public synchronized void start() throws Exception {
        if ( _log.isInfoEnabled() ) {

            _log.info("starting...");
        }
        _thread=new Thread(this, "WADI Coordinator");
        _thread.start();
        if ( _log.isInfoEnabled() ) {

            _log.info("...started");
        }
    }

    public synchronized void stop() throws Exception {
        // somehow wake up thread
        if ( _log.isInfoEnabled() ) {

            _log.info("stopping...");
        }
        _flag.put(Boolean.FALSE);
        _thread.join();
        _thread=null;
        if ( _log.isInfoEnabled() ) {

            _log.info("...stopped");
        }
    }

    public synchronized void queueRebalancing() {
        if ( _log.isInfoEnabled() ) {

            _log.info("queueing rebalancing...");
        }
        try {
            _flag.offer(Boolean.TRUE, 0);
        } catch (InterruptedException e) {
            _log.warn("unexpected interruption");
        }
        if ( _log.isInfoEnabled() ) {

            _log.info("...rebalancing queued");
        }
    }

    public void run() {
        try {
            while (_flag.take()==Boolean.TRUE) {
                rebalanceClusterState();
            }
        } catch (InterruptedException e) {
            Thread.interrupted();
            if ( _log.isInfoEnabled() ) {

                _log.info("interrupted"); // hmmm.... - TODO
            }
        }
    }

    public void rebalanceClusterState() {
    	int failures=0;
    	try {

    		Map nodeMap=_cluster.getNodes();

    		Collection stayingNodes=nodeMap.values();
    		synchronized (stayingNodes) {stayingNodes=new ArrayList(stayingNodes);} // snapshot
    		stayingNodes.add(_cluster.getLocalNode());

    		Collection l=_config.getLeavers();
    		synchronized (l) {l=new ArrayList(l);} // snapshot

    		Collection leavingNodes=new ArrayList();
    		for (Iterator i=l.iterator(); i.hasNext(); ) {
    			Destination d=(Destination)i.next();
    			Node leaver=getNode(d);
    			if (leaver!=null) {
    				leavingNodes.add(leaver);
    				stayingNodes.remove(leaver);
    			}
    		}

    		if ( _log.isInfoEnabled() ) {

                _log.info("--------");
                _log.info("STAYING:");
            }

    		printNodes(stayingNodes);
            if ( _log.isInfoEnabled() ) {

                _log.info("LEAVING:");
            }
    		printNodes(leavingNodes);
            if ( _log.isInfoEnabled() ) {

                _log.info("--------");
            }

    		Node [] leaving=(Node[])leavingNodes.toArray(new Node[leavingNodes.size()]);

    		if (stayingNodes.size()==0) {
    			_log.warn("we are the last node - no need to rebalance cluster");
    		} else {

    			Node [] living=(Node[])stayingNodes.toArray(new Node[stayingNodes.size()]);

    			_config.regenerateMissingPartitions(living, leaving);

    			RedistributionPlan plan=new RedistributionPlan(living, leaving, _numItems);

                if ( _log.isInfoEnabled() ) {

                    _log.info("--------");
                    _log.info("BEFORE:");
                }
    			printNodes(living, leaving);
                if ( _log.isInfoEnabled() ) {

                    _log.info("--------");
                }

    			Map rvMap=_config.getRendezVousMap();
    			Quipu rv=new Quipu(0);
    			String correlationId=_dispatcher.nextCorrelationId();
    			rvMap.put(correlationId, rv);
    			execute(plan, correlationId, rv); // quipu will be incremented as participants are invited

    			try {
                    if ( _log.isInfoEnabled() ) {

                        _log.info("WAITING ON RENDEZVOUS");
                    }
    				if (rv.waitFor(_inactiveTime)) {
                        if ( _log.isInfoEnabled() ) {

                            _log.info("RENDEZVOUS SUCCESSFUL");
                        }
    					//Collection results=rv.getResults();
    				} else {
    					_log.warn("RENDEZVOUS FAILED");
    					failures++;
    				}
    			} catch (TimeoutException e) {
    				_log.warn("timed out waiting for response", e);
    				failures++;
    			} catch (InterruptedException e) {
    				_log.warn("unexpected interruption", e);
    				failures++;
    			} finally {
    				rvMap.remove(correlationId);
    				// somehow check all returned success.. - TODO
    			}

                if ( _log.isInfoEnabled() ) {

                    _log.info("--------");
    			    _log.info("AFTER:");
                }
    			printNodes(living, leaving);
                if ( _log.isInfoEnabled() ) {

                    _log.info("--------");
                }
    		}

    		// send EvacuationResponses to each leaving node... - hmmm....
    		Collection left=_config.getLeft();
    		for (int i=0; i<leaving.length; i++) {
    			Node node=leaving[i];
    			if (!left.contains(node.getDestination())) {
    				PartitionEvacuationResponse response=new PartitionEvacuationResponse();
    				if (!_dispatcher.reply(_cluster.getLocalNode().getDestination(), node.getDestination(), node.getName(), response)) {
                        if ( _log.isErrorEnabled() ) {

                            _log.error("problem sending EvacuationResponse to " + DIndex.getNodeName(node));
                        }
    					failures++;
    				}
    				left.add(node.getDestination());
    			}
    		}

    	} catch (Throwable t) {
    		_log.warn("problem rebalancing indeces", t);
    		failures++;
    	}

    	if (failures>0) {
    		_log.warn("rebalance failed - backing off for "+_inactiveTime+" millis...");
    		queueRebalancing();
    	}
    }

    protected void execute(RedistributionPlan plan, String correlationId, Quipu quipu) {
        quipu.increment(); // add a safety margin of '1', so if we are caught up by acks, waiting thread does not finish
        Iterator p=plan.getProducers().iterator();
        Iterator c=plan.getConsumers().iterator();

        PartitionOwner consumer=null;
        while (p.hasNext()) {
            PartitionOwner producer=(PartitionOwner)p.next();
            Collection transfers=new ArrayList();
            while (producer._deviation>0) {
                if (consumer==null)
                    consumer=c.hasNext()?(PartitionOwner)c.next():null;
                    if (producer._deviation>=consumer._deviation) {
                        transfers.add(new PartitionTransfer(consumer._node.getDestination(), DIndex.getNodeName(consumer._node), consumer._deviation));
                        producer._deviation-=consumer._deviation;
                        consumer._deviation=0;
                        consumer=null;
                    } else {
                        transfers.add(new PartitionTransfer(consumer._node.getDestination(), DIndex.getNodeName(consumer._node), producer._deviation));
                        consumer._deviation-=producer._deviation;
                        producer._deviation=0;
                    }
            }

            PartitionTransferCommand command=new PartitionTransferCommand((PartitionTransfer[])transfers.toArray(new PartitionTransfer[transfers.size()]));
            quipu.increment();
            if (!_dispatcher.send(_cluster.getLocalNode().getDestination(), producer._node.getDestination(), correlationId, command)) {
                if ( _log.isErrorEnabled() ) {

                    _log.error("problem sending transfer command");
                }
            }
        }
        quipu.decrement(); // remove safety margin
    }

    protected int printNodes(Collection nodes) {
        int total=0;
        for (Iterator i=nodes.iterator(); i.hasNext(); )
            total+=printNode((Node)i.next());
        return total;
    }

    protected void printNodes(Node[] living, Node[] leaving) {
        int total=0;
        for (int i=0; i<living.length; i++)
            total+=printNode(living[i]);
        for (int i=0; i<leaving.length; i++)
            total+=printNode(leaving[i]);
        if ( _log.isInfoEnabled() ) {

            _log.info("TOTAL: " + total);
        }
    }

    protected int printNode(Node node) {
        if (node!=_cluster.getLocalNode())
            node=(Node)_cluster.getNodes().get(node.getDestination());
        if (node==null) {
            if ( _log.isInfoEnabled() ) {

                _log.info(DIndex.getNodeName(node) + " : <unknown>");
            }
            return 0;
        } else {
            PartitionKeys keys=DIndex.getPartitionKeys(node);
            int amount=keys.size();
            if ( _log.isInfoEnabled() ) {

                _log.info(DIndex.getNodeName(node) + " : " + amount + " - " + keys);
            }
            return amount;
        }
    }

    protected Node getNode(Destination destination) {
        Node localNode=_cluster.getLocalNode();
        Destination localDestination=localNode.getDestination();
        if (destination.equals(localDestination))
            return localNode;
        else
            return (Node)_cluster.getNodes().get(destination);
    }

}
