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

package org.codehaus.wadi.old.impl;

import java.util.Collection;
import java.util.Map;
import javax.jms.Destination;

import org.codehaus.wadi.old.HttpSessionImpl;
import org.codehaus.wadi.old.MigrationService;

/**
 * A MigrationService which uses activecluster/mq to not only locate
 * the session, but migrate it as well.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class
  MessagedMigrationService
  extends AbstractMigrationService
{
  public MigrationService.Server getServer(){return _server;}
  public MigrationService.Client getClient(){return _client;}

  public class
    Client
    extends AbstractMigrationService.Client
    {
      public boolean
	emmigrateMultipleSessions(Map sessions, Collection candidates, long timeout, Destination dst)
	{
	  return true;
	}

      public boolean
	immigrateSingleSession(String realId, HttpSessionImpl placeholder, long timeout, Destination dst)
	{
	  Destination src=_server.getDestination();
	  String correlationId=realId+"-"+src.toString();
	  Object result=_adaptor.send(_manager.getCluster(),
				      new MessagedMigrationRequest(realId, timeout),
				      correlationId,
				      timeout,
				      src,
				      dst,
				      placeholder);

	  return (placeholder==result);
	}
    }

  public class
    Server
    extends AbstractMigrationService.Server
    {
      public Destination getDestination(){return _manager.getCluster().getLocalNode().getDestination();}
    }

  protected Client _client = new Client();
  protected Server _server = new Server();
}
