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

package org.codehaus.wadi.old.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.SessionIdFactory;

/**
 * A really simple session id generator - NOT for production use :-)
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class DumbIdGenerator
    implements SessionIdFactory
  {
    protected final Log _log = LogFactory.getLog(getClass());
    
    protected int _tmpHack=0;
    public synchronized Object create(){return System.currentTimeMillis()+"."+_tmpHack++;}

    public int getSessionIdLength() {
        return -1; // variable :-)
    }
    
    public void setSessionIdLength(int l) {
        _log.warn("session id length is not a writeable attribute - ignoring new setting: "+l);
    }
    
    // this could be moved into a SeparatingByChar strategy

//     public String
//       getId(String idId, String idRoute)
//     {
//       return idId+"."+idRoute;
//     }

//   public String
//     getIdId(String id)
//     {
//       int index=id.indexOf('.');
//       return index<0?id:id.substring(0,index);
//     }

//   public String
//     getIdRoute(String id)
//     {
//       int index=id.indexOf('.');
//       return index<0?null:id.substring(index+1,id.length());
//     }
  }
