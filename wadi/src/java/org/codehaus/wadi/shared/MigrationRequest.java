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

package org.codehaus.wadi.shared;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import org.codehaus.activecluster.Cluster;

public class
    MigrationRequest
    implements Invocable
{
  protected SerializableLog _log=new SerializableLog(getClass());
  protected String          _id;
  protected InetAddress     _address;
  protected int             _port;
  protected long            _timeout;

  protected Destination     _replyTo;

  public
    MigrationRequest(String id, InetAddress address, int port, long timeout)
  {
    _id      =id;
    _address =address;
    _port    =port;
    _timeout =timeout;
  }

  public void
    invoke(Manager manager, ObjectMessage in)
  {
    HttpSessionImpl impl=null;

    if ((impl=(HttpSessionImpl)manager._local.get(_id))!=null)
    {
      MigrationService.Client client=new MigrationService.Client();
      Collection list=new ArrayList(1);
      list.add(impl);		// must be mutable
      client.emmigrate(manager._local, list, _timeout, _address, _port, manager.getStreamingStrategy(), true);

      Cluster cluster=manager.getCluster();

      Destination dest=null;
      try
      {
	dest=in.getJMSReplyTo();
	_log.info("sending response to: "+dest);
	ObjectMessage out = cluster.createObjectMessage();
	out.setJMSReplyTo(cluster.getLocalNode().getDestination());
	out.setObject(new MigrationResponse(_id, _timeout, null));
	cluster.send(dest, out);
	_log.info("sent response to: "+dest);
      }
      catch (JMSException e)
      {
	_log.warn("could not send migration response to: "+dest, e);
      }
    }
    else
    {
      if (_log.isTraceEnabled()) _log.info("session not present: "+_id);
    }
  }
}

