/*
 * Created on Feb 16, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.codehaus.wadi.sandbox.context.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.StreamingStrategy;
import org.codehaus.wadi.sandbox.context.Context;
import org.codehaus.wadi.sandbox.context.Contextualiser;
import org.codehaus.wadi.sandbox.context.Promoter;

import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * @author jules
 *
 * Maps id:File where file contains Context content...
 */
public class LocalDiscContextualiser extends AbstractMappedContextualiser {
	protected final Log _log = LogFactory.getLog(getClass());
	protected final StreamingStrategy _streamingStrategy;
	
	/**
	 * 
	 */
	public LocalDiscContextualiser(Contextualiser next, Map map, StreamingStrategy streamingStrategy) {
		super(next, map);
		_streamingStrategy=streamingStrategy;
	}

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.sandbox.context.Contextualiser#contextualise(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain, java.lang.String, org.codehaus.wadi.sandbox.context.Contextualiser)
	 */
	public boolean contextualiseLocally(ServletRequest req, ServletResponse res,
		FilterChain chain, String id, Promoter promoter, Sync promotionMutex) throws IOException, ServletException {
		File file=(File)_map.get(id);
		if (file.exists()) {
			Context context=promoter.nextContext();
			ObjectInput oi=_streamingStrategy.getInputStream(new FileInputStream(file));
			try {
				context.readContent(oi);
			} catch (ClassNotFoundException e) {
				throw new ServletException("problem loading context from local disc: "+id, e);
			}
			oi.close();
			_log.info("loaded (local disc): "+id+" : "+context);
			_log.info("promoting: "+id);
			promoter.promoteAndContextualise(req, res, chain, id, context, promotionMutex); // inject result into our caller - now available to new threads
			file.delete(); // perhaps this should be wrapped up in a callback object and passed up to the promoter with the promotionMutex - otherwise file is not removed until after request has run...
			_log.info("removed (local disc): "+id);
			return true;
		} else {
			return false;
		}
	}
	
	public Promoter getPromoter(Promoter promoter){return promoter;} // just pass contexts straight through...
}
