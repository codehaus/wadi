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
import org.codehaus.wadi.jetty.HttpSessionImpl;
import org.codehaus.wadi.shared.MigrationService;

public class
  TestMigrationService
  extends TestCase
{
  protected final Log _log=LogFactory.getLog(getClass());

  public
    TestMigrationService(String name)
  {
    super(name);
  }

  //----------------------------------------

  public void
    testMigration()
    throws Exception
  {

    Map serverSessions=new HashMap();
    Map serverLocks=new HashMap();

    HttpSessionImpl session=new HttpSessionImpl();
    session.setId(""+System.currentTimeMillis());
    String id=session.getId();
    _log.info("session: "+id);
    serverSessions.put(id, session);

    MigrationService.Server server=new MigrationService.Server(serverSessions, serverLocks);
    server.start();

    int serverPort=server.getPort();
    InetAddress serverAddress=server.getAddress();
    _log.info("server: "+serverAddress+":"+serverPort);

    Map clientSessions=new HashMap();
    Map clientLocks=new HashMap();

    MigrationService.Client client=new MigrationService.Client();
    assertTrue(serverSessions.size()==1);
    assertTrue(serverSessions.containsKey(id));
    assertTrue(serverLocks.size()==0);
    assertTrue(clientSessions.size()==0);
    assertTrue(!clientSessions.containsKey(id));
    assertTrue(clientLocks.size()==0);
    _log.info("serverSessions: "+serverSessions+", clientSessions:"+clientSessions);
    client.run(clientSessions, clientLocks, id, serverAddress, serverPort);
    server.stop();
    assertTrue(serverSessions.size()==0);
    assertTrue(!serverSessions.containsKey(id));
    assertTrue(serverLocks.size()==0);
    assertTrue(clientSessions.size()==1);
    assertTrue(clientSessions.containsKey(id));
    assertTrue(clientLocks.size()==0);
    _log.info("serverSessions: "+serverSessions+", clientSessions:"+clientSessions);
  }
}
