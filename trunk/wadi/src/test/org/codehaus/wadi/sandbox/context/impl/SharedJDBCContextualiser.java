/**
 *
 * Copyright 2003-2004 The Apache Software Foundation
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
package org.codehaus.wadi.sandbox.context.impl;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.sandbox.context.Collapser;
import org.codehaus.wadi.sandbox.context.Contextualiser;
import org.codehaus.wadi.sandbox.context.Emoter;
import org.codehaus.wadi.sandbox.context.Evicter;
import org.codehaus.wadi.sandbox.context.Immoter;
import org.codehaus.wadi.sandbox.context.Motable;

import EDU.oswego.cs.dl.util.concurrent.Sync;


//SORT OUT THIS FILE
//MOVE MOTABLE BACK IN
//USE SAME CONNECTION FOR WHOLE MOTION
//FIX UP REMAINING TESTS
//FIX SPELLING ON EMOTER/ABLE/...
//FIX SPELLING ON EMMIGRAT...
//ETC...
//RENAME CONTEXTUALISERS TO STORES
//REFACTOR MEMORY STORE /ABSTRACTMAPPED STORE TO BE MORE ALIKE


/**
 * TODO - JavaDoc this type
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class SharedJDBCContextualiser extends AbstractChainedContextualiser {
	protected final Log _log=LogFactory.getLog(getClass());
	protected final DataSource _ds;
	protected final String _table;
	protected final Immoter _immoter;
	protected final Emoter _emoter;

	public SharedJDBCContextualiser(Contextualiser next, Collapser collapser, Evicter evicter, DataSource ds, String table) {
		super(next, collapser, evicter);
		_ds=ds;
		_table=table;

		_immoter=new SharedJDBCImmoter();
		_emoter=new ChainedEmoter() {public String getInfo(){return "database";}}; // overwrite - yeugh ! - fix when we have a LifeCycle
	}
	
	public Immoter getImmoter(){return _immoter;}
	public Emoter getEmoter(){return _emoter;}

	public Immoter getPromoter(Immoter immoter) {
		return immoter; // just pass contexts straight through...
	}
	
	public Immoter getDemoter(String id, Motable motable) {
		// TODO - should check _next... - just remove when we have an evicter sorted
		return new SharedJDBCImmoter();
	}

	public boolean contextualiseLocally(HttpServletRequest hreq, HttpServletResponse hres,
			FilterChain chain, String id, Immoter immoter, Sync promotionLock) throws IOException, ServletException {
//			Connection c=_ds.getConnection();
//			Statement s=c.createStatement();
			
			if (immoter!=null) {
				Motable emotable=new SharedJDBCImmoter().nextMotable(id, null); // TODO - WRONG - tmp hack
				return contextualiseElsewhere(hreq, hres, chain, id, immoter, promotionLock, emotable);
			} else
				return false;
			// TODO - consider how to contextualise locally... should be implemented in ChainedContextualiser, as should this..
	}
	
	public void evict() {
		// TODO - NYI
	}
	
	public boolean isLocal(){return false;}
	
	/**
	 * An Immoter that deals in terms of LocalDiscMotables
	 *
	 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
	 * @version $Revision$
	 */
	public class SharedJDBCImmoter extends AbstractImmoter {	
		
		public Motable nextMotable(String id, Motable emotable) {
			SharedJDBCMotable sjm=new SharedJDBCMotable();
			sjm.setId(id);
			sjm.setDataSource(_ds);
			sjm.setTable(_table);
			return sjm;
		}
		
		public void commit(String id, Motable immotable) {
			//_log.info("insertion (database): "+id);
		}
		
		public void contextualise(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Motable immotable) throws IOException, ServletException {
			// TODO - we need to pass through a promotionLock
//			contextualiseLocally(hreq, hres, chain, id, new NullSync(), motable);
		}
		
		public String getInfo() {
			return "database";
		}
	}
}
