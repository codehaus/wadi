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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.codehaus.wadi.core.contextualiser.Contextualiser;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.servicespace.InvocationResultCombiner;
import org.codehaus.wadi.servicespace.ServiceSpace;
import org.codehaus.wadi.servicespace.ServiceSpaceName;
import org.codehaus.wadi.servicespace.resultcombiner.SuccessfullSetResultCombiner;

/**
 * 
 * @version $Revision: 1538 $
 */
public class GetSessionInfos extends AbstractServiceCommand {

    public GetSessionInfos(ServiceSpaceName name) {
        super(name, ContextualiserStackExplorer.NAME);
    }

    public InvocationResultCombiner getInvocationResultCombiner() {
        return SuccessfullSetResultCombiner.COMBINER;
    }
    
    protected Object execute(LocalPeer localPeer, ServiceSpace serviceSpace, Object service) {
        Set sessionInfos = new HashSet();
        
        ContextualiserStackExplorer stackExplorer = (ContextualiserStackExplorer) service;
        List contextualisers = stackExplorer.getContextualisers();
        for (int i = 0; i < contextualisers.size(); i++) {
            Contextualiser contextualiser = (Contextualiser) contextualisers.get(i);
            addSessionInfos(localPeer, contextualiser, i, sessionInfos);
        }
        
        return sessionInfos;
    }
    
    protected void addSessionInfos(LocalPeer localPeer, Contextualiser contextualiser, int index, Set sessionInfos) {
        String contextualiserName = contextualiser.getClass().getName();
        contextualiserName = contextualiserName.substring(contextualiserName.lastIndexOf(".") + 1);
        
        Set sessionNames = contextualiser.getSessionNames();
        for (Iterator iter = sessionNames.iterator(); iter.hasNext();) {
            String name = (String) iter.next();
            sessionInfos.add(new SessionInfo(localPeer, name, contextualiserName, index));
        }
    }

}
