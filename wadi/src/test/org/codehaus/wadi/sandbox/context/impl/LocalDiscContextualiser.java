/*
 * Created on Feb 16, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.codehaus.wadi.sandbox.context.impl;

import java.io.IOException;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.sandbox.context.Collapser;
import org.codehaus.wadi.sandbox.context.Context;
import org.codehaus.wadi.sandbox.context.Contextualiser;
import org.codehaus.wadi.sandbox.context.Promoter;

import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * @author jules
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class LocalDiscContextualiser extends AbstractMappedContextualiser {
	protected final Log _log = LogFactory.getLog(getClass());
	
	/**
	 * 
	 */
	public LocalDiscContextualiser(Collapser collapser, Contextualiser next, Map map) {
		super(collapser, next, map);
	}

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.sandbox.context.Contextualiser#contextualise(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain, java.lang.String, org.codehaus.wadi.sandbox.context.Contextualiser)
	 */
	public boolean contextualiseLocally(ServletRequest req, ServletResponse res,
		FilterChain chain, String id, Promoter promoter, Sync overlap) throws IOException, ServletException {
		Context c=(Context)_map.get(id); // TODO - should load from disc
		_log.info("promoting: "+id);
		promoter.promoteAndContextualise(req, res, chain, id, c, overlap); // inject result into our caller - now available to new threads
		// TODO - remove file here...
		return true;
	}
	
	public Promoter getPromoter(Promoter promoter){return promoter;} // just pass contexts straight through...
}
