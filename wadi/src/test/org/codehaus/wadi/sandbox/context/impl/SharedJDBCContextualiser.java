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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.SerializableContent;
import org.codehaus.wadi.StreamingStrategy;
import org.codehaus.wadi.sandbox.context.Collapser;
import org.codehaus.wadi.sandbox.context.Context;
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
	protected final Log _log = LogFactory.getLog(getClass());

	protected final Evicter _evicter;
	protected final DataSource _ds;
	protected final String _table;
	protected final StreamingStrategy _ss;
	/**
	 * @param next
	 */
	public SharedJDBCContextualiser(Contextualiser next, Collapser collapser, Evicter evicter, DataSource ds, String table, StreamingStrategy ss) {
		super(next, collapser);
		_evicter=evicter;
		_ds=ds;
		_table=table;
		_ss=ss;
	}

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.sandbox.context.impl.AbstractChainedContextualiser#getPromoter(org.codehaus.wadi.sandbox.context.Promoter)
	 */
	public Promoter getPromoter(Promoter promoter){return promoter;} // just pass contexts straight through...

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.sandbox.context.impl.AbstractChainedContextualiser#contextualiseLocally(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain, java.lang.String, org.codehaus.wadi.sandbox.context.Promoter, EDU.oswego.cs.dl.util.concurrent.Sync)
	 */
	public boolean contextualiseLocally(ServletRequest req, ServletResponse res,
			FilterChain chain, String id, Promoter promoter, Sync promotionMutex) throws IOException, ServletException {
		Context context=null;
		try {
			Connection c=_ds.getConnection();
			Statement s=c.createStatement();

			ResultSet rs=s.executeQuery("SELECT MyValue FROM "+_table+" WHERE MyKey='"+id+"'");
			if (rs.next()) {
				Context sc=promoter.nextContext();
		    	ObjectInput oi=_ss.getInputStream(new ByteArrayInputStream((byte[])rs.getObject(1)));
		    	sc.readContent(oi);
		    	oi.close();

		    	context=sc;
		    	_log.info("loaded (database): "+id);
		      }

			// TODO - revisit...
		    if (context!=null) {
		    	_log.info("promoting (from database): "+id);
		    	if (promoter.prepare(id, context)) {
			    	int r=s.executeUpdate("DELETE FROM "+_table+" WHERE MyKey='"+id+"'");
			    	_log.info("removed (database): "+id);
		    		promoter.commit(id, context);
		    		promotionMutex.release();
		    		promoter.contextualise(req, res, chain, id , context);
		    	} else {
		    		promoter.rollback(id, context);
		    	}
		    }

		    s.close();
		    c.close();

		    return true;
		} catch (SQLException e) {
		    throw new ServletException("promotion (database) failed: "+id, e);
		} catch (ClassNotFoundException e2) {
			throw new ServletException("promotion (database) failed: "+id, e2);
		}

		//return false;
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
	public void demote(String key, Motable val) {
	    try
	    {
	    	_log.info("demoting (to database): "+key);
	      SerializableContent sc=(SerializableContent)val;

	      Connection c=_ds.getConnection();
	      PreparedStatement ps=c.prepareStatement("INSERT INTO "+_table+" (MyKey, MyValue) VALUES ('"+key.toString()+"', ?)");
	      ByteArrayOutputStream baos=new ByteArrayOutputStream();
	      ObjectOutput oos=_ss.getOutputStream(baos);
	      sc.writeContent(oos);
	      oos.flush();
	      oos.close();
	      ps.setObject(1, baos.toByteArray());
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
