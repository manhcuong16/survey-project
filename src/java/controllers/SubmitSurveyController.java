/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */

package controllers;

import dal.AnswerDAO;
import dal.QuestionDAO;
import dal.SurveyDAO;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import models.Answer;
import models.QuestionDetail;
import models.Survey;
import models.SurveyDetail;
import models.User;

/**
 *
 * @author ADMIN
 */
public class SubmitSurveyController extends HttpServlet {

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

        Integer surveyId = tryParseInt(request.getParameter("surveyId"));
        Integer questionCount = tryParseInt(request.getParameter("questionCount"));

        if (surveyId == null || questionCount == null || questionCount <= 0) {
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

        List<Answer> answers = new ArrayList<>();
        for (int i = 1; i <= questionCount; i++) {
            Integer questionId = tryParseInt(request.getParameter("questionId_" + i));
            if (questionId == null) {
                continue;
            }
            String value = request.getParameter("answer_" + i);
            Answer ans = new Answer();
            ans.setSurveyId(surveyId);
            ans.setQuestionId(questionId);
            ans.setUserId(user.getId());
            ans.setContent(value == null ? "" : value.trim());
            answers.add(ans);
        }

        AnswerDAO answerDAO = new AnswerDAO();
        int inserted = answerDAO.insertAnswers(answers);

        if (inserted <= 0) {
            QuestionDAO questionDAO = new QuestionDAO();
            ArrayList<QuestionDetail> questions = questionDAO.getQuestionsBySurvey(surveyId);
            SurveyDetail detail = new SurveyDetail(surveyId, survey == null ? "" : survey.getTitle(), questions);
            request.setAttribute("surveyDetail", detail);
            request.setAttribute("error", "Submit failed. Please try again.");
            request.getRequestDispatcher("views/doSurvey.jsp").forward(request, response);
            return;
        }

        response.sendRedirect(request.getContextPath() + "/view?id=" + surveyId);
    }

    @Override
    public String getServletInfo() {
        return "Short description";
    }
}


