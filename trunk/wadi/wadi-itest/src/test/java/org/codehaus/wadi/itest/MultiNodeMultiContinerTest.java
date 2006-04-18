package org.codehaus.wadi.itest;

import java.io.IOException;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MultiNodeMultiContinerTest extends TestCase {
  protected Log _log = LogFactory.getLog(getClass());
  private static ContainerTestDecorator decorator;
  private static String nodesProp = "red,green,blue,yellow";
  private static String nodes[] = nodesProp.split(",");
  private static String containersProp = "tomcat50,tomcat55,tomcat50,tomcat55";

  public static Test suite() {
    TestSuite suite = new TestSuite("tests");
    suite.addTestSuite(MultiNodeMultiContinerTest.class);
    try {
      System.setProperty("nodes", nodesProp);
      System.setProperty("containers", containersProp);
      decorator = new ContainerTestDecorator(suite);
    } catch (Throwable t) {
      t.printStackTrace();
    }
    return decorator;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(MultiNodeMultiContinerTest.class);
  }

  public MultiNodeMultiContinerTest(String name) {
    super(name);
  }

  protected void setUp() throws Exception {
    super.setUp();
  }

  public void testSimpleReplication() throws Exception {
    HttpClient client = new HttpClient();
    client.setState(new HttpState());
    HttpMethod methods[] = new HttpMethod[nodes.length];
    for (int i = 0; i < nodes.length; i++) {
      methods[i] = new GetMethod("http://localhost:"
          + decorator.getPortForNode(nodes[i]));
    }
    // there are no sessions so these should not return 200
    for (int i = 0; i < methods.length; i++) {
      assertNotSame(new Integer(200), new Integer(get(client, methods[i],
          "/wadi-test;jsessionid=foo")));
      methods[i] = null;
    }
    // create a session on each node
    for (int i = 0; i < nodes.length; i++) {
      methods[i] = new GetMethod("http://localhost:"
          + decorator.getPortForNode(nodes[i]));
    }
    for (int i = 0; i < methods.length; i++) {
      assertEquals("Should find the web app "
          + methods[i].getURI().getEscapedURI() + "/wadi-test",
          new Integer(200), new Integer(get(client, methods[i], "/wadi-test")));
      methods[i] = null;
    }
    // have the methods[0] set a value the rest check the value
    for (int i = 0; i < nodes.length; i++) {
      methods[i] = new GetMethod("http://localhost:"
          + decorator.getPortForNode(nodes[i]));
    }
    assertEquals(new Integer(200), new Integer(get(client, methods[0],
        "/wadi-test/set.jsp")));
    for (int i = 1; i < methods.length; i++) {
      assertEquals(new Integer(200), new Integer(get(client, methods[i],
          "/wadi-test/check.jsp")));
      assertNotSame(new Integer(-1), new Integer(methods[i]
          .getResponseBodyAsString().indexOf("foo = bar")));
      methods[i] = null;
    }
  }

  public int get(HttpClient client, HttpMethod method, String path)
      throws IOException, HttpException {
    method.setPath(path);
    _log.debug("getting " + method.getURI().getEscapedURI());
    client.executeMethod(method);
    return method.getStatusCode();
  }

  protected void tearDown() throws Exception {
    super.tearDown();
  }
}
