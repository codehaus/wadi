    <%@ page language="java" contentType="text/html" session="false" %>
    <%@ page import="java.util.Date" %>
    <%@ page import="java.util.Enumeration" %>
      <HTML>
	<BODY>
	  <H2>Session Find</H2>

	  <pre>id:            <%= request.getSession(false).getId() %></pre>
	  <pre>created:       <%= new Date(request.getSession(false).getCreationTime()) %></pre>
	  <pre>last accessed: <%= new Date(request.getSession(false).getLastAccessedTime()) %></pre>

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
	      System.out.println("find.jsp - rendered");
	      %>
	    </table>
	</BODY>
      </HTML>

