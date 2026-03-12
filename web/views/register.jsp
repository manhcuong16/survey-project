<%@page contentType="text/html" pageEncoding="UTF-8"%>

<%
    String next = request.getParameter("next");
    if (next == null) {
        next = "";
    }
    String loginLink = "login";
    if (!next.isEmpty()) {
        loginLink = "login?next=" + next;
    }
%>

<html>

<head>

<title>Create Account</title>

<link rel="stylesheet" href="static/css/style.css">

</head>

<body>

<div class="container">

<h2>Create Account</h2>

<form action="register" method="post">

<input type="text" name="username" placeholder="Username" value="${username}" required>

<br><br>

<input type="password" name="password" placeholder="Password" required>

<br><br>

<input type="password" name="confirmPassword" placeholder="Confirm Password" required>

<input type="hidden" name="next" value="<%= next %>">

<br><br>

<div class="btn-row">
    <button class="btn-create" type="submit">Create</button>
    <a class="btn-create btn-secondary" href="<%= loginLink %>">Back to Login</a>
</div>

</form>

<br>

<p style="color:red">
${error}
</p>

</div>

</body>

</html>
