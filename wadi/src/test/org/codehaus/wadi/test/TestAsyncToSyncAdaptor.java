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

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.jetty.HttpSessionImplFactory;
import org.codehaus.wadi.shared.StreamingStrategy;
import org.codehaus.wadi.plugins.SimpleStreamingStrategy;
import org.codehaus.wadi.shared.AsyncToSyncAdaptor;

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
      _adaptor=new AsyncToSyncAdaptor("test");
    }

  protected void
    tearDown()
    throws InterruptedException
    {
      _adaptor=null;
      _log.info("stopping test");
    }

  //----------------------------------------

//   class OutwardAsyncCommand
//     extends Command, implements Runnable
//   {
//   }

  public void
    testAdaptor()
    throws Exception
    {}
}
