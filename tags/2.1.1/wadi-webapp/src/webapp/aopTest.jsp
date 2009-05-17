<%@page language="java" contentType="text/html" session="true" %>
<%@page import="org.codehaus.wadi.webapp.Node"%>
<%@page import="org.codehaus.wadi.webapp.RequestHelper"%>

<html>
  <body>
    <form>
    <table>
      <tr>
        <th>Depth</th>
        <th>Value</th>
        <th>New Value</th>
      </tr>
<%
  Node node = RequestHelper.handle(request);

  int currentDepth = 0;
  while (null != node) {
%>
      <tr>
        <th><%= currentDepth %></th>
        <th><%= node.getFieldValue() %></th>
        <th><input type="text" name="node.<%= currentDepth %>"></th>
      </tr>
<%
    currentDepth++;
    node = node.getChild();
  }
%>
    </table>
    <input type="submit" name="Submit">
    </form>
  </body>
</html>
