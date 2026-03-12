package controllers;

import dal.UserDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class RegisterController extends HttpServlet {

    private String normalizeNext(String next) {
        if (next == null) {
            return null;
        }
        String value = next.trim();
        if (value.isEmpty()) {
            return null;
        }
        if (!value.startsWith("/")) {
            return null;
        }
        return value;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("views/register.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");
        String next = normalizeNext(request.getParameter("next"));

        String error = null;
        if (username == null || username.trim().isEmpty()
                || password == null || password.isEmpty()
                || confirmPassword == null || confirmPassword.isEmpty()) {
            error = "Please fill in all fields.";
        } else if (!password.equals(confirmPassword)) {
            error = "Password confirmation does not match.";
        } else {
            UserDAO dao = new UserDAO();
            String safeUsername = username.trim();
            if (dao.usernameExists(safeUsername)) {
                error = "Username already exists.";
            } else {
                boolean created = dao.createUser(safeUsername, password);
                if (created) {
                    String redirect = request.getContextPath() + "/login?created=1";
                    if (next != null) {
                        redirect += "&next=" + next;
                    }
                    response.sendRedirect(redirect);
                    return;
                } else {
                    error = "Create account failed. Please try again.";
                }
            }
        }

        request.setAttribute("username", username);
        request.setAttribute("error", error);
        request.getRequestDispatcher("views/register.jsp").forward(request, response);
    }
}
