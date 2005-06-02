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
package org.codehaus.wadi.test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;
import EDU.oswego.cs.dl.util.concurrent.WaitableInt;

public class SoakTestClient implements Runnable {

    class Request implements Runnable {
        
        public void run() {
            _log.info("running request... ");
            _completer.increment();
            _log.info("...request finished: "+_completer.get());
        }
        
    }
    
    protected final static Log _log = LogFactory.getLog(SoakTestClient.class);
    
    protected final PooledExecutor _executor;
    protected final int _numConcurrentRequests;
    protected final Request[] _requests;
    protected final SynchronizedInt _completer;
    
    protected int _remaining;
    
    public SoakTestClient(PooledExecutor executor, int numConcurrentRequests, int numIterations, SynchronizedInt completer) {
        _executor=executor;
        _numConcurrentRequests=numConcurrentRequests;
        _requests=new Request[_numConcurrentRequests];
        for (int i=0; i<_numConcurrentRequests; i++)
            _requests[i]=new Request();
        _remaining=numIterations;
        _completer=completer;
    }
    
    public void start() throws InterruptedException {
        _executor.execute(this);
    }
    
    public void run() {
        _log.info("running client");
        try {
            // put our requests on the execution queue...
            for (int i=0; i<_numConcurrentRequests; i++)
                _executor.execute(_requests[i]);
            // put ourself back on the execution queue...
            if (--_remaining>0)
                _executor.execute(this);
            else
                _log.info("...client finished");
        } catch (InterruptedException e) {
            _log.warn("interruption detected - aborting...");
        }
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        
        int numClients=Integer.parseInt(args[0]);
        _log.info("number of clients: "+numClients);
        int requestsPerClient=Integer.parseInt(args[1]);
        _log.info("number of concurrent requests per client: "+requestsPerClient);
        int numThreads=Integer.parseInt(args[2]);
        _log.info("number of concurrent threads: "+numThreads);
        int numIterations=Integer.parseInt(args[3]);
        _log.info("number of iterations to perform: "+numIterations);
        
        PooledExecutor executor=new PooledExecutor(new LinkedQueue(), numThreads);
        WaitableInt completer=new WaitableInt(0);
        SoakTestClient[] clients=new SoakTestClient[numClients];
        try {
            for (int i=0; i<numClients; i++)
                (clients[i]=new SoakTestClient(executor, requestsPerClient, numIterations, completer)).start();
        } catch (InterruptedException e) {
            _log.warn("interrupted - aborting...");
        }
        
        // wait for work to be done....
        int totalNumRequests=numClients*requestsPerClient;
        try {
            completer.whenEqual(totalNumRequests, null);
        } catch (InterruptedException e) {
            _log.warn("interrupted - aborting...");
        }
        executor.shutdownNow();
        
        _log.info("finished ");
    }

}
