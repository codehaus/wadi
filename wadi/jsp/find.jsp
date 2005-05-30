    <%@ page language="java" contentType="text/html" session="true" %>
      <%@ page import="java.util.Date" %>
      <%@ page import="java.util.Enumeration" %>
      <%@ page import="org.apache.commons.logging.Log" %>
      <%@ page import="org.apache.commons.logging.LogFactory" %>
      <%
      Log log=LogFactory.getLog(getClass());
      log.trace("FIND: "+session.getId());
      String colour=System.getProperty("node.name");
      %>
      <HTML>
	<BODY BGCOLOR="<%= colour %>">
	  <H2>Session Find</H2>

	  <pre>id:            <%= session.getId() %></pre>
	  <pre>created:       <%= new Date(session.getCreationTime()) %></pre>
	  <pre>last accessed: <%= new Date(session.getLastAccessedTime()) %></pre>

	  <p/>
	    <table>
	      <%
	      session.setAttribute(""+session.getLastAccessedTime(), colour);
	      int j=0;
	      for (Enumeration e=session.getAttributeNames(); e.hasMoreElements() && j<15;j++)
	      {
	      String key=(String)e.nextElement();
	      %>
	      <tr>
		<th bgcolor="<%= session.getAttribute(key) %>">
		  <%=  new Date(Long.parseLong(key)) %>
		</th>
	      </tr>
	      <%
	      }
	      %>
	    </table>
	</BODY>
      </HTML>

