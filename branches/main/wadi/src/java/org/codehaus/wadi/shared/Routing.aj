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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


// now done by HttpSession facade... - which knows about bucket name

//----------------------------------------

// every session id may have routing info appended to it - à la
// modjk...

// this aspect ensures that external (application, container) requests
// for the session's id include this suffix, whereas internal requests
// (wadi) do not.

// this is not an ideal way to do this, as the application will see
// the routing info - which I don't think should happen - however this
// is what currently happens in Jetty/TC anyway... The only way around
// this is to have a well defined SPI between container and session
// manager which defines how the session cookie/param is read from the
// request and written to the response. If this were available routing
// info could be stripped and added at this point. This is how I think
// it should be done. This is however, unfortunately beyond the
// current scope of wadi since it requires changes to container
// code...
//----------------------------------------

// TODO - we should cache resulting id and wrap setId to rebuild the cache...

// TODO - a mess :-(

privileged public aspect
  Routing
{
//   private static final Log _log=LogFactory.getLog(Routing.class);

//   protected String _suffix=".foo";
//   protected String _id;

//   pointcut getId(HttpSessionImpl hsi) :
//     execution(String HttpSessionImpl.getId()) && target(hsi) && withincode(String HttpSession.getId());	// TODO - how do we say HttpSession and subclasses ?

//   Object
//     around(HttpSessionImpl hsi)
//     : getId(hsi)
//     {
//       _log.trace(_id+": someone called getId()");

//       if (_id==null)
// 	_id=hsi._id+_suffix;

//       return _id;
//     }

//   pointcut setId(HttpSessionImpl hsi, String id) :
//     execution(void HttpSessionImpl.setId(String)) && args(id) && target(hsi);

//   void
//     around(HttpSessionImpl hsi, String id)
//     : setId(hsi, id)
//     {
//       _log.trace(_id+": someone called setId("+id+")");
//       proceed(hsi, id);
//       _id=id+_suffix;
//     }
}
