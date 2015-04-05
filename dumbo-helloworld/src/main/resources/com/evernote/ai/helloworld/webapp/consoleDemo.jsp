<%@page language="java" contentType="text/html; charset=UTF-8"
  pageEncoding="UTF-8"%><!DOCTYPE html>
<%@page import="com.evernote.ai.dumbo.Extensions"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<meta name="robots" content="noindex, nofollow" />
<meta http-equiv="X-UA-Compatible" content="IE=edge" />
<meta name="viewport" content="width=device-width, initial-scale=1" />
<title>AppServer Demo</title>
<%=com.evernote.ai.dumbo.Extensions.htmlHead(session)%>
<link rel="stylesheet" href="/css/consoleDemo.css" />
</head>
<body>
  <%=com.evernote.ai.dumbo.Extensions.htmlBodyTop(session)%>
  <div class="container">

    <div class="page-header">
      <h1>Console Demo</h1>
      <p class="lead">System.in/out no more</p>
    </div>

    <p>
      <a id="link-to-source">Check the source code!</a>
    </p>
    <div class="row">
      <div class="col-md-9">
        <input type="text" class="form-control" id="commandLine"
          value="" />
      </div>
    </div>

    <hr />

    <div id="console">
      <div class="color-message">
        <div class="panel panel-default"
          style="margin-top: 0.5em; margin-bottom: 0.5em">
          <div class="panel-body">
            <div class="message-text">This is a test message</div>
          </div>
        </div>
      </div>

      <div class="app-console-exception" style="margin-top: 0.5em">
        <div class="panel panel-danger">
          <div class="panel-heading">Exception</div>
          <div class="panel-body">
            <div class="app-javaClass"></div>
            <div class="app-exceptionMessage"></div>
          </div>
        </div>
      </div>

    </div>
  </div>
  <script type="text/javascript" src="/js/consoleDemo.js"></script>
</body>
</html>
