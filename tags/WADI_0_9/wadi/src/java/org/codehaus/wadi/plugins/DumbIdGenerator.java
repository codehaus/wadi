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

package org.codehaus.wadi.plugins;

import org.codehaus.wadi.shared.IdGenerator;

/**
 * A really simple session id generator - NOT for production use :-)
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class DumbIdGenerator
    implements IdGenerator
  {
    protected int _tmpHack=0;
    public synchronized Object take(){return System.currentTimeMillis()+"."+_tmpHack++;}
    public synchronized Object poll(long millis){return take();}

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
