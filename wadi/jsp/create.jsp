    <%@ page language="java" contentType="text/html" session="true" %>
      <%@ page import="java.util.Date" %>
      <%@ page import="java.util.Enumeration" %>
      <%
      String colour=System.getProperty("wadi.colour");
      %>
      <HTML>
	<BODY BGCOLOR="<%= colour %>">
	  <H2>Session Creation</H2>

	  <pre>id:            <%= session.getId() %></pre>
	  <pre>created:       <%= new Date(session.getCreationTime()) %></pre>
	  <pre>last accessed: <%= new Date(session.getLastAccessedTime()) %></pre>

	  <p/>
	    <table>
	      <%
	      session.setAttribute(""+session.getLastAccessedTime(), colour);
	      for (Enumeration e=session.getAttributeNames(); e.hasMoreElements();)
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

