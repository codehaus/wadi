/**
 * Copyright 2007 The Apache Software Foundation
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
package org.codehaus.wadi.webapp;

import java.io.Serializable;

import org.codehaus.wadi.aop.annotation.ClusteredState;


/**
 * 
 * @version $Revision: 2340 $
 */
@ClusteredState
public class Node implements Serializable {
    
    public static Node buildNodeWithDepth(int depth) {
        Node topNode = new Node(); 
        Node node = topNode;
        for (int i = 0; i < depth; i++) {
            node.fieldValue = i + "";
            node.child = new Node();
            node = node.child;
        }
        return topNode;
    }
    
    private String fieldValue;
    private Node child;
    
    public String getFieldValue() {
        return fieldValue;
    }
    
    public void setFieldValue(String fieldValue) {
        this.fieldValue = fieldValue;
    }
    
    public Node getChild() {
        return child;
    }
    
    public void setChild(Node child) {
        this.child = child;
    }
    
}
