package controllers;

import dal.SurveyDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import models.User;

public class SurveyLockController extends HttpServlet {

    private boolean isAdmin(User user) {
        return user != null && "ADMIN".equalsIgnoreCase(user.getRole());
    }

    private Integer tryParseInt(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String enc(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect(request.getContextPath() + "/admin-surveys");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        User user = (session == null) ? null : (User) session.getAttribute("user");

        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login?next=/admin-surveys");
            return;
        }
        if (!isAdmin(user)) {
            response.sendRedirect(request.getContextPath() + "/survey-list");
            return;
        }

        Integer surveyId = tryParseInt(request.getParameter("id"));
        String lockedParam = request.getParameter("locked");
        boolean locked = "true".equalsIgnoreCase(lockedParam) || "1".equals(lockedParam);

        if (surveyId == null) {
            response.sendRedirect(request.getContextPath() + "/admin-surveys?error=" + enc("Invalid survey id."));
            return;
        }

        SurveyDAO dao = new SurveyDAO();
        boolean updated = dao.setSurveyLocked(surveyId, locked);
        if (updated) {
            String msg = locked ? "Survey locked." : "Survey unlocked.";
            response.sendRedirect(request.getContextPath() + "/admin-surveys?success=" + enc(msg));
        } else {
            response.sendRedirect(request.getContextPath() + "/admin-surveys?error=" + enc("Update lock failed."));
        }
    }

    @Override
    public String getServletInfo() {
        return "Short description";
    }
}
