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

/**
 * Abstract the encoding and decoding of routing info within the
 * session id, so that different load balancer integrations may be
 * pluggable.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public interface
  RoutingStrategy
{
  /**
   * Strip any routing info from this session id.
   *
   * @param session a <code>String</code> value
   * @return a <code>String</code> value
   */
  String strip(String session);

  /**
   * Strip this routing info from this session id.
   *
   * @param bucket a <code>String</code> value
   * @param session a <code>String</code> value
   * @return a <code>String</code> value
   */
  String strip(String bucket, String session);

  /**
   * Add this routing info to this session id.
   *
   * @param bucket a <code>String</code> value
   * @param session a <code>String</code> value
   * @return a <code>String</code> value
   */
  String augment(String bucket, String session);

  /**
   * Return the corresponding routing info or bucket name for the
   * given session id.
   *
   * @param id a <code>String</code> value
   * @return a <code>String</code> value
   */
  String get(String id);

//   /**
//    * return the given session id without routing info.
//    *
//    * @param id a <code>String</code> value
//    * @return a <code>String</code> value
//    */
//   String getShortName(String id);

//   /**
//    * return the given session id with routing info.
//    *
//    * @param id a <code>String</code> value
//    * @return a <code>String</code> value
//    */
//   String getLongName(String id);
}
