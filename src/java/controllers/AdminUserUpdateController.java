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

public class AdminUserUpdateController extends HttpServlet {

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
        User currentUser = (session == null) ? null : (User) session.getAttribute("user");

        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/login?next=/admin-users");
            return;
        }
        if (!isAdmin(currentUser)) {
            response.sendRedirect(request.getContextPath() + "/survey-list");
            return;
        }

        Integer userId = tryParseInt(request.getParameter("id"));
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String role = normalizeRole(request.getParameter("role"));

        if (userId == null || userId <= 0 || username == null || username.trim().isEmpty() || role == null) {
            response.sendRedirect(request.getContextPath() + "/admin-users?error=" + enc("Invalid user data."));
            return;
        }

        UserDAO dao = new UserDAO();
        User target = dao.getUserById(userId);
        if (target == null) {
            response.sendRedirect(request.getContextPath() + "/admin-users?error=" + enc("User not found."));
            return;
        }

        if (dao.usernameExistsForOtherUser(userId, username.trim())) {
            response.sendRedirect(request.getContextPath() + "/admin-users?error=" + enc("Username already exists."));
            return;
        }

        boolean targetIsAdmin = target.getRole() != null && "ADMIN".equalsIgnoreCase(target.getRole());
        if (targetIsAdmin && !"ADMIN".equalsIgnoreCase(role)) {
            int adminCount = dao.countAdmins();
            if (adminCount <= 1) {
                response.sendRedirect(request.getContextPath() + "/admin-users?error=" + enc("Cannot remove the last admin."));
                return;
            }
        }

        String newPassword = (password == null || password.isEmpty()) ? null : password;
        boolean updated = dao.updateUser(userId, username.trim(), newPassword, role);

        if (updated) {
            response.sendRedirect(request.getContextPath() + "/admin-users?success=" + enc("User updated successfully."));
        } else {
            response.sendRedirect(request.getContextPath() + "/admin-users?error=" + enc("Update user failed."));
        }
    }

    @Override
    public String getServletInfo() {
        return "Short description";
    }
}
