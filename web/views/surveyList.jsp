<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.util.*"%>
<%@page import="jakarta.servlet.http.HttpSession"%>
<%@page import="models.User"%>
<%@page import="models.Survey"%>
<%@page import="models.SurveyDetail"%>
<%@page import="models.QuestionDetail"%>

<%
    List<Survey> surveys = (List<Survey>) request.getAttribute("surveys");
    Map<Integer, SurveyDetail> surveyDetailsById = (Map<Integer, SurveyDetail>) request.getAttribute("surveyDetailsById");

    if (surveys == null) {
        surveys = new ArrayList<>();
    }

    HttpSession sess = request.getSession(false);
    User user = (sess == null) ? null : (User) sess.getAttribute("user");
%>

<html>
<head>
<title>Survey List</title>

<link rel="stylesheet"
href="${pageContext.request.contextPath}/static/css/style.css">

</head>

<body>

<div class="container">

<h2>Survey List</h2>

<br>

<a class="btn-create"
href="${pageContext.request.contextPath}/create">
Create Survey
</a>

<br><br>

<table border="1" class="survey-table">

<tr>
<th>ID</th>
<th>Title</th>
<th>Questions</th>
<th>Action</th>
</tr>

<%
    for (Survey s : surveys) {
        SurveyDetail detail = (surveyDetailsById == null) ? null : surveyDetailsById.get(s.getId());
%>
<tr>
<td><%= s.getId() %></td>
<td><%= s.getTitle() %></td>
<td>
<%
        if (detail != null && detail.getQuestions() != null && !detail.getQuestions().isEmpty()) {
%>
    <ul>
<%
            for (QuestionDetail q : detail.getQuestions()) {
%>
        <li><%= q.getContent() %></li>
<%
            }
%>
    </ul>
<%
        } else {
%>
    <em>No questions found</em>
<%
        }
%>
</td>
<td>
<% if (user != null) { 
       boolean isCreator = s.getCreatedBy() == user.getId();
%>
<a class="btn-do"
href="${pageContext.request.contextPath}/do-survey?id=<%= s.getId() %>">
Do Survey
</a>
<a class="btn-view"
href="${pageContext.request.contextPath}/view?id=<%= s.getId() %>">
<%= isCreator ? "View Result" : "My Result" %>
</a>
<% if (isCreator) { %>
<a class="btn-share"
href="${pageContext.request.contextPath}/share?id=<%= s.getId() %>">
Share
</a>
<form class="inline-form" method="post"
action="${pageContext.request.contextPath}/survey-delete"
onsubmit="return confirm('Delete this survey?');">
<input type="hidden" name="id" value="<%= s.getId() %>" />
<button type="submit" class="btn-delete">Delete</button>
</form>
<% } %>
<% } else { %>
<em>Login to take survey</em>
<% } %>
</td>
</tr>
<%
    }
%>

</table>

</div>

</body>
</html>
