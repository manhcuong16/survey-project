<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.util.*"%>
<%@page import="models.User"%>

<%
    List<User> users = (List<User>) request.getAttribute("users");
    if (users == null) {
        users = new ArrayList<>();
    }
    String error = (String) request.getAttribute("error");
    String success = (String) request.getAttribute("success");
%>

<!DOCTYPE html>
<html>
<head>
    <title>Admin Users</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css">
</head>
<body>
    <div class="container">
        <h2>Admin - User Management</h2>

        <div class="btn-row">
            <a class="btn-create btn-secondary" href="${pageContext.request.contextPath}/admin-surveys">Manage Surveys</a>
            <a class="btn-create" href="${pageContext.request.contextPath}/admin-users">Manage Users</a>
        </div>

        <br>
        <% if (success != null && !success.trim().isEmpty()) { %>
            <p style="color:green"><%= success %></p>
        <% } %>
        <% if (error != null && !error.trim().isEmpty()) { %>
            <p style="color:red"><%= error %></p>
        <% } %>

        <h3>Create User</h3>
        <form action="${pageContext.request.contextPath}/admin-user-create" method="post">
            <input type="text" name="username" placeholder="Username" required>
            <br><br>
            <input type="password" name="password" placeholder="Password" required>
            <br><br>
            <select name="role" required>
                <option value="USER">User</option>
                <option value="CREATOR">Creator</option>
                <option value="ADMIN">Admin</option>
            </select>
            <br><br>
            <button class="btn-create" type="submit">Create User</button>
        </form>

        <br>
        <h3>User List</h3>

        <table border="1" class="survey-table">
            <tr>
                <th>ID</th>
                <th>Username</th>
                <th>Role</th>
                <th>Action</th>
            </tr>
            <% for (User u : users) { %>
            <tr>
                <td><%= u.getId() %></td>
                <td><%= u.getUsername() == null ? "" : u.getUsername() %></td>
                <td><%= u.getRole() == null ? "" : u.getRole() %></td>
                <td>
                    <a class="btn-edit" href="${pageContext.request.contextPath}/admin-user-edit?id=<%= u.getId() %>">Edit</a>
                    <form class="inline-form" method="post"
                          action="${pageContext.request.contextPath}/admin-user-delete"
                          onsubmit="return confirm('Delete this user?');">
                        <input type="hidden" name="id" value="<%= u.getId() %>">
                        <button type="submit" class="btn-delete">Delete</button>
                    </form>
                </td>
            </tr>
            <% } %>
        </table>
    </div>
</body>
</html>
