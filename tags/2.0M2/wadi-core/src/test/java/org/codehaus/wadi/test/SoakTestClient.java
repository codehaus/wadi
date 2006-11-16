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

import java.io.InputStream;

import org.apache.commons.httpclient.Cookie;
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

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class SoakTestClient implements Runnable {

	class Request implements Runnable {

		protected final GetMethod _request=new GetMethod();
		protected final String _path;
		protected final boolean _cleanUp;

		public Request(String path) {
			_path=path;
			_cleanUp=false;
		}

		protected final byte[] _buffer=new byte[1024];

		public void run() {
			HttpClient httpClient=null;
			try {
				httpClient=(HttpClient)_httpClients.take();
				String before="";
				Cookie[] cookies=_state.getCookies();
				if (cookies!=null && cookies.length>0)
					before=cookies[0].getValue();
        GetMethod request=new GetMethod();
				request.setPath(_path);
				int status=httpClient.executeMethod(_hostConfiguration, request, _state);
				InputStream is=request.getResponseBodyAsStream();
				while (is.read(_buffer)>0); // read complete body of response and chuck...
				if (status!=200) {
					if (_log.isErrorEnabled()) _log.error("bad status: " + status + " : " + request.getStatusText());
					_errors.increment();
				}
				String after=_state.getCookies()[0].getValue();
				checkSession(before, after);
				request.releaseConnection();
				Thread.interrupted(); // looks like something in commons-httpclient leaves this flag set - the Channel barfs if so...
				_httpClients.put(httpClient); // don't put it back if anything goes wrong...
				request.recycle();
			} catch (Exception e) {
				_log.error("problem executing http request", e);
				_errors.increment();
			} finally {
				int c=_completer.increment();
				if (_log.isInfoEnabled()) _log.info("" + c + " = " + _state.getCookies()[0].getValue() + " : " + _path);
				if (_cleanUp) {
				}
			}
		}
	}

	protected void checkSession(String before, String after) {
		if (before.length()==0) {
			// session created...
			return;
		}

		if (before.equals(after)) {
			// no change
			return;
		}

		int i=before.lastIndexOf(".");
		if (before.regionMatches(0, after, 0, i)) {
			// session relocated
			if (_log.isInfoEnabled()) _log.info("session cookie association: " + before + " --> " + after);
			return;
		}

		if (_log.isErrorEnabled()) _log.error("session changed:  " + before + " --> " + after);
		_errors.increment();
	}

	protected final static Log _log = LogFactory.getLog(SoakTestClient.class);
	protected final static String _host="smilodon";
	protected final static int _port=90;
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
		_createRequest=new Request("/wadi/session.jsp");
		_destroyRequest=new Request("/wadi/destroy.jsp");
		_renderRequests=new Request[_numConcurrentRequests];
		for (int i=0; i<_numConcurrentRequests; i++)
			_renderRequests[i]=new Request("/wadi/session.jsp?limit=25");
		_remaining=numIterations;
		_completer=completer;
		_errors=errors;
		_httpClients=httpClients;
	}

	public void start() throws InterruptedException {
		_createRequest.run();
		_executor.execute(this);
	}

	public void run() {
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
			if (_log.isInfoEnabled()) _log.info("number of clients: " + numClients);
			int numConnection=Integer.parseInt(args[1]);
			if (_log.isInfoEnabled()) _log.info("number of connections per client: " + numConnection);
      int numIterations=Integer.parseInt(args[2]);
      if (_log.isInfoEnabled()) _log.info("number of iterations per connection: " + numIterations);
      int basePort=8080;//Integer.parseInt(args[3]);
      if (_log.isInfoEnabled()) _log.info("basePort: " + basePort);
      int numPorts=8; //Integer.parseInt(args[4]);
      if (_log.isInfoEnabled()) _log.info("number of ports: " + numPorts);

      int[] ports=new int[numPorts];
      for (int i=0; i>numPorts; i++)
        ports[i]=basePort+numPorts;
      
      int totalConnections=numClients*numConnection;

			Channel httpClients=new LinkedQueue(); // a Pool of HttpClients - otherwise we run out of fds...
			for (int i=0; i<totalConnections; i++)
				httpClients.put(new HttpClient(/*new MultiThreadedHttpConnectionManager()*/));

			PooledExecutor executor=new PooledExecutor(totalConnections);
			WaitableInt completer=new WaitableInt(0);
			SynchronizedInt errors=new SynchronizedInt(0);
			SoakTestClient[] clients=new SoakTestClient[numClients];

			for (int i=0; i<numClients; i++)
				(clients[i]=new SoakTestClient(executor, numConnection, numIterations, completer, errors, httpClients)).start();
			// wait for work to be done....
			int totalNumRequests=numClients*(numConnection*numIterations+2); // create, render*n, destroy
			if (_log.isInfoEnabled()) _log.info("waiting for " + totalNumRequests + " requests to be completed...");
			completer.whenEqual(totalNumRequests, null);
			executor.shutdownNow();

			int e=errors.get();
			if (e>0) {
				if (_log.isErrorEnabled()) _log.error("finished: " + totalNumRequests + " requests and " + e + " ERRORS");
			} else {
				if (_log.isInfoEnabled()) _log.info("finished: " + totalNumRequests + " requests and no errors");
			}

		} catch (InterruptedException e) {
			_log.warn("interrupted - aborting...");
		}

	}

}
