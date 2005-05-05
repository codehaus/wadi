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

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;

import org.activecluster.Cluster;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.old.Executable;
import org.codehaus.wadi.old.HttpSessionImpl;
import org.codehaus.wadi.old.MigrationService;
import org.codehaus.wadi.old.ObjectInputStream;

public class
  MessagedMigrationResponse
  implements Executable
{
  protected static final Log _log=LogFactory.getLog(MessagedMigrationResponse.class);
  protected String     _id;
  protected byte[]     _impl;
  protected long       _timeout;

  public
    MessagedMigrationResponse(String id, long timeout, byte[] impl)
  {
    _id      =id;
    _timeout =timeout;
    _impl    =impl;
  }

  public void
    invoke(MigrationService service, Destination source, String correlationID)
  {
    boolean ok=false;

    try
    {
      ByteArrayInputStream bais=new ByteArrayInputStream(_impl);
      ObjectInputStream    ois =new ObjectInputStream(bais);
      HttpSessionImpl      impl=(HttpSessionImpl)service.getHttpSessionImplMap().get(_id);
      impl.readContent(ois);
      ois.close();
      service.getAsyncToSyncAdaptor().receive(impl, correlationID, _timeout);
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

    Destination src=null;
    Destination dst=null;
    try
    {
      Cluster cluster=service.getManager().getCluster();
      ObjectMessage out=cluster.createObjectMessage();
      src=cluster.getLocalNode().getDestination();
      dst=source;
      out.setJMSReplyTo(src);
      out.setJMSCorrelationID(correlationID);
      out.setObject(new MessagedMigrationAcknowledgement(_id, ok, _timeout));
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

  // this is how I want to manage serialisation for this message - but
  // I don't have the HttpSessionImpl factory to hand at the point
  // that I am demarshalled, so do not know which type of session to
  // read the incoming session content into :-(

//   private void readObject(java.io.ObjectInputStream is)
//     throws java.io.IOException, ClassNotFoundException
//   {
//     _log.info("demarshalling");
//     _id=(String)is.readObject();
//     _timeout=is.readLong();
//     _impl.readContent(is);
//   }

//   private void writeObject(java.io.ObjectOutputStream os)
//     throws java.io.IOException
//   {
//     _log.info("marshalling");
//     os.writeObject(_id);
//     os.writeLong(_timeout);
//     _impl.writeContent(os);
//   }
}

