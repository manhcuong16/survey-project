package controllers;

import dal.UserDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import models.User;

public class AdminUserEditController extends HttpServlet {

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

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
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
            response.sendRedirect(request.getContextPath() + "/admin-users?error=Invalid+user+id.");
            return;
        }

        UserDAO dao = new UserDAO();
        User editUser = dao.getUserById(userId);
        if (editUser == null) {
            response.sendRedirect(request.getContextPath() + "/admin-users?error=User+not+found.");
            return;
        }

        request.setAttribute("editUser", editUser);
        request.setAttribute("error", request.getParameter("error"));
        request.getRequestDispatcher("views/adminUserEdit.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }

    @Override
    public String getServletInfo() {
        return "Short description";
    }
}
