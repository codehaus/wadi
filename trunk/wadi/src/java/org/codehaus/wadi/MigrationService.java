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

import java.util.Collection;
import java.util.Map;
import javax.jms.Destination;

/**
 * Abstracts out mechanism for i/emmigration of sessions between WADI
 * peers.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public interface
  MigrationService
{
  public Map getHttpSessionImplMap();
  public void setHttpSessionImplMap(Map sessions);

  public StreamingStrategy getStreamingStrategy();
  public void setStreamingStrategy(StreamingStrategy strategy);

  public Manager getManager();
  public void setManager(Manager manager);

  public AsyncToSyncAdaptor getAsyncToSyncAdaptor(); // Hmm...

  public Server getServer();
  public Client getClient();

  public interface
    Client
  {

    /**
     * Migrate a group of sessions from this node to another, The
     * other node is not expecting the transmission.
     *
     * @param candidates a <code>Collection</code> value
     * @param timeout a <code>long</code> value
     * @param dst a <code>Destination</code> value
     * @return a <code>boolean</code> value
     */
    public boolean emmigrate(Collection candidates, long timeout, Destination dst);

    /**
     * Locate a remote session by id and cause it to be migrated from
     * its current location to this node. This node is expecting the
     * transmission.
     *
     * @param realId a <code>String</code> value
     * @param placeholder a <code>HttpSessionImpl</code> value
     * @param timeout a <code>long</code> value
     * @param dst a <code>Destination</code> value
     * @return a <code>boolean</code> value
     */
    public boolean immigrate(String realId, HttpSessionImpl placeholder, long timeout, Destination dst);
  }

  public interface
    Server
  {
    public void start() throws Exception;
    public void stop() throws Exception;

    public Destination getDestination();

    public HttpSessionImplFactory getHttpSessionImplFactory();
    public void setHttpSessionImplFactory(HttpSessionImplFactory factory);

  }
}
