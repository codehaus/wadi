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
import org.codehaus.wadi.MigrationService;
import org.codehaus.wadi.StreamingStrategy;
import org.codehaus.wadi.impl.MessagedMigrationService;
import org.codehaus.wadi.impl.SimpleStreamingStrategy;
import org.codehaus.wadi.impl.StreamedMigrationService;
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

  protected MigrationService        _sms0=new StreamedMigrationService();
  protected MigrationService        _sms1=new StreamedMigrationService();
  protected MigrationService        _mms0=new MessagedMigrationService();
  protected MigrationService        _mms1=new MessagedMigrationService();

  protected StreamingStrategy _streamingStrategy=new SimpleStreamingStrategy();
  protected HttpSessionImplFactory _factory=new HttpSessionImplFactory();

  public
    TestMigrationService(String name)
    {
      super(name);
    }

//    protected void
//      setUp()
//      throws Exception
//      {
//        setUp(_sms0);
//        setUp(_sms1);
//        setUp(_mms0);
//        setUp(_mms1);
//        _log.info("starting test");
//      }

//    protected void
//      setUp(MigrationService ms)
//      {
//        InetAddress address=InetAddress.getByName("228.5.6.7");
//        int port=6789;
//        int timeout=5000;		// 5 secs
//        ms.setHttpSessionImplMap(new Mao());
//        ms.setHttpSessionImplFactory(_factory);
//        ms.start();
//      }

//    protected void
//      tearDown()
//      throws Exception
//      {
//        tearDown(_sms0);
//        tearDown(_sms1);
//        tearDown(_mms0);
//        tearDown(_mms1);
//        _log.info("ending test");
//      }

//    protected void
//      tearDown(MigrationService ms)
//      {
//        ms.stop();
//      }

//    //----------------------------------------

//    public void
//      testMigration()
//      throws Exception
//      {

//        org.codehaus.wadi.HttpSessionImpl session=new org.codehaus.wadi.tomcat.HttpSessionImpl();
//        session.setId(""+System.currentTimeMillis());
//        String id=session.getRealId();
//        if (_log.isInfoEnabled()) _log.info("session: "+id);
//        _clientSessions.put(id, session);

//        if (_log.isInfoEnabled()) _log.info("server: "+_server.getDestination());

//        assertTrue(_serverSessions.size()==0);
//        assertTrue(_clientSessions.size()==1);
//        assertTrue(_clientSessions.containsKey(id));
//        if (_log.isInfoEnabled())
//        {
//  	_log.info("_clientSessions: "+_clientSessions);
//  	_log.info("_serverSessions: "+_serverSessions);
//        }
//        _client.emmigrateMultipleSessions(_clientSessions.values(), 5000L, _server.getDestination());
//        assertTrue(_clientSessions.size()==0);
//        assertTrue(_serverSessions.size()==1);
//        assertTrue(_serverSessions.containsKey(id));
//        if (_log.isInfoEnabled())
//        {
//  	_log.info("_clientSessions: "+_clientSessions);
//  	_log.info("_serverSessions: "+_serverSessions);
//        }
//      }
}
