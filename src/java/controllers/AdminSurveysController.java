package controllers;

import dal.SurveyDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import models.Survey;
import models.User;

public class AdminSurveysController extends HttpServlet {

    private boolean isAdmin(User user) {
        return user != null && "ADMIN".equalsIgnoreCase(user.getRole());
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
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

        SurveyDAO dao = new SurveyDAO();
        ArrayList<Survey> surveys = dao.getAllSurveys();
        request.setAttribute("surveys", surveys);
        request.setAttribute("error", request.getParameter("error"));
        request.setAttribute("success", request.getParameter("success"));
        request.getRequestDispatcher("views/adminSurveys.jsp").forward(request, response);
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
