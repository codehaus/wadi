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

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import javax.jms.ObjectMessage;

public class
    EmmigrationResponse
    implements Command
{
  protected SerializableLog _log=new SerializableLog(getClass());
  protected String          _id;
  protected HttpSessionImpl _impl;
  protected long            _timeout;

  public
    EmmigrationResponse(String id, long timeout, HttpSessionImpl impl)
  {
    _id      =id;
    _timeout =timeout;
    _impl    =impl;
  }

  public void
    run(ObjectMessage message, Manager manager)
  {
    _log.info("here is the session: "+"xxx");
  }
}

