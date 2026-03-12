<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="models.User"%>

<%
    User editUser = (User) request.getAttribute("editUser");
    String error = (String) request.getAttribute("error");
%>

<!DOCTYPE html>
<html>
<head>
    <title>Edit User</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css">
</head>
<body>
    <div class="container">
        <h2>Edit User</h2>

        <% if (error != null && !error.trim().isEmpty()) { %>
            <p style="color:red"><%= error %></p>
        <% } %>

        <% if (editUser == null) { %>
            <p><em>User not found.</em></p>
            <a class="btn-create" href="${pageContext.request.contextPath}/admin-users">Back</a>
        <% } else { %>
            <form action="${pageContext.request.contextPath}/admin-user-update" method="post">
                <input type="hidden" name="id" value="<%= editUser.getId() %>">

                <label>Username</label>
                <input type="text" name="username" value="<%= editUser.getUsername() == null ? "" : editUser.getUsername() %>" required>
                <br><br>

                <label>Password (leave blank to keep)</label>
                <input type="password" name="password" placeholder="Leave blank to keep">
                <br><br>

                <label>Role</label>
                <select name="role" required>
                    <option value="USER" <%= "USER".equalsIgnoreCase(editUser.getRole()) ? "selected" : "" %>>User</option>
                    <option value="CREATOR" <%= "CREATOR".equalsIgnoreCase(editUser.getRole()) ? "selected" : "" %>>Creator</option>
                    <option value="ADMIN" <%= "ADMIN".equalsIgnoreCase(editUser.getRole()) ? "selected" : "" %>>Admin</option>
                </select>
                <br><br>

                <div class="btn-row">
                    <button class="btn-edit" type="submit">Update</button>
                    <a class="btn-create btn-secondary" href="${pageContext.request.contextPath}/admin-users">Back</a>
                </div>
            </form>
        <% } %>
    </div>
</body>
</html>
