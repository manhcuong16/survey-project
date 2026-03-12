package models;

import java.util.ArrayList;


public class SurveyDetail {

    private int surveyId;
    private String title;
    private ArrayList<QuestionDetail> questions;

    public SurveyDetail() {
    }

    public SurveyDetail(int surveyId, String title, ArrayList<QuestionDetail> questions) {
        this.surveyId = surveyId;
        this.title = title;
        this.questions = questions;
    }

    public int getSurveyId() {
        return surveyId;
    }

    public void setSurveyId(int surveyId) {
        this.surveyId = surveyId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public ArrayList<QuestionDetail> getQuestions() {
        return questions;
    }

    public void setQuestions(ArrayList<QuestionDetail> questions) {
        this.questions = questions;
    }
}
