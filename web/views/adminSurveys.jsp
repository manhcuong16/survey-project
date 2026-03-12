<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.util.*"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="models.Survey"%>

<%
    List<Survey> surveys = (List<Survey>) request.getAttribute("surveys");
    if (surveys == null) {
        surveys = new ArrayList<>();
    }
    String error = (String) request.getAttribute("error");
    String success = (String) request.getAttribute("success");
    SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm");
%>

<!DOCTYPE html>
<html>
<head>
    <title>Admin Surveys</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css">
</head>
<body>
    <div class="container">
        <h2>Admin - Survey Management</h2>

        <div class="btn-row">
            <a class="btn-create" href="${pageContext.request.contextPath}/admin-surveys">Manage Surveys</a>
            <a class="btn-create btn-secondary" href="${pageContext.request.contextPath}/admin-users">Manage Users</a>
            <a class="btn-create btn-secondary" href="${pageContext.request.contextPath}/create">Create Survey</a>
        </div>

        <br>
        <% if (success != null && !success.trim().isEmpty()) { %>
            <p style="color:green"><%= success %></p>
        <% } %>
        <% if (error != null && !error.trim().isEmpty()) { %>
            <p style="color:red"><%= error %></p>
        <% } %>

        <table border="1" class="survey-table">
            <tr>
                <th>ID</th>
                <th>Title</th>
                <th>Created By</th>
                <th>Created Date</th>
                <th>Status</th>
                <th>Action</th>
            </tr>
            <% for (Survey s : surveys) { %>
            <tr>
                <td><%= s.getId() %></td>
                <td><%= s.getTitle() == null ? "" : s.getTitle() %></td>
                <td><%= s.getCreatedBy() %></td>
                <td><%= s.getCreatedDate() == null ? "-" : fmt.format(s.getCreatedDate()) %></td>
                <td><%= s.isLocked() ? "Locked" : "Open" %></td>
                <td>
                    <a class="btn-view" href="${pageContext.request.contextPath}/view?id=<%= s.getId() %>">View Results</a>
                    <form class="inline-form" method="post"
                          action="${pageContext.request.contextPath}/survey-lock">
                        <input type="hidden" name="id" value="<%= s.getId() %>">
                        <input type="hidden" name="locked" value="<%= s.isLocked() ? "false" : "true" %>">
                        <button type="submit" class="<%= s.isLocked() ? "btn-unlock" : "btn-lock" %>">
                            <%= s.isLocked() ? "Unlock" : "Lock" %>
                        </button>
                    </form>
                    <form class="inline-form" method="post"
                          action="${pageContext.request.contextPath}/survey-delete"
                          onsubmit="return confirm('Delete this survey?');">
                        <input type="hidden" name="id" value="<%= s.getId() %>">
                        <button type="submit" class="btn-delete">Delete</button>
                    </form>
                </td>
            </tr>
            <% } %>
        </table>
    </div>
</body>
</html>
