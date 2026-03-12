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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import models.Answer;
import models.QuestionDetail;
import models.Survey;
import models.User;

/**
 *
 * @author ADMIN
 */
public class ViewResultController extends HttpServlet {

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

        QuestionDAO questionDAO = new QuestionDAO();
        ArrayList<QuestionDetail> questions = questionDAO.getQuestionsBySurvey(surveyId);

        if (survey == null) {
            request.setAttribute("error", "Survey not found.");
            request.getRequestDispatcher("views/viewResult.jsp").forward(request, response);
            return;
        }

        boolean isAdmin = user != null && "ADMIN".equalsIgnoreCase(user.getRole());
        boolean isCreator = isAdmin || (survey.getCreatedBy() > 0 && survey.getCreatedBy() == user.getId());

        AnswerDAO answerDAO = new AnswerDAO();
        List<Integer> questionIds = new ArrayList<>();
        if (questions != null) {
            for (QuestionDetail q : questions) {
                if (q != null) {
                    questionIds.add(q.getId());
                }
            }
        }
        ArrayList<Answer> answers = isCreator
                ? answerDAO.getAnswersBySurvey(surveyId, questionIds)
                : answerDAO.getAnswersBySurveyForUser(surveyId, user.getId(), questionIds);

        Map<Integer, List<Answer>> answersByQuestionId = new LinkedHashMap<>();
        if (questions != null) {
            for (QuestionDetail q : questions) {
                if (q != null) {
                    answersByQuestionId.put(q.getId(), new ArrayList<>());
                }
            }
        }
        if (answers != null) {
            for (Answer a : answers) {
                if (a == null) {
                    continue;
                }
                List<Answer> list = answersByQuestionId.get(a.getQuestionId());
                if (list == null) {
                    list = new ArrayList<>();
                    answersByQuestionId.put(a.getQuestionId(), list);
                }
                list.add(a);
            }
        }

        request.setAttribute("survey", survey);
        request.setAttribute("questions", questions);
        request.setAttribute("answersByQuestionId", answersByQuestionId);
        request.setAttribute("isCreator", isCreator);
        request.getRequestDispatcher("views/viewResult.jsp").forward(request, response);
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


