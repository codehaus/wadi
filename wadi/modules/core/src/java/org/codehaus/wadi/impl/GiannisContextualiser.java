package org.codehaus.wadi.impl;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import org.codehaus.wadi.Collapser;
import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.ContextualiserConfig;
import org.codehaus.wadi.Emoter;
import org.codehaus.wadi.Evicter;
import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.Motable;

/**
 * Maps id:File where file contains Context content...
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class GiannisContextualiser extends AbstractExclusiveContextualiser {

    protected final Immoter _immoter;
	protected final Emoter _emoter;
	protected final DatabaseStore _store;

	public GiannisContextualiser(Contextualiser next, Collapser collapser, boolean clean, Evicter evicter, Map map, DatabaseStore dbstore) throws Exception {
	    super(next, new CollapsingLocker(collapser), clean, evicter, map);
	    _immoter=new GiannisImmoter(_map);
	    _emoter=new GiannisEmoter(_map);
	    _store=dbstore;
	}

    public void init(ContextualiserConfig config) {
        super.init(config);
        // perhaps this should be done in start() ?
        if (_clean)
            _store.clean();
    }
    
    public String getStartInfo() {
        return "["+_store.getStartInfo()+"]";
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
	
	class GiannisImmoter extends AbstractMappedImmoter {

	    public GiannisImmoter(Map map) {
	        super(map);
	    }

		public Motable nextMotable(String name, Motable emotable) {
            return _store.create(); // TODO - Pool, maybe as ThreadLocal
		}

		public boolean prepare(String name, Motable emotable, Motable immotable) {
			try {
				Connection connection=_store.getDataSource().getConnection();
				DatabaseMotable motable=(DatabaseMotable)immotable;
				motable.setConnection(connection);
                motable.init(_store);
			} catch (Exception e) {
                if (_log.isErrorEnabled()) _log.error("store ("+_store.getDescription()+") failed", e);
				return false;
			}

			// inserts immotable into map
			return super.prepare(name, emotable, immotable);
		}
		
		public void commit(String name, Motable immotable) {
			DatabaseMotable motable=((DatabaseMotable)immotable);
			Connection connection=motable.getConnection();
			motable.setConnection(null);
			try {
				// noop
				super.commit(name, immotable);
				connection.close();
			} catch (SQLException e) {
                if (_log.isWarnEnabled()) _log.warn("store ("+_store.getDescription()+") problem", e);
			}
		}

		public void rollback(String name, Motable immotable) {
			DatabaseMotable motable=((DatabaseMotable)immotable);
			Connection connection=motable.getConnection();
			motable.setConnection(null);
			try {
				// remove immotable from map
				// destroy immotable
				super.rollback(name, immotable);
				connection.rollback();
				connection.close();
			} catch (SQLException e) {
                if (_log.isWarnEnabled()) _log.warn("store ("+_store.getDescription()+") problem", e);
			}
		}
		
		public String getInfo() {
			return _store.getDescription();
		}
	}

	class GiannisEmoter extends AbstractMappedEmoter {

		public GiannisEmoter(Map map) {super(map);}

		public boolean prepare(String name, Motable emotable, Motable immotable) {
			try {
				DatabaseMotable motable=(DatabaseMotable)emotable;
				motable.setConnection(_store.getConnection());
				//motable.init(_store, name); // only loads header, we could load body as well...
				// copies emotable content into immotable
				return super.prepare(name, emotable, immotable);
			} catch (Exception e) {
				if (_log.isErrorEnabled()) _log.error("load ("+_store.getDescription()+") failed", e);
				return false;
			}
		}
        
		public void commit(String name, Motable emotable) {
			DatabaseMotable motable=((DatabaseMotable)emotable);
			Connection connection=motable.getConnection();
			// remove emotable from map
			// destroy emotable
			super.commit(name, emotable);
			motable.setConnection(null);
			try {
				connection.close();
			} catch (SQLException e) {
				if (_log.isWarnEnabled()) _log.warn("load ("+_store.getDescription()+") problem", e);
			}
		}

		public void rollback(String name, Motable emotable) {
			DatabaseMotable motable=((DatabaseMotable)emotable);
			Connection connection=motable.getConnection();
			motable.setConnection(null);
			try {
				// noop
				super.rollback(name, emotable);
				connection.rollback();
				connection.close();
			} catch (SQLException e) {
                if (_log.isWarnEnabled()) _log.warn("load ("+_store.getDescription()+") problem", e);
			}
		}
		
		public String getInfo() {
			return _store.getDescription();
		}
	}

    public void start() throws Exception {
//        Store.Putter putter=new Store.Putter() {
//            public void put(String name, Motable motable) {
//                _map.put(name, motable);
//            }
//        };
//        _store.load(putter, ((DistributableContextualiserConfig)_config).getAccessOnLoad());
    	
    	// TODO - create DBMotables for all entries BEFORE any are promoted from further down the stack...
    	
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
