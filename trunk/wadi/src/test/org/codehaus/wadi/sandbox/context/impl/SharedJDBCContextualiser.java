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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.StreamingStrategy;
import org.codehaus.wadi.sandbox.context.Collapser;
import org.codehaus.wadi.sandbox.context.Contextualiser;
import org.codehaus.wadi.sandbox.context.Evicter;
import org.codehaus.wadi.sandbox.context.Motable;
import org.codehaus.wadi.sandbox.context.Promoter;

import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * TODO - JavaDoc this type
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class SharedJDBCContextualiser extends AbstractChainedContextualiser {
	protected final Log _log=LogFactory.getLog(getClass());
	protected final Evicter _evicter;
	protected final StreamingStrategy _streamer;
	protected final DataSource _ds;
	protected final String _table;
	/**
	 * @param next
	 */
	public SharedJDBCContextualiser(Contextualiser next, Collapser collapser, Evicter evicter, StreamingStrategy streamer, DataSource ds, String table) {
		super(next, collapser);
		_evicter=evicter;
		_streamer=streamer;
		_ds=ds;
		_table=table;
	}
	
	/* (non-Javadoc)
	 * @see org.codehaus.wadi.sandbox.context.impl.AbstractChainedContextualiser#getPromoter(org.codehaus.wadi.sandbox.context.Promoter)
	 */
	public Promoter getPromoter(Promoter promoter){return promoter;} // just pass contexts straight through...
	
	/* (non-Javadoc)
	 * @see org.codehaus.wadi.sandbox.context.impl.AbstractChainedContextualiser#contextualiseLocally(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain, java.lang.String, org.codehaus.wadi.sandbox.context.Promoter, EDU.oswego.cs.dl.util.concurrent.Sync)
	 */
	public boolean contextualiseLocally(HttpServletRequest hreq, HttpServletResponse hres,
			FilterChain chain, String id, Promoter promoter, Sync promotionLock) throws IOException, ServletException {
		try {
			Connection c=_ds.getConnection();
			Statement s=c.createStatement();
			
			ResultSet rs=s.executeQuery("SELECT MyValue FROM "+_table+" WHERE MyKey='"+id+"'");
			if (rs.next()) {
				Motable motable=promoter.nextMotable();
				motable.setBytes((byte[])rs.getObject(1));
				_log.info("loaded (database): "+id);
				
				// TODO - revisit...
				_log.info("promoting (from database): "+id);
				if (promoter.prepare(id, motable)) {
					s.executeUpdate("DELETE FROM "+_table+" WHERE MyKey='"+id+"'");
					_log.info("removed (database): "+id);
					promoter.commit(id, motable);
					promotionLock.release();
					promoter.contextualise(hreq, hres, chain, id , motable);
				} else {
					promoter.rollback(id, motable);
				}
			}
			
			s.close();
			c.close();
			
			return true;
		} catch (Exception e) {
			_log.warn("promotion (database) failed: "+id, e);
			return false;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.codehaus.wadi.sandbox.context.Contextualiser#evict()
	 */
	public void evict() {
		// TODO - NYI
	}
	
	/* (non-Javadoc)
	 * @see org.codehaus.wadi.sandbox.context.Contextualiser#demote(java.lang.String, org.codehaus.wadi.sandbox.context.Motable)
	 */
	public void demote(String key, Motable motable) {
		try
		{
			_log.info("demoting (to database): "+key);
			Connection c=_ds.getConnection();
			PreparedStatement ps=c.prepareStatement("INSERT INTO "+_table+" (MyKey, MyValue) VALUES ('"+key.toString()+"', ?)");
			ps.setObject(1, motable.getBytes());
			ps.executeUpdate();
			ps.close();
			c.close();
			
			// we should write metadata like ttl into another column so it is queryable by the evicter and sysadmin..
			// do we need to worry about ttl ?
			//	long willTimeOutAt=impl.getLastAccessedTime()+(impl.getMaxInactiveInterval()*1000);
			//	file.setLastModified(willTimeOutAt);
			
			_log.info("stored (database): "+key);
		}
		catch (Exception e)
		{
			_log.error("eviction (database) failed: "+key, e);
		}
	}
	
	public boolean isLocal(){return false;}
}
