/*
 * Created on Feb 16, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.codehaus.wadi.sandbox.context.impl;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.sandbox.context.Collapser;
import org.codehaus.wadi.sandbox.context.Contextualiser;
import org.codehaus.wadi.sandbox.context.Evicter;


/**
 * @author jules
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public abstract class AbstractMappedContextualiser extends
		AbstractChainedContextualiser {

	protected final Log _log = LogFactory.getLog(getClass());
	protected final Map _map;
	protected final Evicter _evicter;
	
	/**
	 * @param next
	 */
	public AbstractMappedContextualiser(Contextualiser next, Collapser collapser, Map map, Evicter evicter) {
		super(next, collapser);
		_map=map;
		_evicter=evicter;
	}
	
	protected String _stringPrefix="<"+getClass().getName()+":";
	protected String _stringSuffix=">";
	public String toString() {
		return new StringBuffer(_stringPrefix).append(_map.size()).append(_stringSuffix).toString();
	}
}
