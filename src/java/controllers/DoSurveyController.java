/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */

package controllers;

import dal.QuestionDAO;
import dal.SurveyDAO;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import models.QuestionDetail;
import models.Survey;
import models.SurveyDetail;
import models.User;

/**
 *
 * @author ADMIN
 */
public class DoSurveyController extends HttpServlet {

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

        SurveyDAO surveyDAO = new SurveyDAO();
        Survey survey = surveyDAO.getSurveyById(surveyId);

        if (survey != null && survey.isLocked()) {
            request.setAttribute("error", "Survey is locked.");
            request.getRequestDispatcher("views/doSurvey.jsp").forward(request, response);
            return;
        }

        QuestionDAO questionDAO = new QuestionDAO();
        ArrayList<QuestionDetail> questions = questionDAO.getQuestionsBySurvey(surveyId);

        if (survey == null || questions == null || questions.isEmpty()) {
            request.setAttribute("error", "Survey not found or has no questions.");
            request.getRequestDispatcher("views/doSurvey.jsp").forward(request, response);
            return;
        }

        SurveyDetail detail = new SurveyDetail(surveyId, survey.getTitle(), questions);
        request.setAttribute("surveyDetail", detail);
        request.getRequestDispatcher("views/doSurvey.jsp").forward(request, response);
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

