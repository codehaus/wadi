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

package org.codehaus.wadi.sandbox.cluster;

import java.util.Collection;
import java.util.Iterator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.activecluster.Cluster;
import org.activecluster.ClusterFactory;

public class
  Cell
{
  protected Log            _log=LogFactory.getLog(getClass());
  protected ClusterFactory _factory;
  protected String         _id;
  protected String         _clusterId;
  protected Collection     _peers;
  protected Cluster        _cluster;

  public
    Cell(String id, String clusterId, Collection peers, ClusterFactory factory)
  {
    _id=id;
    _clusterId=clusterId;
    _peers=peers;
    _factory=factory;
    _cluster=null;
  }

  public void
    start()
  {
      if (_log.isInfoEnabled()) _log.info("starting: " + _id);

    try
    {
      _cluster=_factory.createCluster(_clusterId+"."+_id);
    }
    catch (Exception e)
    {
        if (_log.isWarnEnabled()) _log.warn("unexpected problem starting Cell: " + _id, e);
    }
  }

  public void
    stop()
  {
      if (_log.isInfoEnabled()) _log.info("stopping: " + _id);

    try
    {
      _cluster.stop();
      _cluster=null;
    }
    catch (Exception e)
    {
        if (_log.isWarnEnabled()) _log.warn("unexpected problem stopping Cell: " + _id, e);
    }
  }

  public String
    toString()
  {
    return "<Cell:"+_id+">";
  }

  public String getId(){return _id;}

  public static String
    id(Collection peers)
  {
    String id="";
    for (Iterator i=peers.iterator(); i.hasNext(); )
      id+=((id.length()==0)?"":"-")+((Peer)i.next()).getId();
    return id;
  }
}
