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

  protected final String      _id;
  protected final long        _timeout;

  public
    MigrationRequest(String id, long timeout)
  {
    _id          =id;
    _timeout     =timeout;
  }

  public abstract void invoke(MigrationService service, Destination source, String correlationID);
 

  public String toString() {return "<MigrationRequest:"+_id+">";}
}
