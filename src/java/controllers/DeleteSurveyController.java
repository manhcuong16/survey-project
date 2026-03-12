/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package controllers;

import dal.SurveyDAO;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import models.SurveyDetail;
import models.User;

/**
 *
 * @author ADMIN
 */
public class DeleteSurveyController extends HttpServlet {

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

    private boolean isAdmin(User user) {
        return user != null && "ADMIN".equalsIgnoreCase(user.getRole());
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect(request.getContextPath() + "/survey-list");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        User user = (session == null) ? null : (User) session.getAttribute("user");

        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        Integer surveyId = tryParseInt(request.getParameter("id"));
        if (surveyId == null) {
            response.sendRedirect(request.getContextPath() + "/survey-list");
            return;
        }

        SurveyDAO dao = new SurveyDAO();
        if (isAdmin(user)) {
            dao.deleteSurveyById(surveyId, null);
        } else {
            dao.deleteSurveyById(surveyId, user.getId());
        }

        if (session != null) {
            @SuppressWarnings("unchecked")
            Map<Integer, SurveyDetail> detailsById =
                    (Map<Integer, SurveyDetail>) session.getAttribute("surveyDetailsById");
            if (detailsById != null) {
                detailsById.remove(surveyId);
            }
        }

        response.sendRedirect(request.getContextPath() + "/survey-list");
    }

    @Override
    public String getServletInfo() {
        return "Short description";
    }
}
