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
import models.Survey;
import models.SurveyDetail;
import models.User;

/**
 *
 * @author ADMIN
 */
public class SurveyListController extends HttpServlet {

    private boolean isAdmin(User user) {
        return user != null && "ADMIN".equalsIgnoreCase(user.getRole());
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

        if (isAdmin(user)) {
            response.sendRedirect(request.getContextPath() + "/admin-surveys");
            return;
        }

        SurveyDAO dao = new SurveyDAO();
        ArrayList<Survey> surveys = dao.getSurveysByUser(user.getId());
        request.setAttribute("surveys", surveys);

        @SuppressWarnings("unchecked")
        Map<Integer, SurveyDetail> sessionDetails = (session == null)
                ? null
                : (Map<Integer, SurveyDetail>) session.getAttribute("surveyDetailsById");

        Map<Integer, SurveyDetail> mergedDetails = new LinkedHashMap<>();
        QuestionDAO questionDAO = new QuestionDAO();

        if (surveys != null) {
            for (Survey s : surveys) {
                SurveyDetail detail = (sessionDetails == null) ? null : sessionDetails.get(s.getId());
                if (detail == null || detail.getQuestions() == null || detail.getQuestions().isEmpty()) {
                    ArrayList<QuestionDetail> questions = questionDAO.getQuestionsBySurvey(s.getId());
                    detail = new SurveyDetail(s.getId(), s.getTitle(), questions);
                }
                mergedDetails.put(s.getId(), detail);
            }
        }

        request.setAttribute("surveyDetailsById", mergedDetails);

        request.getRequestDispatcher("views/surveyList.jsp")
                .forward(request, response);
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
