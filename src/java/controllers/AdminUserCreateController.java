package controllers;

import dal.UserDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import models.User;

public class AdminUserCreateController extends HttpServlet {

    private boolean isAdmin(User user) {
        return user != null && "ADMIN".equalsIgnoreCase(user.getRole());
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return null;
        }
        String value = role.trim().toUpperCase();
        if ("ADMIN".equals(value) || "CREATOR".equals(value) || "USER".equals(value)) {
            return value;
        }
        return null;
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
        response.sendRedirect(request.getContextPath() + "/admin-users");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        User user = (session == null) ? null : (User) session.getAttribute("user");

        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login?next=/admin-users");
            return;
        }
        if (!isAdmin(user)) {
            response.sendRedirect(request.getContextPath() + "/survey-list");
            return;
        }

        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String role = normalizeRole(request.getParameter("role"));

        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty() || role == null) {
            response.sendRedirect(request.getContextPath() + "/admin-users?error=" + enc("Please fill in all fields."));
            return;
        }

        UserDAO dao = new UserDAO();
        String safeUsername = username.trim();
        if (dao.usernameExists(safeUsername)) {
            response.sendRedirect(request.getContextPath() + "/admin-users?error=" + enc("Username already exists."));
            return;
        }

        boolean created = dao.createUser(safeUsername, password, role);
        if (created) {
            response.sendRedirect(request.getContextPath() + "/admin-users?success=" + enc("User created successfully."));
        } else {
            response.sendRedirect(request.getContextPath() + "/admin-users?error=" + enc("Create user failed."));
        }
    }

    @Override
    public String getServletInfo() {
        return "Short description";
    }
}
