<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.util.*"%>
<%@page import="models.SurveyDetail"%>
<%@page import="models.QuestionDetail"%>

<%
    SurveyDetail detail = (SurveyDetail) request.getAttribute("surveyDetail");
    List<String> answers = (List<String>) request.getAttribute("answers");
    if (answers == null) {
        answers = new ArrayList<>();
    }
%>

<!DOCTYPE html>
<html>
<head>
    <title>Survey Submitted</title>
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/static/css/style.css">
</head>
<body>
    <div class="container">
        <h2>Survey Submitted</h2>
        <p>Thanks for your response.</p>

        <%
            if (detail != null) {
        %>
            <h3><%= detail.getTitle() %></h3>
            <table border="1" class="survey-table">
                <tr>
                    <th>Question</th>
                    <th>Answer</th>
                </tr>
                <%
                    List<QuestionDetail> questions = detail.getQuestions();
                    if (questions != null) {
                        int idx = 0;
                        for (QuestionDetail q : questions) {
                            String ans = (idx < answers.size()) ? answers.get(idx) : "";
                %>
                <tr>
                    <td><%= q.getContent() %></td>
                    <td><%= ans %></td>
                </tr>
                <%
                            idx++;
                        }
                    }
                %>
            </table>
        <%
            } else {
        %>
            <table border="1" class="survey-table">
                <tr>
                    <th>Question</th>
                    <th>Answer</th>
                </tr>
                <%
                    for (int i = 0; i < answers.size(); i++) {
                %>
                <tr>
                    <td>Question <%= (i + 1) %></td>
                    <td><%= answers.get(i) %></td>
                </tr>
                <%
                    }
                %>
            </table>
        <%
            }
        %>

        <br>
        <a class="btn-create" href="${pageContext.request.contextPath}/survey-list">Back to list</a>
    </div>
</body>
</html>


