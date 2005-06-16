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

import org.activecluster.Cluster;
import org.activemq.ActiveMQConnectionFactory;

public class DIndexNode {

    //protected final String _clusterUri="peer://org.codehaus.wadi";
    //protected final String _clusterUri="tcp://localhost:61616";
    protected final String _clusterUri="tcp://smilodon:61616";
    protected final ActiveMQConnectionFactory _connectionFactory=new ActiveMQConnectionFactory(_clusterUri);

    protected final DIndex _dindex;
    
    public DIndexNode(String nodeName, int numBuckets) {
        _dindex=new DIndex(nodeName, numBuckets, _connectionFactory);
    }

    public void start() throws Exception {
        _connectionFactory.start();
        _dindex.start();
    }
    
    public void stop() throws Exception {
        _dindex.stop();
        _connectionFactory.stop();
    }
    
    public Cluster getCluster() {
        return _dindex.getCluster();
    }
    
    //-----------------------------------------------------------
    
    protected static Object _exitSync = new Object();

    public static void main(String[] args) throws Exception {
        String nodeName=args[0];
        int numIndexPartitions=Integer.parseInt(args[1]);
        
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                System.err.println("SHUTDOWN");
                synchronized (_exitSync) {_exitSync.notifyAll();}
                try {
                    synchronized (_exitSync) {_exitSync.wait();}
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        });
        
        DIndexNode node=new DIndexNode(nodeName, numIndexPartitions);
        node.start();
        
        synchronized (_exitSync) {_exitSync.wait();}
        
        node.stop();
        
        synchronized (_exitSync) {_exitSync.notifyAll();}
        
    }
}
