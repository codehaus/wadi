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

package org.codehaus.wadi.impl.async;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.activecluster.Cluster;
import org.codehaus.wadi.HttpSessionImpl;
import org.codehaus.wadi.Executable;
import org.codehaus.wadi.Manager;
import org.codehaus.wadi.ObjectInputStream;

public class
  MigrationResponse
  implements Executable
{
  protected static final Log _log=LogFactory.getLog(MigrationResponse.class);
  protected final String     _id;
  protected final byte[]     _impl;
  protected final long       _timeout;

  public
    MigrationResponse(String id, long timeout, byte[] impl)
  {
    _id      =id;
    _timeout =timeout;
    _impl    =impl;
  }

  public void
    invoke(Manager manager, ObjectMessage in)
  {
    boolean ok=false;

    try
    {
      ByteArrayInputStream bais=new ByteArrayInputStream(_impl);
      ObjectInputStream    ois =new ObjectInputStream(bais);
      HttpSessionImpl impl=manager.getLocalSession(_id);
      impl.readContent(ois);
      ois.close();
      manager.getAsyncToSyncAdaptor().receive(impl, in.getJMSCorrelationID(), _timeout);
      ok=true;
    }
    catch (IOException e)
    {
      _log.warn(_id+": IO problems demarshalling session for immigration", e);
    }
    catch (ClassNotFoundException e)
    {
      _log.warn(_id+": ClassLoading problems demarshalling session for immigration", e);
    }
    catch (JMSException e)
    {
      _log.warn(_id+": could not extract correlation/rendez-vous id", e);
    }

    Destination src=null;
    Destination dst=null;
    String correlationID=null;
    try
    {
      Cluster cluster=manager.getCluster();
      ObjectMessage out=cluster.createObjectMessage();
      src=cluster.getLocalNode().getDestination();
      correlationID=in.getJMSCorrelationID();
      dst=in.getJMSReplyTo();
      out.setJMSReplyTo(src);
      out.setJMSCorrelationID(correlationID);
      out.setObject(new MigrationAcknowledgement(_id, ok, _timeout));
      cluster.send(dst, out);
      _log.trace(correlationID+": response sent");
    }
    catch (JMSException e)
    {
      _log.warn(correlationID+": could not send migration acknowledgement to: "+dst, e);
    }
  }

  public String
    toString()
    {
      return "<MigrationResponse:"+_id+">";
    }
}

