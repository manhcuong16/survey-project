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
import java.util.LinkedHashMap;
import java.util.Map;
import models.QuestionDetail;
import models.SurveyDetail;
import models.User;

/**
 *
 * @author ADMIN
 */
public class CreateSurveyController extends HttpServlet {

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        s = s.trim();
        return s.isEmpty() ? null : s;
    }

    private boolean isAdmin(User user) {
        return user != null && "ADMIN".equalsIgnoreCase(user.getRole());
    }

    private boolean isCreator(User user) {
        return user != null && ("CREATOR".equalsIgnoreCase(user.getRole()) || isAdmin(user));
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
        if (!isCreator(user)) {
            response.sendRedirect(request.getContextPath() + "/survey-list");
            return;
        }

        request.getRequestDispatcher("views/createSurvey.jsp")
                .forward(request, response);
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
        if (!isCreator(user)) {
            response.sendRedirect(request.getContextPath() + "/survey-list");
            return;
        }

        String title = trimToNull(request.getParameter("title"));
        String description = trimToNull(request.getParameter("description"));

        if (title == null) {
            request.setAttribute("error", "Title is required");
            request.getRequestDispatcher("views/createSurvey.jsp").forward(request, response);
            return;
        }

        ArrayList<QuestionDetail> questions = new ArrayList<>();

        // Parse questions in order: question_1, question_2, ...
        for (int i = 1; ; i++) {
            String qContent = trimToNull(request.getParameter("question_" + i));
            if (qContent == null) {
                break;
            }

            String type = trimToNull(request.getParameter("type_" + i));
            if (type == null) {
                type = "MCQ";
            }

            ArrayList<String> options = new ArrayList<>();
            if ("MCQ".equalsIgnoreCase(type)) {
                for (int opt = 1; ; opt++) {
                    String optVal = trimToNull(request.getParameter("option_" + i + "_" + opt));
                    if (optVal == null) {
                        break;
                    }
                    options.add(optVal);
                }
            }

            questions.add(new QuestionDetail(qContent, type, options));
        }

        SurveyDAO surveyDAO = new SurveyDAO();
        int surveyId = surveyDAO.createSurveyAndReturnId(title, description, user.getId());

        if (surveyId == -1) {
            request.setAttribute("error", "Create failed. Please check database columns or connection.");
            request.getRequestDispatcher("views/createSurvey.jsp").forward(request, response);
            return;
        }

        QuestionDAO questionDAO = new QuestionDAO();
        boolean questionSaved = questionDAO.createQuestions(surveyId, questions);

        if (!questionSaved) {
            surveyDAO.deleteSurveyById(surveyId, user.getId());
            request.setAttribute("error", "Create failed. Could not save questions to database.");
            request.getRequestDispatcher("views/createSurvey.jsp").forward(request, response);
            return;
        }

        @SuppressWarnings("unchecked")
        Map<Integer, SurveyDetail> detailsById = (Map<Integer, SurveyDetail>) session.getAttribute("surveyDetailsById");

        if (detailsById == null) {
            detailsById = new LinkedHashMap<>();
            session.setAttribute("surveyDetailsById", detailsById);
        }

        detailsById.put(surveyId, new SurveyDetail(surveyId, title, questions));

        response.sendRedirect(request.getContextPath() + "/survey-list");
    }

    @Override
    public String getServletInfo() {
        return "Short description";
    }
}
