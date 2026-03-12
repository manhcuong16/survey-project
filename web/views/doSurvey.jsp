<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.util.*"%>
<%@page import="models.SurveyDetail"%>
<%@page import="models.QuestionDetail"%>

<%
    SurveyDetail detail = (SurveyDetail) request.getAttribute("surveyDetail");
    String error = (String) request.getAttribute("error");
%>

<!DOCTYPE html>
<html>
<head>
    <title>Do Survey</title>
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/static/css/style.css">
</head>
<body>
    <div class="form-container">
        <h2>Do Survey</h2>

        <%
            if (error != null) {
        %>
            <p style="color:red;"><%= error %></p>
            <a class="btn-create" href="${pageContext.request.contextPath}/survey-list">
                Back to list
            </a>
        <%
            } else if (detail != null && detail.getQuestions() != null && !detail.getQuestions().isEmpty()) {
        %>
            <h3><%= detail.getTitle() %></h3>

            <form action="${pageContext.request.contextPath}/submit" method="post">
                <input type="hidden" name="surveyId" value="<%= detail.getSurveyId() %>">
                <input type="hidden" name="questionCount" value="<%= detail.getQuestions().size() %>">

                <%
                    int idx = 1;
                    for (QuestionDetail q : detail.getQuestions()) {
                        String type = (q.getType() == null) ? "MCQ" : q.getType();
                %>
                <div class="question-box">
                    <h4>Question <%= idx %>: <%= q.getContent() %></h4>
                    <input type="hidden" name="questionId_<%= idx %>" value="<%= q.getId() %>">

                    <%
                        if ("TEXT".equalsIgnoreCase(type)) {
                    %>
                        <input type="text" name="answer_<%= idx %>" required>
                    <%
                        } else {
                            List<String> options = q.getOptions();
                            boolean rendered = false;
                            if (options != null) {
                                int optIndex = 0;
                                for (String opt : options) {
                                    if (opt == null || opt.trim().isEmpty()) {
                                        continue;
                                    }
                                    boolean required = (optIndex == 0);
                                    optIndex++;
                                    rendered = true;
                    %>
                        <label>
                            <input type="radio" name="answer_<%= idx %>" value="<%= opt %>" <%= required ? "required" : "" %>>
                            <%= opt %>
                        </label>
                    <%
                                }
                            }
                            if (!rendered) {
                    %>
                        <input type="text" name="answer_<%= idx %>" required>
                    <%
                            }
                        }
                    %>
                </div>
                <%
                        idx++;
                    }
                %>

                <button class="submit-btn" type="submit">Submit</button>
            </form>
        <%
            } else {
        %>
            <p><em>No questions found for this survey.</em></p>
            <a class="btn-create" href="${pageContext.request.contextPath}/survey-list">
                Back to list
            </a>
        <%
            }
        %>
    </div>
</body>
</html>

