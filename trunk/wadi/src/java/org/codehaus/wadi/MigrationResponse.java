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

package org.codehaus.wadi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.activecluster.Cluster;

public class
  MigrationResponse
  implements Invocable
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
      manager._adaptor.receive(impl, _id+"-request", _timeout);
      ok=true;
    }
    catch (IOException e)
    {
      _log.warn("IO problems demarshalling session for immigration", e);
    }
    catch (ClassNotFoundException e)
    {
      _log.warn("ClassLoading problems demarshalling session for immigration", e);
    }

    Destination dest=null;
    Object result=null;
    try
    {
      Cluster cluster=manager.getCluster();
      ObjectMessage om=cluster.createObjectMessage();
      Destination src=cluster.getLocalNode().getDestination();
      om.setJMSReplyTo(src);
      om.setObject(new MigrationAcknowledgement(_id, ok, _timeout));
      cluster.send(in.getJMSReplyTo(), om);
    }
    catch (JMSException e)
    {
      _log.warn("could not send migration response to: "+dest, e);
    }

    ok=(result==Boolean.TRUE);

    HttpSessionImpl impl=null;

    impl=manager.getLocalSession(_id);

    if (impl==null)
    {
      _log.warn(_id+": IN REAL TROUBLE"); // FIXME
    }
    else
    {
      try
      {
	impl=manager.getLocalSession(_id);
	if (ok)
	{
	  _log.debug(_id+": committing emmigration");
	  manager.releaseImpl(impl);
	}
	else
	{
	  _log.debug(_id+": rolling back emmigration");
	}
      }
      finally
      {
	impl.getContainerLock().release();
      }
    }
  }

  public String
    toString()
    {
      return "<MigrationResponse:"+_id+">";
    }
}

