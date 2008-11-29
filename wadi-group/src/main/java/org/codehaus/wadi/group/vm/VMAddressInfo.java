/**
 * Copyright 2006 The Apache Software Foundation
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
package org.codehaus.wadi.group.vm;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;

import org.codehaus.wadi.group.impl.AbstractCluster;

/**
 * 
 * @version $Revision: 1603 $
 */
class VMAddressInfo implements Serializable {
    private static final long serialVersionUID = 1898741773344932378L;

    private String nodeName;
    private VMAddress address;
    
    public VMAddressInfo(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getNodeName() {
        return nodeName;
    }
    
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeUTF(nodeName);
    }
    
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        nodeName = in.readUTF();
        VMLocalCluster cluster = (VMLocalCluster) AbstractCluster.clusterThreadLocal.get();
        address = (VMAddress) cluster.getAddress(nodeName);
    }
    
    public Object readResolve() throws ObjectStreamException {
        return address;
    }
}
