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

import EDU.oswego.cs.dl.util.concurrent.BrokenBarrierException;
import EDU.oswego.cs.dl.util.concurrent.Rendezvous;
import EDU.oswego.cs.dl.util.concurrent.TimeoutException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.jms.Destination;
import javax.jms.ObjectMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.activecluster.Cluster;

/**
 * Enable a thread to send a Command over an async medium and wait for
 * a reply or timeout before continuing...
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version 1.0
 */
public class
  AsyncToSyncAdaptor
{
  protected final Log _log     = LogFactory.getLog(getClass());
  protected final Map _entries = Collections.synchronizedMap(new HashMap());

  public Object
    send(Cluster cluster, Serializable action, String correlationId, long timeout, Destination src, Destination dst, Object datum)
    {
      int participants=2;
      Rendezvous rv=new Rendezvous(participants);
      _entries.put(correlationId, rv);
      _log.trace(correlationId+"/"+hashCode()+": "+"preparing rendez-vous - "+_entries);

      Object result=null;

      try
      {
	ObjectMessage message = cluster.createObjectMessage();
	if (src!=null) message.setJMSReplyTo(src);
	message.setJMSCorrelationID(correlationId);
	message.setObject(action);
	cluster.send(dst, message);

	long timeStarted=System.currentTimeMillis();
	result=rv.attemptRendezvous(datum, timeout);
	long timeTaken=System.currentTimeMillis()-timeStarted;
	_log.trace(correlationId+"/"+hashCode()+": starter completed rendez-vous successfully in "+timeTaken+" millis");
      }
      catch (TimeoutException e)
      {
	_log.debug(correlationId+"/"+hashCode()+": "+"timed out at rendez-vous - no answer within "+timeout+" millis: "+Thread.currentThread());
      }
      catch (InterruptedException e)
      {
	_log.warn("unexpectedly interrupted whilst waiting on rendez-vous", e);
      }
      catch (BrokenBarrierException e)
      {
	_log.warn("Broken barrier - should never happen - we own this rendez-vous", e);
      }
      catch (Exception e)
      {
	_log.warn("unexpected problem sending message", e);
      }
      finally
      {
	_entries.remove(correlationId);
      }

      return result;
    }

  public Object
    receive(Object datum, String correlationId, long timeout)
    {
      _log.trace(correlationId+"/"+hashCode()+": "+"attending rendez-vous - "+_entries);
      Rendezvous rv=(Rendezvous)_entries.get(correlationId);
      Object result=null;

      if (rv==null)
      {
	if (_log.isInfoEnabled())
	  _log.info(correlationId+"/"+hashCode()+" missed rendez-vous - waiting thread must have timed out ("+timeout+" millis)");
      }
      else
      {
	try
	{
	  long timeStarted=System.currentTimeMillis();
	  result=rv.attemptRendezvous(datum, timeout);
	  long timeTaken=System.currentTimeMillis()-timeStarted;
	  if (_log.isTraceEnabled())
	    _log.trace(correlationId+"/"+hashCode()+": finisher completed rendez-vous successfully in "+timeTaken+" millis - "+datum.hashCode());
	}
	catch (TimeoutException e)
	{
	  _log.warn("rendez-vous timed out ("+timeout+" millis)", e);
	}
	catch (InterruptedException e)
	{
	  _log.warn("unexpectedly interrupted whilst waiting on rendez-vous", e);
	}
	catch (BrokenBarrierException e)
	{
	  _log.warn("Broken barrier - we must have arrived too late for rendez-vous ("+timeout+" millis)", e);
	}
      }

      return result;
    }
}
