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

import javax.jms.Destination;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class MigrationRequest
  implements Executable
{
  protected static final Log  _log = LogFactory.getLog(MigrationRequest.class);

  protected final String _id;
  public String getId(){return _id;}

  protected final long _timeout;
  public long getTimeout(){return _timeout;}

  public
    MigrationRequest(String id, long timeout)
  {
    _id          =id;
    _timeout     =timeout;
  }

  public void
    invoke(MigrationService service, Destination source, String correlationID)
  {
    if (getReplyTo(source).equals(service.getManager().getCluster().getLocalNode().getDestination()))
    {
      _log.trace(_id+": our own message forwarded back to us - ignoring");
      return;
    }

    HttpSessionImpl impl=null;

    if ((impl=(HttpSessionImpl)service.getHttpSessionImplMap().get(_id))==null)
    {
      if (_log.isTraceEnabled()) _log.info("session not present: "+_id);
    }
    else
    {
      boolean acquired=false;
      try
      {
	impl.getRWLock().setPriority(HttpSessionImpl.EMMIGRATION_PRIORITY);
	long start=System.currentTimeMillis();
	if (true==(acquired=impl.getContainerLock().attempt(_timeout)))
	{
	  long elapsed=System.currentTimeMillis()-start;
	  if (impl.getRealId()==null)
	  {
	    _log.info(_id+": session disappeared whilst we were waiting for emmigration lock ("+elapsed+"/"+_timeout+" millis elapsed) - forwarding");

	    // 3 possibilities..

	    // a) it has just been passivated to shared store - in
	    // which case we simply reload it.

	    // b) it has just been emmigrated to another node - still
	    // thinking about this one...

	    // c) it has just been invalidated - we could do nothing,
	    // but better would be to send back a message to cut short
	    // the other nodes waiting...

	    // BUT - how do we know what happened to the session ???

	    // so it looks like we have to send back a message saying
	    // "here it is" or "it went here" or "it's dead"...

//  	    try
//  	    {
//  	      Cluster cluster=service.getManager().getCluster();
//  	      ObjectMessage message = cluster.createObjectMessage();
//  	      message.setJMSReplyTo(source);
//  	      message.setJMSCorrelationID(correlationID);
//  	      message.setObject(this);
//  	      cluster.send(service.getManager().getCluster().getDestination(), message);
//  	    }
//  	    catch (JMSException e)
//  	    {
//  	      _log.warn("problem forwarding migration request", e);
//  	    }
	  }
	  else
	  {
	    if (doit(service, impl, correlationID, source)==Boolean.TRUE)
	    {
	      service.getManager().releaseImpl(impl);
	      _log.trace(_id+": emmigration acknowledged and committed");
	    }
	    else
	    {
	      _log.info(_id+": emmigration failed - rolled back");
	    }
	  }
	}
	else
	  _log.warn(_id+": unable to acquire emmigration lock within "+_timeout+" millis");
      }
      catch (InterruptedException e)
      {
	_log.warn(_id+": interrupted whilst trying to acquire emmigration lock", e);
      }
      finally
      {
	if (acquired)
	  impl.getContainerLock().release();
      }
    }
  }

  public abstract Object doit(MigrationService service, HttpSessionImpl impl, String correlationID, Destination source);

  public String toString() {return "<"+getClass().getName()+":"+_id+">";}

  public abstract Destination getReplyTo(Destination destination);
}
