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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.activecluster.Cluster;
import org.codehaus.wadi.impl.async.MigrationResponse;

public class
  MigrationRequest
  implements Executable
{
  protected static final Log  _log=LogFactory.getLog(MigrationRequest.class);
  protected final String      _id;
  protected final Destination _destination;
  protected final long        _timeout;

  public
    MigrationRequest(String id, Destination destination, long timeout)
  {
    _id      =id;
    _destination=destination;
    _timeout =timeout;
  }

  public void
    invoke(Manager manager, ObjectMessage in)
  {
    HttpSessionImpl impl=null;

    if ((impl=(HttpSessionImpl)manager._local.get(_id))==null)
    {
      if (_log.isTraceEnabled()) _log.info("session not present: "+_id);
    }
    else
    {
      //       MigrationService.Client client=new MigrationService.Client();
      //       Collection list=new ArrayList(1);
      //       list.add(impl);		// must be mutable
      //       client.emmigrate(manager._local, list, _timeout, _address, _port, manager.getStreamingStrategy(), true);

      boolean acquired=false;
      Object result=null;
      try
      {
	impl.getContainerLock().attempt(_timeout);
	acquired=true;
	ByteArrayOutputStream baos=new ByteArrayOutputStream();
	ObjectOutputStream    oos =new ObjectOutputStream(baos);
	impl.writeContent(oos);
	oos.flush();
	byte[] buffer=baos.toByteArray();
	oos.close();

	Destination dest=null;
	try
	{
	  Cluster cluster=manager.getCluster();
	  result=manager.getAsyncToSyncAdaptor().send(cluster,
				       new MigrationResponse(_id, _timeout, buffer),
				       in.getJMSCorrelationID(),
				       _timeout,
				       cluster.getLocalNode().getDestination(),
				       in.getJMSReplyTo());
	}
	catch (JMSException e)
	{
	  _log.warn("could not send migration response to: "+dest, e);
	}
      }
      catch (InterruptedException e)
      {
	_log.warn("could not get container lock on session for emmigration", e);
      }
      catch (IOException e)
      {
	_log.warn("IO problems marshalling session for emmigration", e);
      }
      catch (ClassNotFoundException e)
      {
	_log.warn("ClassLoading problems marshalling session for emmigration", e);
      }
      finally
      {
	if (acquired)
	  impl.getContainerLock().release();
      }
      if (result==Boolean.TRUE)
      {
	manager.releaseImpl(impl);
	_log.info(_id+": emmigration acknowledged and committed");
      }
      else
      {
	_log.info(_id+": emmigration failed - rolled back - we still own session");
      }
    }
  }

  public String
    toString()
    {
      return "<MigrationRequest:"+_id+">";
    }
}

