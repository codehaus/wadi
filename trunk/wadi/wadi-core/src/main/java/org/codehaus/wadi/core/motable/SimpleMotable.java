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
package org.codehaus.wadi.core.motable;

import java.io.Serializable;


/**
 * A very Simple implementation of Motable, with the Bytes field represented as a byte[]
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class SimpleMotable extends AbstractMotable implements Serializable {

	protected byte[] _bytes;

    public synchronized byte[] getBodyAsByteArray() {
        return _bytes;
    }

    public synchronized void setBodyAsByteArray(byte[] bytes) {
        _bytes = bytes;
    }
    
    public boolean equals(Object object) {
        if (this==object)
            return true;
        if (!(object instanceof Motable))
            return false;
        try {
            Motable motable = (Motable) object;
            byte[] bytes=motable.getBodyAsByteArray();
            int l=_bytes.length;
            if (l!=bytes.length)
                return false;
            else {
                for (int i=0; i<l; i++)
                    if (_bytes[i]!=bytes[i])
                        return false;
                return true;
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
    }
}


