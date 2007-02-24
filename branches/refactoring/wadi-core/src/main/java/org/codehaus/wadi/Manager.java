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
package org.codehaus.wadi;

import org.codehaus.wadi.web.WebSession;
import org.codehaus.wadi.web.WebSessionPool;
import org.codehaus.wadi.web.WebSessionWrapperFactory;

/**
 * Manager - A Session Manager abstraction. Responsibilities include Session creation, destruction, storage
 * and the processing of Invocations in the presence of the Session.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public interface Manager extends Lifecycle {
    
    /**
     * Initialise the Manager with information not readily available at construction time.
     * 
     * @param config An object carrying configuration informations
     */
    void init(ManagerConfig config);

    WebSession createWithName(String name) throws SessionAlreadyExistException;

    /**
     * Create a new Session
     * 
     * @param invocation the Invocation
     * @return the Session  
     */
    WebSession create(Invocation invocation); // should return Session, not WebSession
    
    /**
     * Destroy a Session
     * 
     * @param invocation the Invocation
     * @param session the Session
     */
    void destroy(Invocation invocation, WebSession session);

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
     * @return The Pool from/to which Sessions are allocated/returned
     */
    WebSessionPool getSessionPool(); // should be a SessionPool, not a WebSessionPool
    
    /**
     * @return The factory responsible for the generation of Session IDs
     */
    SessionIdFactory getSessionIdFactory();

    /**
     * Return a flag which indicates whether failure to find the relevant Session for an
     * Invocation should be considered an error or not.
     * 
     * @return whether failure to find a Session should be considered an error
     */
    boolean getErrorIfSessionNotAcquired();

    /**
     * Contextualise an Invocation - somehow colocate the Invocation and the relevant Session within the same
     * JVM somewhere in the Cluster, give the Invocation the Session and invoke() it. The Manager possesses
     * a stack of Contextualisers, down which the Invocation is passed in order to achieve this.
     * 
     * @param invocation
     * @throws InvocationException
     */
    boolean contextualise(Invocation invocation) throws InvocationException;
    
    WebSessionWrapperFactory getSessionWrapperFactory();
}