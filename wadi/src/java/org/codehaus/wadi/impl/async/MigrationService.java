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

import java.util.Collection;
import java.util.Map;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.activecluster.Cluster;
import org.codehaus.wadi.AsyncToSyncAdaptor;
import org.codehaus.wadi.HttpSessionImpl;
import org.codehaus.wadi.Executable;
import org.codehaus.wadi.Manager;
import org.codehaus.wadi.StreamingStrategy;

/**
 * A MigrationService which uses activecluster/mq to not only locate
 * the session, but migrate it as well.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class
  MigrationService
  implements org.codehaus.wadi.MigrationService
{
  protected final Log _log=LogFactory.getLog(getClass());

  protected StreamingStrategy _streamingStrategy;
  protected Manager           _manager;
  protected Client            _client=new Client();
  protected Server            _server=new Server();

  public StreamingStrategy getStreamingStrategy(){return _streamingStrategy;}
  public void setStreamingStrategy(StreamingStrategy strategy){_streamingStrategy=strategy;}

  public Manager getManager(){return _manager;}
  public void setManager(Manager manager){_manager=manager;}


  public org.codehaus.wadi.MigrationService.Server getServer(){return _server;}
  public org.codehaus.wadi.MigrationService.Client getClient(){return _client;}

  public class
    Client
    implements org.codehaus.wadi.MigrationService.Client
  {
    public boolean
      emmigrate(Map local, Collection candidates, long timeout, Destination dst)
      {
	return true;
      }

    public boolean
      immigrate(Map local, String realId, HttpSessionImpl placeholder, long timeout, Destination dst)
      {
	return false;
      }
  }

  public class
    Server
    implements org.codehaus.wadi.MigrationService.Server
    {
      protected Destination _destination;

      public Destination getDestination(){return _destination;}

      protected MessageConsumer   _clusterConsumer;
      protected MessageConsumer   _nodeConsumer;
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
	implements MessageListener, Runnable
      {
	protected Manager _manager;

	public
	  InvokableListener(Manager manager)
	{
	  _manager=manager;
	}

	protected AsyncToSyncAdaptor _adaptor=new AsyncToSyncAdaptor(); // TODO

	protected Message _message;

	public void
	  onMessage(Message message)
	{
	  _message=message;
	  new Thread(this, "MessageListener").start();
	}

	public void
	  run()
	{
	  Message message=_message;
	  _log.info("message arriving: "+Thread.currentThread());
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
	      _log.info("message arrived: "+invocable);
	      try
	      {
		invocable.invoke(_manager, om);
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
