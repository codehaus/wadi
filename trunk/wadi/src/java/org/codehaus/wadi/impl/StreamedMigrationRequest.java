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
import org.codehaus.wadi.MigrationService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class
  StreamedMigrationRequest
  extends org.codehaus.wadi.MigrationRequest
{
  protected final static Log _log=LogFactory.getLog(StreamedMigrationRequest.class);
  protected final Destination _destination;

  public
    StreamedMigrationRequest(String id, Destination destination, long timeout)
  {
    super(id, timeout);
    _destination=destination;
    _log.info("ctor");
  }

  public void
    invoke(MigrationService service, Destination source, String correlationID)
  {
    _log.info("invoke - NYI");
  }
}
