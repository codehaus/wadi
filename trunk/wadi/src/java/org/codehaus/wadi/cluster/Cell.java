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

package org.codehaus.wadi.cluster;

import java.util.Collection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class
  Cell
{
  protected Log        _log=LogFactory.getLog(getClass());
  protected String     _id;
  protected Collection _peers;

  public Cell(String id, Collection peers)
  {
    _id=id;
    _peers=peers;
  }

  public void
    start()
  {
    _log.info("starting: "+_id);
  }

  public void
    stop()
  {
    _log.info("stopping: "+_id);
  }
}
