/*
 * Created on Feb 16, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.codehaus.wadi.sandbox.context.impl;

import java.util.Map;

import org.codehaus.wadi.sandbox.context.Contextualiser;

/**
 * @author jules
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public abstract class AbstractMappedContextualiser extends
		AbstractChainedContextualiser {

	protected final Map _map;
	/**
	 * @param next
	 */
	public AbstractMappedContextualiser(Contextualiser next, Map map) {
		super(next);
		_map=map;
	}
	
	protected String _stringPrefix="<"+getClass().getName()+":";
	protected String _stringSuffix=">";
	public String toString() {
		return new StringBuffer(_stringPrefix).append(_map.size()).append(_stringSuffix).toString();
	}

}
