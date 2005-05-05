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

package org.codehaus.wadi.impl;

import java.util.Map;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.activecluster.Cluster;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.AsyncToSyncAdaptor;
import org.codehaus.wadi.Executable;
import org.codehaus.wadi.HttpSessionImplFactory;
import org.codehaus.wadi.Manager;
import org.codehaus.wadi.MigrationService;
import org.codehaus.wadi.sandbox.Streamer;
import org.codehaus.wadi.sandbox.impl.SimpleStreamer;

/**
 * Describe AbstractMigrationService here...
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public abstract class
  AbstractMigrationService
  implements MigrationService
{
  protected final Log _log = LogFactory.getLog(getClass());

  protected AsyncToSyncAdaptor _adaptor = new AsyncToSyncAdaptor();
  public AsyncToSyncAdaptor getAsyncToSyncAdaptor(){return _adaptor;}

  protected Streamer _streamingStrategy=new SimpleStreamer();
  public Streamer getStreamingStrategy(){return _streamingStrategy;}
  public void setStreamingStrategy(Streamer strategy){_streamingStrategy=strategy;}

  protected Map _sessions;
  public Map getHttpSessionImplMap(){return _sessions;}
  public void setHttpSessionImplMap(Map sessions){_sessions=sessions;}

  protected Manager _manager;
  public Manager getManager(){return _manager;}
  public void setManager(Manager manager){_manager=manager;}

  abstract class
    Client
    implements MigrationService.Client
  {
    protected final Log _log = LogFactory.getLog(getClass());
  }

  abstract class
    Server
    implements MigrationService.Server
  {
    protected final Log _log = LogFactory.getLog(getClass());

    protected HttpSessionImplFactory _factory;
    public HttpSessionImplFactory getHttpSessionImplFactory(){return _factory;}
    public void setHttpSessionImplFactory(HttpSessionImplFactory factory){_factory=factory;}

    protected MessageConsumer _clusterConsumer;
    protected MessageConsumer _nodeConsumer;
    protected InvokableListener _listener;

    public void
      start()
      throws JMSException
    {
      _listener=new InvokableListener(_manager);
      Cluster cluster=_manager.getCluster();
      (_clusterConsumer=cluster.createConsumer(cluster.getDestination(), null, true)).setMessageListener(_listener);
      (_nodeConsumer   =cluster.createConsumer(cluster.getLocalNode().getDestination())).setMessageListener(_listener);
    }

    public void
      stop()
      throws JMSException
    {
      _clusterConsumer.close();
      _clusterConsumer=null;
      _nodeConsumer.close();
      _nodeConsumer=null;
      _listener=null;
    }

    class InvokableListener
      implements MessageListener
    {
      protected Manager _manager;

      public
	InvokableListener(Manager manager)
      {
	_manager=manager;
      }

      public void
	onMessage(Message message)
      {
	new Thread(new Runner(message), "MessageListener").start();
      }

      class Runner
	implements Runnable
      {
	protected Message _message;

	Runner(Message message)
	{
	  _message=message;
	}

	public void
	  run()
	{
	  Message message=_message;
	  try
	  {
	    ObjectMessage om=null;
	    Object tmp=null;
	    Executable invocable=null;
	    if (message instanceof ObjectMessage &&
		(om=(ObjectMessage)message)!=null &&
		(tmp=om.getObject())!=null &&
		tmp instanceof Executable &&
		(invocable=(Executable)tmp)!=null)
	    {
	      try
	      {
		invocable.invoke(AbstractMigrationService.this, om.getJMSReplyTo(), om.getJMSCorrelationID());
	      }
	      catch (Throwable t)
	      {
		_log.warn("unexpected problem responding to message:"+invocable, t);
	      }
	    }
	    else
	    {
	      _log.warn("null message or unrecognised message type:"+message);
	    }
	  }
	  catch (JMSException e)
	  {
	    _log.warn("unexpected problem unpacking message:"+message);
	  }
	}
      }
    }
  }
}
