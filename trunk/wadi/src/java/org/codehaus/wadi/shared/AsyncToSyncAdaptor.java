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
  protected final Map _entries =Collections.synchronizedMap(new HashMap());
  protected final String _name;

  public AsyncToSyncAdaptor(String name){_name=name;}

  protected int _nextId  =0;

  public static abstract class
    SyncCommand
    implements Command
    {
      protected long _rvTimeout=2000L;
      protected String _rvId=null;

      public String getRvId() {
	return _rvId;
      }
      public void setRvId(String id) {
	_rvId = id;
      }
      public long getRvTimeout() {
	return _rvTimeout;
      }
      public void setRvTimeout(long _timeout) {
	this._rvTimeout = _timeout;
      }
    }

  interface Sender {public void send(SyncCommand cc);}

  public Object
    send(SyncCommand command, Sender sender, long timeout)
    {
      int participants=2;
      Rendezvous rv=new Rendezvous(participants);
      String id=_name+"-"+_nextId++;
      _entries.put(id, rv);
      command.setRvId(id);
      command.setRvTimeout(timeout);

      sender.send(command);

      Object result=null;
      try
      {
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
      finally
      {
	_entries.remove(id);
      }

      return result;
    }

  public Object
    receive(SyncCommand command)
    {
      long timeout=command.getRvTimeout();
      String id=command.getRvId();
      Rendezvous rv=(Rendezvous)_entries.get(id);
      Object result=null;

      if (rv==null)
	_log.warn("missed rendezvous - invoker thread must have timed out");
      else
	try
	{
	  result=rv.attemptRendezvous(command, timeout);
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
