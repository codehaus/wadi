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

package org.codehaus.wadi.impl;

import javax.jms.Destination;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Executable;
import org.codehaus.wadi.MigrationService;

public class
    MessagedMigrationAcknowledgement
    implements Executable
{
  protected static final Log _log=LogFactory.getLog(MessagedMigrationAcknowledgement.class);
  protected final String     _id;
  protected final boolean    _ok;
  protected final long       _timeout;

  public
    MessagedMigrationAcknowledgement(String id, boolean ok, long timeout)
  {
    _id=id;
    _ok=ok;
    _timeout=timeout;
  }

  public void
    invoke(MigrationService service, Destination source, String correlationID)
  {
      service.getAsyncToSyncAdaptor().receive(_ok?Boolean.TRUE:Boolean.FALSE, correlationID, _timeout);
  }

  public String
    toString()
    {
      return "<MigrationAcknowledgement:"+_id+">";
    }
}
