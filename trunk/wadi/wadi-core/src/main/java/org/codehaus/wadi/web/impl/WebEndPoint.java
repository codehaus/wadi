/**
 *
 * Copyright 2003-2006 Core Developers Network Ltd.
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
package org.codehaus.wadi.web.impl;

import java.io.Serializable;
import java.net.InetSocketAddress;
import org.codehaus.wadi.EndPoint;
import org.codehaus.wadi.impl.Utils;


/**
 * WebEndPoint - an Endpoint that encapsulates the IP address and Port of a WebServer service.
 *
 * @author jules
 * @versio $Revision$
 */
public class WebEndPoint implements EndPoint, Comparable, Serializable {
    
    protected final InetSocketAddress _address;
    
    public WebEndPoint(InetSocketAddress address) {
        _address=address;
    }

    // 'java.lang.Object' API
    
    public String toString() {
        return "<"+Utils.basename(getClass())+":"+_address+">";
    }
    
    public boolean equals(Object object) {
        if (!(object instanceof WebEndPoint))
            return false;
        
        WebEndPoint that=(WebEndPoint)object;
        return this._address.equals(that._address);
    }
    
    // 'java.lang.Comparable' API
    
    public int compareTo(Object object) {
        WebEndPoint that=(WebEndPoint)object;
        int answer=compareTo(this._address.getAddress().getAddress(), that._address.getAddress().getAddress());
        return (answer==0)?this._address.getPort()-that._address.getPort():answer;
    }
    
    protected int compareTo(byte[] b1, byte[] b2) {
        int l1=b1.length;
        int l2=b2.length;
        for (int i=0; i<l1 && i<l2; i++) {
            int b=b1[i]-b2[i];
            if (b!=0)
                return b;
        }
        return l1-l2;            
    }
    
    // 'org.codehaus.wadi.web.WebEndPoint' API
    
    public InetSocketAddress getInetSocketAddress() {
        return _address;
    }
    
}
