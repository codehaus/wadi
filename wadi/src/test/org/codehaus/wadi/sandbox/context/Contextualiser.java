/*
 * Created on Feb 16, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.codehaus.wadi.sandbox.context;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import EDU.oswego.cs.dl.util.concurrent.Sync;



/**
 * @author jules
 *
 * Contextualising a request is realising/processing it within the correct Context, i.e. in the presence of the required HttpSession, if any.
 * 
 * A Contextualiser can choose to either process the request within itself, or promote a Context to its caller, within which the request may be processed.
 * It should indicate to its caller, via return code, whether said processing has already been carried out or not.
 */
public interface Contextualiser {
	
	// I'd like to add Manager to param list, but it bloats dependency tree - can we get along without it ?
	// FilterChain.doFilter() throws IOException, ServletException...
	boolean contextualise(ServletRequest req, ServletResponse res, FilterChain chain, String id, Promoter promoter, Sync overlap) throws IOException, ServletException;

}
