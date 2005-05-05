/**
 *
 * Copyright 2003-2005 Core Developers Network Ltd.
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
package org.codehaus.wadi.impl;

// TODO - a Disc-based equivalent...

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.codehaus.wadi.Collapser;
import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.ContextualiserConfig;
import org.codehaus.wadi.Emoter;
import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.Motable;

import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * A Contextualiser which stores its Contexts in a shared database via JDBC.
 * On shutdown of the cluster's last node, all extant sessions will be demoted to here.
 * On startup of the cluster's first node, all sessions stored here will be promoted upwards.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class SharedJDBCContextualiser extends AbstractSharedContextualiser {
	protected final DataSource _dataSource;
	protected final String _table;
	protected final Immoter _immoter;
	protected final Emoter _emoter;

	public SharedJDBCContextualiser(Contextualiser next, Collapser collapser, boolean clean, DataSource dataSource, String table) {
		super(next, new CollapsingLocker(collapser), clean);
		_dataSource=dataSource;
		_table=table;

		_immoter=new SharedJDBCImmoter();
		_emoter=new SharedJDBCEmoter();
	}

    public void init(ContextualiserConfig config) {
        super.init(config);
        
        if (_clean) {
            Connection connection=null;
            Statement s=null;
            try {
                connection=_dataSource.getConnection();
                s=connection.createStatement();
                s.executeUpdate("DELETE FROM "+_table);
                if (_log.isTraceEnabled()) _log.trace("removed (shared database) sessions"); // TODO - how many ?
            } catch (SQLException e) {
                if (_log.isErrorEnabled()) _log.error("remove (shared database) failed", e);
            } finally {
                try {
                    if (s!=null)
                        s.close();
                } catch (SQLException e) {
                    _log.warn("problem closing database statement", e);
                }
                try {
                    if (connection!=null)
                        connection.close();
                } catch (SQLException e) {
                    _log.warn("problem closing database connection", e);
                }
            }
        }
    }
    
	public Immoter getImmoter(){return _immoter;}
	public Emoter getEmoter(){return _emoter;}

	public Immoter getDemoter(String id, Motable motable) {
		// TODO - should check _next... - just remove when we have an evicter sorted
		return new SharedJDBCImmoter();
	}

	public Motable get(String id) {
	    Motable motable=new SharedJDBCMotable();
		motable.setId(id);
		return motable;
	}

	/**
	 * An Emoter that deals in terms of SharedJDBCMotables
	 *
	 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
	 * @version $Revision$
	 */
	public class SharedJDBCEmoter extends AbstractChainedEmoter {

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

		public String getInfo(){return "shared database";}

	}

	/**
	 * An Immoter that deals in terms of SharedJDBCMotables
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

		public String getInfo() {
			return "shared database";
		}
	}

    public int loadMotables(Emoter emoter, Immoter immoter) {
        // this should only happen when we are the first node in the cluster...
        try {
            return SharedJDBCMotable.load(_dataSource.getConnection(), _table, emoter, immoter, _config.getAccessOnLoad());
        } catch (Exception e) {
            _log.error("problem loading data from db", e);
            return 0;
        }
    }

    public Emoter getEvictionEmoter() {throw new UnsupportedOperationException();} // FIXME
    public void expire(Motable motable) {throw new UnsupportedOperationException();} // FIXME

    /**
     * Shared Contextualisers do nothing at runtime. They exist only to load data at startup and store it at shutdown.
     */
    public boolean contextualise(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Immoter immoter, Sync motionLock, boolean exclusiveOnly) throws IOException, ServletException {
        return false;
    }
    
}
