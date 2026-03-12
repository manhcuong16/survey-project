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

public class AdminUserDeleteController extends HttpServlet {

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
        if (userId == null || userId <= 0) {
            response.sendRedirect(request.getContextPath() + "/admin-users?error=" + enc("Invalid user id."));
            return;
        }

        if (currentUser.getId() == userId) {
            response.sendRedirect(request.getContextPath() + "/admin-users?error=" + enc("Cannot delete current account."));
            return;
        }

        UserDAO dao = new UserDAO();
        User target = dao.getUserById(userId);
        if (target == null) {
            response.sendRedirect(request.getContextPath() + "/admin-users?error=" + enc("User not found."));
            return;
        }

        boolean targetIsAdmin = target.getRole() != null && "ADMIN".equalsIgnoreCase(target.getRole());
        if (targetIsAdmin) {
            int adminCount = dao.countAdmins();
            if (adminCount <= 1) {
                response.sendRedirect(request.getContextPath() + "/admin-users?error=" + enc("Cannot delete the last admin."));
                return;
            }
        }

        boolean deleted = dao.deleteUser(userId);
        if (deleted) {
            response.sendRedirect(request.getContextPath() + "/admin-users?success=" + enc("User deleted successfully."));
        } else {
            response.sendRedirect(request.getContextPath() + "/admin-users?error=" + enc("Delete user failed."));
        }
    }

    @Override
    public String getServletInfo() {
        return "Short description";
    }
}
