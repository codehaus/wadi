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
import java.sql.SQLException;

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

/**
 * TODO - JavaDoc this type
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class SharedJDBCContextualiser extends AbstractChainedContextualiser {
	protected final Log _log=LogFactory.getLog(getClass());
	protected final DataSource _dataSource;
	protected final String _table;
	protected final Immoter _immoter;
	protected final Emoter _emoter;

	public SharedJDBCContextualiser(Contextualiser next, Collapser collapser, Evicter evicter, DataSource dataSource, String table) {
		super(next, collapser, evicter);
		_dataSource=dataSource;
		_table=table;

		_immoter=new SharedJDBCImmoter();
		_emoter=new SharedJDBCEmoter();
	}
	
	public Immoter getImmoter(){return _immoter;}
	public Emoter getEmoter(){return _emoter;}

	public Immoter getDemoter(String id, Motable motable) {
		// TODO - should check _next... - just remove when we have an evicter sorted
		return new SharedJDBCImmoter();
	}

	public boolean contextualiseLocally(HttpServletRequest hreq, HttpServletResponse hres,
			FilterChain chain, String id, Immoter immoter, Sync promotionLock) throws IOException, ServletException {
		if (immoter!=null) {
			Motable emotable=new SharedJDBCMotable();
			emotable.setId(id);
			return contextualiseElsewhere(hreq, hres, chain, id, immoter, promotionLock, emotable);
		} else
			return false;
		// TODO - consider how to contextualise locally... should be implemented in ChainedContextualiser, as should this..
	}
	
	public void evict() {
		// TODO - NYI
	}
	
	public boolean isLocal(){return false;}
	
	public class SharedJDBCEmoter extends ChainedEmoter {
		
		public boolean prepare(String id, Motable emotable, Motable immotable) {
			if (super.prepare(id, emotable, immotable)) {
				try {
					Connection connection=_dataSource.getConnection();
					((SharedJDBCMotable)emotable).setTable(_table);
					((SharedJDBCMotable)emotable).setConnection(connection);
					if (SharedJDBCMotable.load(connection, _table, emotable)==null)
						return false;
				} catch (Exception e) {
					_log.error("could not establish database connection", e);
					return false;
				}
				return true;
			} else
				return false;
		}
		
		public void commit(String id, Motable emotable) {
			super.commit(id, emotable);
			SharedJDBCMotable sjm=((SharedJDBCMotable)emotable);
			Connection connection=sjm.getConnection();
			sjm.setConnection(null);
			try {
				connection.close();
			} catch (SQLException e) {
				_log.error("could not close database connection", e);
			}
		}
		
		public void rollback(String id, Motable emotable) {
			super.rollback(id, emotable);
			SharedJDBCMotable sjm=((SharedJDBCMotable)emotable);
			Connection connection=sjm.getConnection();
			sjm.setConnection(null);
			try {
				connection.rollback();
				connection.close();
			} catch (SQLException e) {
				_log.error("could not rollback database connection", e);
			}
		}
		
		// TODO - abstract common code between this and Imoter...
			
		public String getInfo(){return "database";}
		
	};
		
	/**
	 * An Emmoter that deals in terms of SharedJDBCMotables
	 *
	 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
	 * @version $Revision$
	 */
	public class SharedJDBCImmoter extends AbstractImmoter {	
		
		public Motable nextMotable(String id, Motable emotable) {
			Motable motable=new SharedJDBCMotable(); // TODO - could be a thread local... - used then discarded immediately
			motable.setId(id);			
			return motable;
		}
		
		public boolean prepare(String id, Motable emotable, Motable immotable) {
			try {
				Connection connection=_dataSource.getConnection();
				SharedJDBCMotable sjm=(SharedJDBCMotable)immotable;
				sjm.setTable(_table);
				sjm.setConnection(connection);
			} catch (Exception e) {
				_log.error("could not establish database connection", e);
				return false;
			}
			
			return super.prepare(id, emotable, immotable);
		}
		
		public void commit(String id, Motable immotable) {
			super.commit(id, immotable);
			SharedJDBCMotable sjm=((SharedJDBCMotable)immotable);
			Connection connection=sjm.getConnection();
			sjm.setConnection(null);
			try {
				connection.close();
			} catch (SQLException e) {
				_log.error("could not close database connection", e);
			}
		}
		
		public void rollback(String id, Motable immotable) {
			super.rollback(id, immotable);
			SharedJDBCMotable sjm=((SharedJDBCMotable)immotable);
			Connection connection=sjm.getConnection();
			sjm.setConnection(null);
			try {
				connection.rollback();
				connection.close();
			} catch (SQLException e) {
				_log.error("could not rollback database connection", e);
			}
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
