package org.codehaus.wadi.itest.tomcat;

import org.codehaus.cargo.container.tomcat.TomcatPropertySet;

public interface ExtendedTomcatPropertySet extends TomcatPropertySet {
  /**
   * The manager class name to put into the web app context.
   */
  String MANAGER_CLASS_NAME = "cargo.tomcat.manager.classname";
}
