

import java.net.URI;

import org.codehaus.wadi.core.manager.ClusteredManager;
import org.codehaus.wadi.core.manager.Router;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.impl.StackContext;
import org.codehaus.wadi.servicespace.ServiceSpace;
import org.codehaus.wadi.servicespace.ServiceSpaceName;
import org.codehaus.wadi.web.impl.DummyRouter;

public class SampleStack {
    private ClusteredManager manager;
    private ServiceSpace serviceSpace;
    
    public void create(Dispatcher dispatcher) throws Exception {
        StackContext stackContext = new StackContext(new ServiceSpaceName(new URI("name")), dispatcher) {
            protected Router newRouter() {
                return new DummyRouter();
            }
        };
        stackContext.build();

        serviceSpace = stackContext.getServiceSpace();
        manager = stackContext.getManager();
    }

    public ClusteredManager getManager() {
        return manager;
    }

    public void start() throws Exception {
        serviceSpace.start();
    }

    public void stop() throws Exception {
        serviceSpace.stop();
    }

}
