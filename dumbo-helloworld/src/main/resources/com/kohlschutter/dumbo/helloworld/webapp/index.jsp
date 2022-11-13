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
	<p>Check the source code (and play with the JavaScript developer
		console, too)</p>
</body>
</html>
