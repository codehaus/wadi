    <%@ page language="java" contentType="text/html" session="true" %>
      <%@ page import="java.util.Date" %>
      <%@ page import="java.util.Enumeration" %>
      <HTML>
	<BODY>
	  <H2>Session Creation</H2>

	  <pre>id:            <%= session.getId() %></pre>
	  <pre>created:       <%= new Date(session.getCreationTime()) %></pre>
	  <pre>last accessed: <%= new Date(session.getLastAccessedTime()) %></pre>

	  <p/>
	    <table>
	      <%
	      session.setAttribute(""+session.getLastAccessedTime(), new Date(session.getLastAccessedTime()));
	      for (Enumeration e=session.getAttributeNames(); e.hasMoreElements();)
	      {
	      String key=(String)e.nextElement();
	      %>
	      <tr>
		<th>
		  <%= key %>
		</th>
		<td>
		  <%= session.getAttribute(key) %>
		</td>
	      </tr>
	      <%
	      }
	      %>
	    </table>
	</BODY>
      </HTML>

