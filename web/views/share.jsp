<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="models.Survey"%>

<%
    Survey survey = (Survey) request.getAttribute("survey");
    String shareLink = (String) request.getAttribute("shareLink");
    if (shareLink == null) {
        int surveyId = (survey == null) ? 0 : survey.getId();
        String scheme = request.getScheme();
        String server = request.getServerName();
        int port = request.getServerPort();
        String ctx = request.getContextPath();
        String base = scheme + "://" + server + ((port == 80 || port == 443) ? "" : (":" + port)) + ctx;
        shareLink = base + "/share?id=" + surveyId;
    }
    boolean isLocalhost = shareLink.contains("localhost") || shareLink.contains("127.0.0.1");
%>

<!DOCTYPE html>
<html>
<head>
    <title>Share Survey</title>
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/static/css/style.css">
</head>
<body>
    <div class="container">
        <h2>Share Survey</h2>
        <p><strong><%= (survey == null ? "" : survey.getTitle()) %></strong></p>

        <label for="shareLink">Share link</label>
        <input type="text" id="shareLink" value="<%= shareLink %>" readonly>

        <div class="btn-row" style="margin-top:12px;">
            <button class="btn-create" type="button" onclick="copyShareLink()">Copy Link</button>
            <a class="btn-create btn-secondary" href="${pageContext.request.contextPath}/survey-list">Back to list</a>
        </div>

        <% if (isLocalhost) { %>
        <p style="color:#c0392b; margin-top:12px;">
            Link dang là localhost nên ngu?i khác s? không truy c?p du?c. Hãy m? app b?ng IP máy b?n
            (ví d?: http://192.168.x.x:9999) r?i copy link l?i, ho?c deploy lên host/public URL.
        </p>
        <% } %>

        <p id="copyStatus" style="color:green; display:none;">Copied!</p>
    </div>

    <script>
        function copyShareLink() {
            var input = document.getElementById('shareLink');
            input.select();
            input.setSelectionRange(0, 99999);
            var ok = false;
            try {
                ok = document.execCommand('copy');
            } catch (e) {
                ok = false;
            }
            if (!ok && navigator.clipboard && navigator.clipboard.writeText) {
                navigator.clipboard.writeText(input.value).then(function() {
                    showCopied();
                }).catch(function() {
                    // ignore
                });
                return;
            }
            if (ok) {
                showCopied();
            }
        }
        function showCopied() {
            var status = document.getElementById('copyStatus');
            status.style.display = 'block';
            setTimeout(function() {
                status.style.display = 'none';
            }, 2000);
        }
    </script>
</body>
</html>
