/**
 *
 * Copyright 2003-2004 The Apache Software Foundation
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

import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.shared.DiscoveryService;

import java.net.InetAddress;

public class
  TestDiscoveryService
  extends TestCase
{
  protected Log                     _log=LogFactory.getLog(TestHttpSession.class);
  protected DiscoveryService.Server _server;
  protected DiscoveryService.Client _client;
  protected long                    _time=System.currentTimeMillis();
  protected String                  _request="request-"+_time;
  protected String                  _response=_request+"-response-"+_time;

  public TestDiscoveryService(String name) {super(name);}

  protected void
    setUp()
    throws Exception
    {
      _log.info("starting test");
      InetAddress address=InetAddress.getByName("228.5.6.7");
      int port=6789;
      int timeout=5000;		// 5 secs
      _client=new DiscoveryService.Client(address, port, timeout);
      _server=new DiscoveryService.Server(address, port)
	{
	  public String process(String request)
	  {
	    assertTrue(request.equals(_request));
	    return _response;
	  }
	};
      _server.start();
    }

  protected void
    tearDown()
    throws InterruptedException
    {
      _server.stop();
      _server=null;
      _log.info("stopping test");
    }

  public void
    testRoundTrip()
    {
      String response=_client.run(_request);
      assertTrue(response.equals(_response));
    }
}
