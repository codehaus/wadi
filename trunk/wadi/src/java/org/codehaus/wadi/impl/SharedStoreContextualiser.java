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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Collapser;
import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.ContextualiserConfig;
import org.codehaus.wadi.Emoter;
import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.Motable;
import org.codehaus.wadi.Store;
import org.codehaus.wadi.StoreMotable;

import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * A Contextualiser which stores its Contexts in a shared database via JDBC.
 * On shutdown of the cluster's last node, all extant sessions will be demoted to here.
 * On startup of the cluster's first node, all sessions stored here will be promoted upwards.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class SharedStoreContextualiser extends AbstractSharedContextualiser {
    
    public static class DatabaseStore implements Store {

        protected final Log _log=LogFactory.getLog(getClass());
        protected final DataSource _dataSource;
        protected final String _table;
       
        public DatabaseStore(DataSource dataSource, String table) {
            _dataSource=dataSource;
            _table=table;
        }
        
        public DataSource getDataSource() {return _dataSource;}
        public String getTable() {return _table;}
        
        // Store
        
        public void clean() {
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
        
        public void load(Putter putter, boolean accessOnLoad) {
            long time=System.currentTimeMillis();
            Statement s=null;
            int count=0;
            Connection connection=null;
            try {
                connection=_dataSource.getConnection();
                s=connection.createStatement();
                ResultSet rs=s.executeQuery("SELECT Id, CreationTime, LastAccessedTime, MaxInactiveInterval, Bytes FROM "+_table);
                String name=null;
                while (rs.next()) {
                    try {
                        int i=1;
                        Motable motable=new SharedJDBCMotable();
                        name=(String)rs.getObject(i++);
                        long creationTime=rs.getLong(i++);
                        long lastAccessedTime=rs.getLong(i++);
                        lastAccessedTime=accessOnLoad?time:lastAccessedTime;
                        int maxInactiveInterval=rs.getInt(i++);
                        motable.init(creationTime, lastAccessedTime, maxInactiveInterval, name);
                        motable.setBodyAsByteArray((byte[])rs.getObject(i++));
                        if (motable.getTimedOut(time)) {
                            if (_log.isWarnEnabled()) _log.warn("LOADED DEAD SESSION: "+motable.getName());
                            // we should expire it immediately, rather than promoting it...
                            // perhaps we could be even cleverer ?
                        }
                        putter.put(name, motable);
                        count++;
                    } catch (Exception e) {
                       if (_log.isErrorEnabled()) _log.error("load (shared database) failed: "+name, e);
                    }
                }
                _log.info("loaded sessions: "+count);
            } catch (SQLException e) {
                _log.warn("list (shared database) failed", e);
            } finally {
                if (s!=null)
                    try {
                        s.close();
                    } catch (SQLException e) {
                        if (_log.isWarnEnabled()) _log.warn("load (shared database) problem", e);
                    }
            }

            try {
                s=connection.createStatement();
                s.executeUpdate("DELETE FROM "+_table);
            } catch (SQLException e) {
                _log.warn("removal (shared database) failed", e);
            } finally {
                if (s!=null)
                    try {
                        s.close();
                    } catch (SQLException e) {
                        if (_log.isWarnEnabled()) _log.warn("load (shared database) problem", e);
                    }
            }
        }

        public String getStartInfo() {
            return _dataSource.toString();
        }

        public String getDescription() {
            return "shared database";
        }

        public StoreMotable create() {
            return null; //new SharedJDBCImmoter(); - TODO
        }
    }
    
    
    protected final DatabaseStore _store;
	protected final Immoter _immoter;
	protected final Emoter _emoter;

	public SharedStoreContextualiser(Contextualiser next, Collapser collapser, boolean clean, DataSource dataSource, String table) {
		super(next, new CollapsingLocker(collapser), clean);
        _store=new DatabaseStore(dataSource, table);
		_immoter=new SharedJDBCImmoter();
		_emoter=new SharedJDBCEmoter();
	}

    public void init(ContextualiserConfig config) {
        super.init(config);
        if (_clean)
            _store.clean();
    }
    
	public Immoter getImmoter(){return _immoter;}
	public Emoter getEmoter(){return _emoter;}

	public Immoter getDemoter(String id, Motable motable) {
		// TODO - should check _next... - just remove when we have an evicter sorted
		return new SharedJDBCImmoter();
	}

	public Motable get(String id) {
        throw new UnsupportedOperationException();
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
					Connection connection=_store.getDataSource().getConnection();
					((SharedJDBCMotable)emotable).setTable(_store.getTable());
					((SharedJDBCMotable)emotable).setConnection(connection);
					if (SharedJDBCMotable.load(connection, _store.getTable(), emotable)==null)
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

		public String getInfo() {
            return _store.getDescription();
            }
        
        public String getStartInfo() {
            return _store.getStartInfo();
        }

	}

	/**
	 * An Immoter that deals in terms of SharedJDBCMotables
	 *
	 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
	 * @version $Revision$
	 */
	public class SharedJDBCImmoter extends AbstractImmoter {

		public Motable nextMotable(String id, Motable emotable) {
            return new SharedJDBCMotable();  // TODO - Pool this - could be ThreadLocal...
		}

		public boolean prepare(String id, Motable emotable, Motable immotable) {
			try {
				Connection connection=_store.getDataSource().getConnection();
				SharedJDBCMotable sjm=(SharedJDBCMotable)immotable;
				sjm.setTable(_store.getTable());
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

    class SharedPutter implements Store.Putter {

        protected final Emoter _emoter;
        protected final Immoter _immoter;
        
        public SharedPutter(Emoter emoter, Immoter immoter) {
            _emoter=emoter;
            _immoter=immoter;
        }
        
        public void put(String name, Motable motable) {
            Utils.mote(_emoter, _immoter, motable, name);
        }
    };
    
    public void load(Emoter emoter, Immoter immoter) {
        // this should only happen when we are the first node in the cluster...
        _store.load(new SharedPutter(emoter, immoter), _config.getAccessOnLoad());
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
