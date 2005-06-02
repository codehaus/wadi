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

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import EDU.oswego.cs.dl.util.concurrent.Channel;
import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;
import EDU.oswego.cs.dl.util.concurrent.WaitableInt;

public class SoakTestClient implements Runnable {

    class Request implements Runnable {

        protected final GetMethod _request=new GetMethod();
        protected final boolean _cleanUp;

        public Request(String path) {
            _request.setPath(path);
            _cleanUp=false;
        }

        public void run() {
            HttpClient httpClient=null;
            try {
                httpClient=(HttpClient)_httpClients.take();
                httpClient.executeMethod(_hostConfiguration, _request, _state);
                _request.releaseConnection();
                _httpClients.put(httpClient); // don't put it back if anything goes wrong...
            } catch (Exception e) {
                _log.error("problem executing http request", e);
                _errors.increment();
            } finally {
                int c=_completer.increment();
                _log.info(""+c+" = "+_state.getCookies()[0].getValue()+" : "+_request.getPath());
                if (_cleanUp) {
                }
            }
        }

    }

    protected final static Log _log = LogFactory.getLog(SoakTestClient.class);
    protected final static String _host="smilodon";
    protected final static int _port=80;
    protected final static HostConfiguration _hostConfiguration=new HostConfiguration();

    protected final PooledExecutor _executor;
    protected final int _numConcurrentRequests;
    protected final Request _createRequest;
    protected final Request _destroyRequest;
    protected final Request[] _renderRequests;
    protected final SynchronizedInt _completer;
    protected final HttpState _state=new HttpState();
    protected final SynchronizedInt _errors;
    protected final Channel _httpClients;

    protected int _remaining;

    public SoakTestClient(PooledExecutor executor, int numConcurrentRequests, int numIterations, SynchronizedInt completer, SynchronizedInt errors, Channel httpClients) {
        _executor=executor;
        _numConcurrentRequests=numConcurrentRequests;
        _createRequest=new Request("/wadi/jsp/create.jsp");
        _destroyRequest=new Request("/wadi/jsp/destroy.jsp");
        _renderRequests=new Request[_numConcurrentRequests];
        for (int i=0; i<_numConcurrentRequests; i++)
            _renderRequests[i]=new Request("/wadi/jsp/render.jsp");
        _remaining=numIterations;
        _completer=completer;
        _errors=errors;
        _httpClients=httpClients;
    }

    public void start() throws InterruptedException {
        _executor.execute(this);
    }

    public void run() {
        _createRequest.run();

        try {
            // put our requests on the execution queue...
            for (int i=0; i<_numConcurrentRequests; i++)
                _executor.execute(_renderRequests[i]);
            // put ourself back on the execution queue...
            if (--_remaining>0)
                _executor.execute(this);
            else {
                _executor.execute(_destroyRequest);
            }
        } catch (InterruptedException e) {
            _log.warn("interruption detected - aborting...");
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        _hostConfiguration.setHost(_host, _port);

        try {

            int numClients=Integer.parseInt(args[0]);
            _log.info("number of clients: "+numClients);
            int requestsPerClient=Integer.parseInt(args[1]);
            _log.info("number of concurrent requests per client: "+requestsPerClient);
            int numThreads=Integer.parseInt(args[2]);
            _log.info("number of concurrent threads: "+numThreads);
            int numIterations=Integer.parseInt(args[3]);
            _log.info("number of iterations to perform: "+numIterations);

            Channel httpClients=new LinkedQueue(); // a Pool of HttpClients - otherwise we run out of fds...
            for (int i=0; i<numThreads; i++)
                httpClients.put(new HttpClient());

            PooledExecutor executor=new PooledExecutor(new LinkedQueue(), numThreads);
            WaitableInt completer=new WaitableInt(0);
            SynchronizedInt errors=new SynchronizedInt(0);
            SoakTestClient[] clients=new SoakTestClient[numClients];

            for (int i=0; i<numClients; i++)
                (clients[i]=new SoakTestClient(executor, requestsPerClient, numIterations, completer, errors, httpClients)).start();
            // wait for work to be done....
            int totalNumRequests=numClients*(requestsPerClient+2); // create, render*n, destroy
            completer.whenEqual(totalNumRequests, null);
            executor.shutdownNow();

            int e=errors.get();
            if (e>0)
                _log.error("finished: ERRORS DETECTED: "+e);
            else
                _log.info("finished: no errors");

        } catch (InterruptedException e) {
            _log.warn("interrupted - aborting...");
        }

    }

}
