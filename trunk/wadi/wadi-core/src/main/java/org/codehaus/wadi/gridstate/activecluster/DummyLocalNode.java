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
package org.codehaus.wadi.gridstate.activecluster;

import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;

import org.apache.activecluster.LocalNode;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1563 $
 */
public class DummyLocalNode implements LocalNode {

    public DummyLocalNode() {
        super();
        // TODO Auto-generated constructor stub
    }

    public void setState(Map state) throws JMSException {
        // TODO Auto-generated method stub

    }

    public Destination getDestination() {
        // TODO Auto-generated method stub
        return null;
    }

    public Map getState() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isCoordinator() {
        // TODO Auto-generated method stub
        return false;
    }

    public Object getZone() {
        // TODO Auto-generated method stub
        return null;
    }

}
