<%@page language="java" contentType="text/html; charset=UTF-8"
  pageEncoding="UTF-8"%><!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<meta name="robots" content="noindex, nofollow" />
<title>AppServer Demo</title>
<%=com.kohlschutter.dumbo.Extensions.htmlHead(session)%>
</head>
<body>
  <%=com.kohlschutter.dumbo.Extensions.htmlBodyTop(session)%>
  Hello
  <span id="rpcResponse"></span>
  <p>
    <a id="link-to-source">Check the source code</a> (and play with the
    JavaScript developer console, too)
  </p>
</body>
</html>
