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
import org.codehaus.wadi.MigrationService;
import org.codehaus.wadi.StreamingStrategy;
import org.codehaus.wadi.impl.SimpleStreamingStrategy;
import org.codehaus.wadi.jetty.HttpSessionImplFactory;

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

  protected MigrationService        _service;
  protected MigrationService.Server _server;
  protected MigrationService.Client _client;

  protected Map _clientSessions=new HashMap();
  protected Map _serverSessions=new HashMap();

  protected StreamingStrategy _streamingStrategy=new SimpleStreamingStrategy();
  protected HttpSessionImplFactory _factory=new HttpSessionImplFactory();

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
       _service=new org.codehaus.wadi.impl.async.MigrationService();
       _service.setHttpSessionImplMap(_serverSessions);
       _client=_service.getClient();
       _server=_service.getServer();
       _server.setHttpSessionImplFactory(_factory);
       _server.start();
     }

   protected void
     tearDown()
     throws Exception
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

       org.codehaus.wadi.HttpSessionImpl session=new org.codehaus.wadi.tomcat.HttpSessionImpl();
       session.setId(""+System.currentTimeMillis());
       String id=session.getRealId();
       if (_log.isInfoEnabled()) _log.info("session: "+id);
       _clientSessions.put(id, session);

       if (_log.isInfoEnabled()) _log.info("server: "+_server.getDestination());

       assertTrue(_serverSessions.size()==0);
       assertTrue(_clientSessions.size()==1);
       assertTrue(_clientSessions.containsKey(id));
       if (_log.isInfoEnabled())
       {
 	_log.info("_clientSessions: "+_clientSessions);
 	_log.info("_serverSessions: "+_serverSessions);
       }
       _client.emmigrate(_clientSessions.values(), 5000L, _server.getDestination());
       assertTrue(_clientSessions.size()==0);
       assertTrue(_serverSessions.size()==1);
       assertTrue(_serverSessions.containsKey(id));
       if (_log.isInfoEnabled())
       {
 	_log.info("_clientSessions: "+_clientSessions);
 	_log.info("_serverSessions: "+_serverSessions);
       }
     }
}
