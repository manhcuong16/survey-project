package models;

import java.util.ArrayList;

public class QuestionDetail {

    private int id;
    private String content;
    private String type;
    private ArrayList<String> options;

    public QuestionDetail() {
    }

    public QuestionDetail(String content, String type, ArrayList<String> options) {
        this.content = content;
        this.type = type;
        this.options = options;
    }

    public QuestionDetail(int id, String content, String type, ArrayList<String> options) {
        this.id = id;
        this.content = content;
        this.type = type;
        this.options = options;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ArrayList<String> getOptions() {
        return options;
    }

    public void setOptions(ArrayList<String> options) {
        this.options = options;
    }
}
