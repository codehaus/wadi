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

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.shared.MigrationService;
import org.codehaus.wadi.plugins.SimpleStreamingStrategy;

/**
 * Test the Migration Service
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class
  TestMigrationService
  extends TestCase
{
  protected final Log _log=LogFactory.getLog(getClass());

  protected MigrationService.Server _server;
  protected MigrationService.Client _client;

  protected Map _serverSessions=new HashMap();
  protected Map _serverLocks=new HashMap();

  public
    TestMigrationService(String name)
    {
      super(name);
    }

  protected void
    setUp()
    throws Exception
    {
      _log.info("starting test");
      InetAddress address=InetAddress.getByName("228.5.6.7");
      int port=6789;
      int timeout=5000;		// 5 secs
      _client=new MigrationService.Client();
      _server=new MigrationService.Server(_serverSessions, _serverLocks, new SimpleStreamingStrategy());
      _server.start();
    }

  protected void
    tearDown()
    throws InterruptedException
    {
      _server.stop();
      _server=null;
      _client=null;
      _log.info("stopping test");
    }

  //----------------------------------------

  public void
    testMigration()
    throws Exception
    {

      org.codehaus.wadi.shared.HttpSessionImpl session=new org.codehaus.wadi.tomcat.HttpSessionImpl();
      session.setId(""+System.currentTimeMillis());
      String id=session.getId();
      if (_log.isInfoEnabled()) _log.info("session: "+id);
      _serverSessions.put(id, session);

      int serverPort=_server.getPort();
      InetAddress serverAddress=_server.getAddress();
      if (_log.isInfoEnabled()) _log.info("server: "+serverAddress+":"+serverPort);

      Map clientSessions=new HashMap();
      Map clientLocks=new HashMap();

      assertTrue(_serverSessions.size()==1);
      assertTrue(_serverSessions.containsKey(id));
      assertTrue(_serverLocks.size()==0);
      assertTrue(clientSessions.size()==0);
      assertTrue(!clientSessions.containsKey(id));
      assertTrue(clientLocks.size()==0);
      if (_log.isInfoEnabled()) _log.info("_serverSessions: "+_serverSessions+", clientSessions:"+clientSessions);
      _client.run(clientSessions, clientLocks, id, new org.codehaus.wadi.jetty.HttpSessionImpl(), serverAddress, serverPort, new SimpleStreamingStrategy());
      assertTrue(_serverSessions.size()==0);
      assertTrue(!_serverSessions.containsKey(id));
      assertTrue(_serverLocks.size()==0);
      assertTrue(clientSessions.size()==1);
      assertTrue(clientSessions.containsKey(id));
      assertTrue(clientLocks.size()==0);
      if (_log.isInfoEnabled()) _log.info("serverSessions: "+_serverSessions+", clientSessions:"+clientSessions);
    }
}
