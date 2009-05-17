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
package org.codehaus.wadi.servicespace.admin.commands;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.wadi.core.contextualiser.Contextualiser;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.servicespace.ServiceSpace;
import org.codehaus.wadi.servicespace.ServiceSpaceName;

/**
 * 
 * @version $Revision: 1538 $
 */
public class GetContextualiserInfoStack extends AbstractServiceCommand {

    public GetContextualiserInfoStack(ServiceSpaceName name) {
        super(name, ContextualiserStackExplorer.NAME);
    }

    protected Object execute(LocalPeer localPeer, ServiceSpace serviceSpace, Object service) {
        List contextualiserInfos = new ArrayList();
        
        ContextualiserStackExplorer stackExplorer = (ContextualiserStackExplorer) service;
        List contextualisers = stackExplorer.getContextualisers();
        for (int i = 0; i < contextualisers.size(); i++) {
            Contextualiser contextualiser = (Contextualiser) contextualisers.get(i);
            String contextualiserName = contextualiser.getClass().getName();
            contextualiserName = contextualiserName.substring(contextualiserName.lastIndexOf(".") + 1);
            
            contextualiserInfos.add(new ContextualiserInfo(contextualiserName, i));
        }
        
        return contextualiserInfos;
    }
    
}
