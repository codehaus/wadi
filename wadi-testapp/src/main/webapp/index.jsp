<%@ page language="java" contentType="text/html" session="true" %>
<%
  String color=System.getProperty("node.name");
%>
<html>
  <head>
    <title>WADI Test Page</title>
  </head>
  <body bgcolor="<%= color %>">
    <p>The index for the wadi test app.</p>
    <p>The value of foo = <%= session.getAttribute("foo") %></p>
  </body>
</html>