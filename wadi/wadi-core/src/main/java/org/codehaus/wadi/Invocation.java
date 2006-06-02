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
     * Prepare this Invocation for recycling via a Pool
     */
    void clear();
    
    /**
     * Return the key carried by the Invocation associating it with server-side state - a Session
     * 
     * @return the key
     */
    String getSessionKey();
    
    /**
     * Attach the relevant Session to this Invocation for its lifetime
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
     * Does this Invocation know how to 'relocate' itself to another EndPoint ?
     * 
     * @return whether the Invocation is capable of relocating itself.
     */
    boolean getRelocatable();
    
    /**
     * Ask this Invocation to relocate itself to a given EndPoint
     * 
     * @param endPoint the EndPoint
     */
    public void relocate(EndPoint endPoint);
    
    
	/**
     * Actually make the encapsulated Invocation. Called when required environment has been prepared -
     * i.e. Session has been locked into memory and attached to the Invocation via setSession().
     * 
	 * @throws InvocationException
	 */
	void invoke() throws InvocationException;
    
    
	// these two methods are left overs from web-specific time - on their way out...
    
    void invoke(PoolableInvocationWrapper wrapper) throws InvocationException;
    
	boolean isProxiedInvocation();

}