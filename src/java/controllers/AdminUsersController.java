package controllers;

import dal.UserDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import models.User;

public class AdminUsersController extends HttpServlet {

    private boolean isAdmin(User user) {
        return user != null && "ADMIN".equalsIgnoreCase(user.getRole());
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
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

        UserDAO dao = new UserDAO();
        ArrayList<User> users = dao.getAllUsers();
        request.setAttribute("users", users);
        request.setAttribute("error", request.getParameter("error"));
        request.setAttribute("success", request.getParameter("success"));
        request.getRequestDispatcher("views/adminUsers.jsp").forward(request, response);
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
