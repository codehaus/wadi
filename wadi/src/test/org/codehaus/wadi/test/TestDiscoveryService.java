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
	protected Log _log=LogFactory.getLog(TestHttpSession.class);
	protected DiscoveryService.Server _server;
	protected DiscoveryService.Client _client;

  public TestDiscoveryService(String name) {super(name);}

    protected void
    setUp()
    throws Exception
  {
    _log.info("starting test");
    _server=new DiscoveryService.Server(InetAddress.getByName("228.5.6.7"), 6789)
	{
    	public String process(String request)
    	{
    		_log.info("received request: "+request);
    		return request;
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
    testCreateHttpSession()
  {
    assertTrue(true);
  }
}
