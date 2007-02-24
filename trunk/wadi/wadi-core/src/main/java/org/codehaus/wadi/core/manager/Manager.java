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
package org.codehaus.wadi.core.manager;

import org.codehaus.wadi.core.Lifecycle;
import org.codehaus.wadi.core.contextualiser.Invocation;
import org.codehaus.wadi.core.contextualiser.InvocationException;
import org.codehaus.wadi.core.session.Session;
import org.codehaus.wadi.servicespace.ServiceName;

/**
 * Manager - A Session Manager abstraction. Responsibilities include Session creation, destruction, storage
 * and the processing of Invocations in the presence of the Session.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public interface Manager extends Lifecycle {
    public static final ServiceName NAME = new ServiceName("Manager");
    
    /**
     * Initialise the Manager with information not readily available at construction time.
     * 
     * @param config An object carrying configuration informations
     */
    void init(ManagerConfig config);

    Session createWithName(String name) throws SessionAlreadyExistException;

    /**
     * Create a new Session
     * 
     * @param invocation the Invocation
     * @return the Session  
     */
    Session create(Invocation invocation);
    
    /**
     * Destroy a Session
     * 
     * @param session the Session
     */
    void destroy(Session session);

    /**
     * @return The maximum amount of time (in seconds) that a Session
     * may lie inactive (i.e. without receiving an Invocation) before it 
     * should be garbage collected.
     */
    int getMaxInactiveInterval();

    /**
     * @param The maximum amount of time (in seconds) that a Session
     * may lie inactive (i.e. without receiving an Invocation) before it 
     * should be garbage collected.
     */
    void setMaxInactiveInterval(int interval);

    /**
     * @return The factory responsible for the generation of Session IDs
     */
    SessionIdFactory getSessionIdFactory();

    /**
     * Contextualise an Invocation - somehow colocate the Invocation and the relevant Session within the same
     * JVM somewhere in the Cluster, give the Invocation the Session and invoke() it. The Manager possesses
     * a stack of Contextualisers, down which the Invocation is passed in order to achieve this.
     * 
     * @param invocation
     * @throws InvocationException
     */
    boolean contextualise(Invocation invocation) throws InvocationException;
}