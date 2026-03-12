<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.util.*"%>
<%@page import="models.Survey"%>
<%@page import="models.QuestionDetail"%>
<%@page import="models.Answer"%>

<%
    Survey survey = (Survey) request.getAttribute("survey");
    List<QuestionDetail> questions = (List<QuestionDetail>) request.getAttribute("questions");
    Map<Integer, List<Answer>> answersByQuestionId = (Map<Integer, List<Answer>>) request.getAttribute("answersByQuestionId");
    String error = (String) request.getAttribute("error");
    Boolean isCreatorAttr = (Boolean) request.getAttribute("isCreator");
    boolean isCreator = isCreatorAttr != null && isCreatorAttr.booleanValue();

    if (questions == null) {
        questions = new ArrayList<>();
    }
%>

<!DOCTYPE html>
<html>
<head>
    <title>View Result</title>
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/static/css/style.css">
</head>
<body>
    <div class="container">
        <h2><%= isCreator ? "All Results" : "My Result" %></h2>

        <%
            if (error != null) {
        %>
            <p style="color:red;"><%= error %></p>
        <%
            } else if (survey != null) {
        %>
            <h3><%= survey.getTitle() %></h3>
        <%
            }
        %>

        <%
            if (questions.isEmpty()) {
        %>
            <p><em>No questions found for this survey.</em></p>
        <%
            } else {
                int idx = 1;
                for (QuestionDetail q : questions) {
                    List<Answer> answers = (answersByQuestionId == null) ? null : answersByQuestionId.get(q.getId());
                    int total = (answers == null) ? 0 : answers.size();
                    String type = (q.getType() == null) ? "MCQ" : q.getType();
        %>
            <div class="question-box">
                <h4>Question <%= idx %>: <%= q.getContent() %></h4>
                <p><em>Responses: <%= total %></em></p>

                <%
                    if (!isCreator) {
                        if (answers == null || answers.isEmpty()) {
                %>
                    <em>No response from you yet.</em>
                <%
                        } else {
                %>
                    <ul>
                <%
                            for (Answer a : answers) {
                %>
                        <li><%= a.getContent() %></li>
                <%
                            }
                %>
                    </ul>
                <%
                        }
                    } else if ("TEXT".equalsIgnoreCase(type)) {
                        if (answers == null || answers.isEmpty()) {
                %>
                    <em>No responses yet.</em>
                <%
                        } else {
                %>
                    <ul>
                <%
                            for (Answer a : answers) {
                %>
                        <li><%= a.getContent() %></li>
                <%
                            }
                %>
                    </ul>
                <%
                        }
                    } else {
                        LinkedHashMap<String, Integer> counts = new LinkedHashMap<>();
                        List<String> opts = q.getOptions();
                        if (opts != null) {
                            for (String opt : opts) {
                                if (opt != null && !opt.trim().isEmpty()) {
                                    counts.put(opt, 0);
                                }
                            }
                        }
                        if (answers != null) {
                            for (Answer a : answers) {
                                String val = (a == null || a.getContent() == null) ? "" : a.getContent().trim();
                                if (!counts.containsKey(val)) {
                                    counts.put(val, 0);
                                }
                                counts.put(val, counts.get(val) + 1);
                            }
                        }
                        if (counts.isEmpty()) {
                %>
                    <em>No responses yet.</em>
                <%
                        } else {
                %>
                    <table border="1" class="survey-table">
                        <tr>
                            <th>Option</th>
                            <th>Count</th>
                        </tr>
                        <%
                            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                        %>
                        <tr>
                            <td><%= entry.getKey() %></td>
                            <td><%= entry.getValue() %></td>
                        </tr>
                        <%
                            }
                        %>
                    </table>
                <%
                        }
                    }
                %>
            </div>
        <%
                    idx++;
                }
            }
        %>

        <br>
        <a class="btn-create" href="${pageContext.request.contextPath}/survey-list">Back to list</a>
    </div>
</body>
</html>
