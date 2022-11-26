<%@page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%><!DOCTYPE html>
<%@page import="com.kohlschutter.dumbo.Components"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<meta name="robots" content="noindex, nofollow" />
<meta http-equiv="X-UA-Compatible" content="IE=edge" />
<meta name="viewport" content="width=device-width, initial-scale=1" />
<title>AppServer Demo</title>
<%=com.kohlschutter.dumbo.JSPSupport.htmlHead(session)%>
<link rel="stylesheet" href="/css/consoleDemo.css" />
</head>
<body>
	<%=com.kohlschutter.dumbo.JSPSupport.htmlBodyTop(session)%>
	<div class="container" style="padding-top: 20pt">

		<div class="page-header">
			<h1>Console Demo</h1>
			<p class="lead">System.in/out no more</p>
		</div>

		<p>Check the source code!</p>
		<div class="row">
			<div class="col-md-9">
				<input type="text" class="form-control" id="commandLine" value="" />
			</div>
		</div>

		<hr />

		<div id="console">
			<div class="color-message">
				<div class="card" style="margin: 6pt 0pt 6pt 0pt">
					<div class="card-body">
						<div class="panel-body">
							<div class="message-text">This is a test message</div>
						</div>
					</div>
				</div>
			</div>

			<div class="app-console-exception" style="margin-top: 0.5em">
				<div class="card">
					<div class="card-header bg-danger text-white">Exception</div>
					<div class="card-body">
						<div class="app-javaClass">javaClass</div>
						<div class="app-exceptionMessage">Exception Message</div>
					</div>
				</div>
			</div>

		</div>
	</div>
	<script type="text/javascript" src="/js/consoleDemo.js"></script>
</body>
</html>
