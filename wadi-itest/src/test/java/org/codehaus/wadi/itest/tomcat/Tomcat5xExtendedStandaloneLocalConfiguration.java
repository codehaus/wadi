package org.codehaus.wadi.itest.tomcat;

import java.io.File;
import org.codehaus.cargo.container.deployable.WAR;
import org.codehaus.cargo.container.tomcat.Tomcat5xStandaloneLocalConfiguration;

public class Tomcat5xExtendedStandaloneLocalConfiguration extends
    Tomcat5xStandaloneLocalConfiguration {

  public Tomcat5xExtendedStandaloneLocalConfiguration(File arg0) {
    super(arg0);
  }

  /**
   * If the managerClassName property has been set then the Contex will have a
   * Manager subelement that uses the specified class name.
   * 
   * @param deployable
   *          the WAR to deploy
   * @return the "context" XML element to instert in the Tomcat
   *         <code>server.xml</code> configuration file
   */
  protected String createContextToken(WAR deployable) {
    StringBuffer contextTokenValue = new StringBuffer();
    contextTokenValue.append("<Context path=\"");
    contextTokenValue.append("/" + deployable.getContext());
    contextTokenValue.append("\" docBase=\"");
    // Tomcat requires an absolute path for the "docBase" attribute.
    contextTokenValue.append(deployable.getFile().getAbsolutePath());
    // contextTokenValue.append("\" debug=\"");
    // contextTokenValue.append(getTomcatLoggingLevel(
    // getPropertyValue(GeneralPropertySet.LOGGING)));
    String managerClassName = getPropertyValue(ExtendedTomcatPropertySet.MANAGER_CLASS_NAME);

    if (null == managerClassName) {
      contextTokenValue.append("\"/>");
    } else {
      contextTokenValue.append("\">");
      contextTokenValue.append("<Manager className=\"" + managerClassName
          + "\"/>");
      contextTokenValue.append("</Context>");
    }
    return contextTokenValue.toString();
  }
}
