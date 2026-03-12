<%@page contentType="text/html" pageEncoding="UTF-8"%>

<%
    String next = request.getParameter("next");
    if (next == null) {
        next = "";
    }
    String registerLink = "register";
    if (!next.isEmpty()) {
        registerLink = "register?next=" + next;
    }
%>

<html>

<head>

<title>Login</title>

<link rel="stylesheet" href="static/css/style.css">

</head>

<body>

<div class="container">

<h2>Login</h2>

<form action="login" method="post">

<input type="text" name="username" placeholder="Username" required>

<br><br>

<input type="password" name="password" placeholder="Password" required>

<input type="hidden" name="next" value="<%= next %>">

<br><br>

<div class="btn-row">
    <button class="btn-create" type="submit">Login</button>
    <a class="btn-create btn-secondary" href="<%= registerLink %>">Create Account</a>
</div>

</form>

<br>

<% if ("1".equals(request.getParameter("created"))) { %>
<p style="color:green">Account created. Please login.</p>
<% } %>

<p style="color:red">
${error}
</p>

</div>

</body>

</html>
