<%@page language="java" contentType="text/html; charset=UTF-8"
  pageEncoding="UTF-8"%><!DOCTYPE html>
<%@page import="com.kohlschutter.dumbo.Extensions"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<meta name="robots" content="noindex, nofollow" />
<meta http-equiv="X-UA-Compatible" content="IE=edge" />
<meta name="viewport" content="width=device-width, initial-scale=1" />
<title>AppServer Demo</title>
<%=com.kohlschutter.dumbo.Extensions.htmlHead(session)%>
<link rel="stylesheet" href="/css/consoleOutDemo.css" />
</head>
<body>
  <%=com.kohlschutter.dumbo.Extensions.htmlBodyTop(session)%>
  <div class="container" style="padding-top: 20pt">

    <div class="page-header">
      <h1>Console Demo</h1>
      <p class="lead">System.out no more</p>
    </div>
    <div id="console">
    </div>
  </div>
  <script type="text/javascript" src="/js/consoleOutDemo.js"></script>
</body>
</html>
