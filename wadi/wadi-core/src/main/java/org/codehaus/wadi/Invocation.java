/**
 *
 * Copyright 2005 Core Developers Network Ltd.
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


/**
 * @version $Revision$
 */
public interface Invocation {

    /**
     * prepare this Invocation for recycling via a Pool
     */
    void clear();
    
    /**
     * @return the key associating a Session with this Invocation
     */
    String getKey();
    
    /**
     * Attach the relevant session to this Invocation for its lifetime
     * 
     * @param session The Session
     */
    public void setSession(Session session);
    
    /**
     * Send an error back to the client from which the Invocation originated
     * 
     * @param code an error code
     * @param message an error message
     * @throws InvocationException
     */
    void sendError(int code, String message) throws InvocationException; // a little web specific ?
    
    /**
     * @return whether or not this Invocation knows how to relocate itself to a given EndPoint
     */
    boolean getRelocatable();
    
    /**
     * Ask this Invocation to relocate itself to a given EndPoint
     * 
     * @param endPoint the EndPoint
     */
    public void relocate(EndPoint endPoint);
    
    // old
    
	void invoke(PoolableInvocationWrapper wrapper) throws InvocationException;
	
	void invoke() throws InvocationException;
	
	boolean isProxiedInvocation();

}
