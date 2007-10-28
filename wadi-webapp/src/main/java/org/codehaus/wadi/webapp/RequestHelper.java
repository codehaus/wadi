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

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;


/**
 * 
 * @version $Revision: 2340 $
 */
public class RequestHelper {
    
    public static Node handle(HttpServletRequest request) {
        HttpSession session = request.getSession();

        Map<String, String> parameters = request.getParameterMap();
        parameters = new HashMap<String, String>(parameters);
        
        if (null == session.getAttribute("node")) {
            String depthAsString = parameters.get("depth");
            if (null == depthAsString) {
                depthAsString = "10";
            }
            int depth = Integer.parseInt(depthAsString);
            Node node = Node.buildNodeWithDepth(depth);
            session.setAttribute("node", node);
        }

        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("node.")) {
                String nodeDepthAsString = key.substring(5);
                int nodeDepth = Integer.parseInt(nodeDepthAsString);
                Node node = (Node) session.getAttribute("node");
                if (null == node) {
                    throw new AssertionError();
                }
                for (int i = 0; i < nodeDepth; i++) {
                    node = node.getChild();
                }
                String value = request.getParameter(key);
                if (value.equals("")) {
                    continue;
                }
                node.setFieldValue(value);
            }
        }
        
        return (Node) session.getAttribute("node");
    }
    
}
