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

package org.codehaus.wadi.test;

import javax.jms.ObjectMessage;

import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.AsyncToSyncAdaptor;
import org.codehaus.wadi.Executable;
import org.codehaus.wadi.Manager;

/**
 * Test the Migration Service
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class
  TestAsyncToSyncAdaptor
  extends TestCase
{
  protected final Log _log=LogFactory.getLog(getClass());

  public
    TestAsyncToSyncAdaptor(String name)
  {
    super(name);
  }

  protected AsyncToSyncAdaptor _adaptor;

  protected void
    setUp()
    throws Exception
  {
    _log.info("starting test");
    _adaptor=new AsyncToSyncAdaptor();
  }

  protected void
    tearDown()
    throws InterruptedException
  {
    _adaptor=null;
    _log.info("stopping test");
  }

  //----------------------------------------

  long _timeout=2000L;
  String _id="rendez-vous id";
  String _data="payload";


  // now that AsyncToSyncAdaptor is explicitly tied to having a
  // Cluster this becomes more difficult... - do we really need it ?

//   public void
//     testAdaptor()
//     throws Exception
//     {
//       _log.info("[0] entering adaptor");
//       Object result=_adaptor.send(
// 				  new Invocable()
// 				  {
// 				    public void
// 				      invoke(Manager manager, ObjectMessage message)
// 				    {
// 				      _log.info("[0] starting Command");
// 				      new Thread()
// 				      {
// 					public void
// 					  run()
// 					{
// 					  _log.info("[1] entering adaptor");
// 					  Object result=_adaptor.receive(_data, _id, _timeout);
// 					  _log.info("[1] leaving adaptor");
// 					}
// 				      }.start();
// 				      _log.info("[0] ending Command");
// 				    }
// 				  },
// 				  _id,
// 				  _timeout,
// 				  new AsyncToSyncAdaptor.Sender()
// 				  {
// 				    public void
// 				      send(Object command)
// 				    {
// 				      _log.info("[0] send Object");
// 				      ((Invocable)command).invoke(null, null);
// 				      _log.info("[0] sent Object");
// 				    }
// 				  }
// 				  );

//       assertTrue(result.equals(_data));
//       _log.info("[0] data was: "+_data);
//       _log.info("[0] leaving adaptor");

//       Thread.sleep(2000L);
//     }
}
