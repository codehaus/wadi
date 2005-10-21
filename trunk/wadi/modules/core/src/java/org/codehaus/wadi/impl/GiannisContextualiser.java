package org.codehaus.wadi.impl;

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
        return "["+_store.getLabel()+"/"+_store.getTable()+"] : "+_map.size()+" sessions loaded";
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
			StoreMotable motable=_store.create();
			motable.init(_store);
            return motable; // TODO - Pool, maybe as ThreadLocal
		}

		public String getInfo() {
			return _store.getDescription();
		}
	}

	class GiannisEmoter extends AbstractMappedEmoter {

		public GiannisEmoter(Map map) {super(map);}

		public String getInfo() {
			return _store.getDescription();
		}
	}

    public void start() throws Exception {
    	// TODO - consider moving into load...
    	Store.Putter putter=new Store.Putter() {
    		public void put(String name, Motable motable) {
    			_map.put(name, motable);
    		}
        };	
        _store.load(putter, ((DistributableContextualiserConfig)_config).getAccessOnLoad());
    	
        super.start(); // continue down chain...
    }

    // this should move up.....
    public void expire(Motable motable) {
        // decide whether session needs promotion
        boolean needsLoading=true; // FIXME
        // if so promote to top and expire there
        String id=motable.getName();
        if (_log.isTraceEnabled()) _log.trace("expiring from store: "+id);
        if (needsLoading) {
            _map.remove(id);
            Motable loaded=_config.getSessionPool().take();
            Connection connection=null;
            DatabaseMotable dbm=(DatabaseMotable)motable;
            try {
            	connection=_store.getDataSource().getConnection();
            	dbm.setConnection(connection);
                loaded.copy(dbm);
                dbm.setConnection(null);
                _config.expire(loaded);
            } catch (Exception e) {
                _log.error("unexpected problem expiring from store", e);
            } finally {
            	if (connection!=null) {
            		try {
            		connection.close();
            		} catch (SQLException e) {
            			_log.warn("unexpected problem closing connection", e);
            		}
            	}
            }
            loaded=null;
        } else {
            // else, just drop it off the disc here...
            throw new UnsupportedOperationException(); // FIXME
        }
    }

    public void load(Emoter emoter, Immoter immoter) { // - TODO - is this where we should do our own load ?
        // MappedContextualisers are all Exclusive
    }

    protected void unload() {
    	_log.info("unloaded sessions: "+_map.size());
    	_map.clear();
	}
    
    public Immoter getSharedDemoter() {
    	return getImmoter();
    }

}
