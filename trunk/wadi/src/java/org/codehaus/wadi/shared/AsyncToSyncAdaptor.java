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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import EDU.oswego.cs.dl.util.concurrent.BrokenBarrierException;
import EDU.oswego.cs.dl.util.concurrent.Rendezvous;
import EDU.oswego.cs.dl.util.concurrent.TimeoutException;

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

  public interface Sender {public void send(Object command) throws Exception;}

  public Object
    send(Object command, String id, long timeout, Sender sender)
    {
      int participants=2;
      Rendezvous rv=new Rendezvous(participants);
      _log.trace("preparing rendez-vous: "+id);
      _entries.put(id, rv);

      Object result=null;

      try
      {
	sender.send(command);
	result=rv.attemptRendezvous(null, timeout);
      }
      catch (TimeoutException e)
      {
	_log.warn("timed out at rendezvous", e);
      }
      catch (InterruptedException e)
      {
	_log.warn("unexpectedly interrupted whilst waiting on rendezvous", e);
      }
      catch (BrokenBarrierException e)
      {
	_log.warn("Broken barrier - should never happen - we own this rendezvous", e);
      }
      catch (Exception e)
      {
	_log.warn("unexpected problem sending message", e);
      }
      finally
      {
	_entries.remove(id);
      }

      return result;
    }

  public Object
    receive(Object command, String id, long timeout)
    {
      _log.trace("attending rendez-vous: "+id);
      Rendezvous rv=(Rendezvous)_entries.get(id);
      Object result=null;

      if (rv==null)
	_log.warn("missed rendezvous - invoker thread must have timed out");
      else
	try
	{
	  result=rv.attemptRendezvous(command, timeout);
	  _log.trace("rendez-vous successful: "+id);
	}
	catch (TimeoutException e)
	{
	  _log.warn("rendezvous timed out", e);
	}
	catch (InterruptedException e)
	{
	  _log.warn("unexpectedly interrupted whilst waiting on rendezvous", e);
	}
	catch (BrokenBarrierException e)
	{
	  _log.warn("Broken barrier - we must have arrived too late for rendezvous", e);
	}

      return result;
    }
}
