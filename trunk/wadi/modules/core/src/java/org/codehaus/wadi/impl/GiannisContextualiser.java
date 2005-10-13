package org.codehaus.wadi.impl;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import org.codehaus.wadi.Collapser;
import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.ContextualiserConfig;
import org.codehaus.wadi.DistributableContextualiserConfig;
import org.codehaus.wadi.Emoter;
import org.codehaus.wadi.Evicter;
import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.Motable;
import org.codehaus.wadi.Store;
import org.codehaus.wadi.StoreMotable;

/**
 * Maps id:File where file contains Context content...
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class GiannisContextualiser extends AbstractExclusiveContextualiser {

    protected final Immoter _immoter;
	protected final Emoter _emoter;
	protected final DatabaseStore _dbstore;

	public GiannisContextualiser(Contextualiser next, Collapser collapser, boolean clean, Evicter evicter, Map map, DatabaseStore dbstore) throws Exception {
	    super(next, new CollapsingLocker(collapser), clean, evicter, map);
	    _immoter=new ExclusiveStoreImmoter(_map);
	    _emoter=new ExclusiveStoreEmoter(_map);
	    _dbstore=dbstore;
	}

    public void init(ContextualiserConfig config) {
        super.init(config);
        // perhaps this should be done in start() ?
        if (_clean)
            _dbstore.clean();
    }
    
    public String getStartInfo() {
        return "["+_dbstore.getStartInfo()+"]";
    }
    
	public boolean isExclusive(){
		return true;
	}

	public Immoter getImmoter() {
		return _immoter;
	}
	
	public Emoter getEmoter(){
		return _emoter;
	}
	
	class ExclusiveStoreImmoter extends AbstractMappedImmoter {

	    public ExclusiveStoreImmoter(Map map) {
	        super(map);
	    }

		public Motable nextMotable(String name, Motable emotable) {
            return _dbstore.create(); // TODO - Pool, maybe as ThreadLocal
		}

		public boolean prepare(String name, Motable emotable, Motable immotable) {
			try {
				Connection connection=_dbstore.getDataSource().getConnection();
				DatabaseMotable motable=(DatabaseMotable)immotable;
				motable.setConnection(connection);
                motable.init(_dbstore);
			} catch (Exception e) {
                if (_log.isErrorEnabled()) _log.error("store ("+_dbstore.getDescription()+") failed", e);
				return false;
			}

			return super.prepare(name, emotable, immotable);
		}
		
		public void commit(String name, Motable immotable) {
			DatabaseMotable motable=((DatabaseMotable)immotable);
			Connection connection=motable.getConnection();
			motable.setConnection(null);
			try {
				connection.close();
			} catch (SQLException e) {
                if (_log.isWarnEnabled()) _log.warn("store ("+_dbstore.getDescription()+") problem", e);
			}
			super.commit(name, immotable);
		}

		public void rollback(String name, Motable immotable) {
			super.rollback(name, immotable);
			DatabaseMotable motable=((DatabaseMotable)immotable);
			Connection connection=motable.getConnection();
			motable.setConnection(null);
			try {
				connection.rollback();
				connection.close();
			} catch (SQLException e) {
                if (_log.isWarnEnabled()) _log.warn("store ("+_dbstore.getDescription()+") problem", e);
			}
		}
		
		public String getInfo() {
			return _dbstore.getDescription();
		}
	}

	class ExclusiveStoreEmoter extends AbstractMappedEmoter {

		public ExclusiveStoreEmoter(Map map) {super(map);}

        public boolean prepare(String name, Motable emotable) {
            if (super.prepare(name, emotable)) { // removes from map - should not...
                try {
                    DatabaseMotable motable=(DatabaseMotable)emotable;
                    motable.setConnection(_dbstore.getConnection());
                    motable.init(_dbstore, name);   // waste of time
                } catch (Exception e) {
                    if (_log.isErrorEnabled()) _log.error("load ("+_dbstore.getDescription()+") failed", e);
                    return false;
                }
            } else
                return false;
            
            return true;
        }
        
		public void commit(String name, Motable emotable) {
			super.commit(name, emotable);
			DatabaseMotable motable=((DatabaseMotable)emotable);
			Connection connection=motable.getConnection();
			motable.setConnection(null);
			try {
				connection.close();
			} catch (SQLException e) {
				if (_log.isWarnEnabled()) _log.warn("load ("+_dbstore.getDescription()+") problem", e);
			}
		}

		public void rollback(String name, Motable emotable) {
			super.rollback(name, emotable);
			DatabaseMotable motable=((DatabaseMotable)emotable);
			Connection connection=motable.getConnection();
			motable.setConnection(null);
			try {
				connection.rollback();
				connection.close();
			} catch (SQLException e) {
                if (_log.isWarnEnabled()) _log.warn("load ("+_dbstore.getDescription()+") problem", e);
			}
		}
		
		public String getInfo() {
			return _dbstore.getDescription();
		}
	}

    public void start() throws Exception {
        Store.Putter putter=new Store.Putter() {
            public void put(String name, Motable motable) {
                _map.put(name, motable);
            }
        };
        _dbstore.load(putter, ((DistributableContextualiserConfig)_config).getAccessOnLoad());
        super.start(); // continue down chain...
    }

    // this should move up.....
    public void expire(Motable motable) {
        // decide whether session needs promotion
        boolean needsLoading=true; // FIXME
        // if so promote to top and expire there
        String id=motable.getName();
        if (_log.isTraceEnabled()) _log.trace("expiring from disc: "+id);
        if (needsLoading) {
            _map.remove(id);
            Motable loaded=_config.getSessionPool().take();
            try {
                loaded.copy(motable);
                motable=null;
                _config.expire(loaded);
            } catch (Exception e) {
                _log.error("unexpected problem expiring from disc", e);
            }
            loaded=null;
        } else {
            // else, just drop it off the disc here...
            throw new UnsupportedOperationException(); // FIXME
        }
    }

}
