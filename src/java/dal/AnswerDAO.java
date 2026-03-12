package dal;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import models.Answer;

public class AnswerDAO extends DBContext {

    private static class ResponseTableInfo {
        private final String table;
        private final String idCol;
        private final String surveyIdCol;
        private final String userIdCol;

        private ResponseTableInfo(String table, String idCol, String surveyIdCol, String userIdCol) {
            this.table = table;
            this.idCol = idCol;
            this.surveyIdCol = surveyIdCol;
            this.userIdCol = userIdCol;
        }
    }

    private static String qualify(String schema, String table) {
        String t = "[" + table + "]";
        if (schema == null || schema.trim().isEmpty()) {
            return t;
        }
        return "[" + schema + "]." + t;
    }

    private static String resolveAnswerTable(Connection con) throws Exception {
        DatabaseMetaData md = con.getMetaData();
        ResultSet rs = md.getTables(null, null, "%", new String[] {"TABLE"});
        String exact = null;
        String plural = null;
        String contains = null;
        while (rs.next()) {
            String table = rs.getString("TABLE_NAME");
            String schema = rs.getString("TABLE_SCHEM");
            if (table == null) {
                continue;
            }
            String full = qualify(schema, table);
            if (table.equalsIgnoreCase("Answer")) {
                exact = full;
                break;
            }
            if (plural == null && table.equalsIgnoreCase("Answers")) {
                plural = full;
            }
            if (contains == null && table.toLowerCase().contains("answer")) {
                contains = full;
            }
        }
        rs.close();
        if (exact != null) return exact;
        if (plural != null) return plural;
        return contains;
    }

    private static ResponseTableInfo resolveResponseTable(Connection con) throws Exception {
        DatabaseMetaData md = con.getMetaData();
        ResultSet rs = md.getTables(null, null, "%", new String[] {"TABLE"});
        ResponseTableInfo fallback = null;
        while (rs.next()) {
            String table = rs.getString("TABLE_NAME");
            String schema = rs.getString("TABLE_SCHEM");
            if (table == null) {
                continue;
            }
            String lower = table.toLowerCase();
            if (!lower.contains("response")) {
                continue;
            }
            String full = qualify(schema, table);
            Map<String, String> cols = getColumns(con, full);
            String idCol = pickColumn(cols, "responseid", "response_id", "id");
            String surveyIdCol = pickColumn(cols, "surveyid", "survey_id", "surveyId");
            String userIdCol = pickColumn(cols, "userid", "user_id", "createdby", "created_by");
            ResponseTableInfo info = new ResponseTableInfo(full, idCol, surveyIdCol, userIdCol);
            if (table.equalsIgnoreCase("Response") || table.equalsIgnoreCase("Responses")) {
                rs.close();
                return info;
            }
            if (fallback == null) {
                fallback = info;
            }
        }
        rs.close();
        return fallback;
    }

    private static Map<String, String> getColumns(Connection con, String table) throws Exception {
        Map<String, String> map = new HashMap<>();
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery("SELECT TOP 0 * FROM " + table);
        ResultSetMetaData md = rs.getMetaData();
        int n = md.getColumnCount();
        for (int i = 1; i <= n; i++) {
            String label = md.getColumnLabel(i);
            if (label == null || label.trim().isEmpty()) {
                label = md.getColumnName(i);
            }
            if (label != null) {
                map.put(label.toLowerCase(), label);
            }
        }
        rs.close();
        st.close();
        return map;
    }

    private static String pickColumn(Map<String, String> map, String... candidates) {
        for (String c : candidates) {
            String v = map.get(c.toLowerCase());
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    private static boolean hasColumn(ResultSet rs, String col) {
        try {
            ResultSetMetaData md = rs.getMetaData();
            int n = md.getColumnCount();
            for (int i = 1; i <= n; i++) {
                String label = md.getColumnLabel(i);
                if (label != null && label.equalsIgnoreCase(col)) {
                    return true;
                }
                String name = md.getColumnName(i);
                if (name != null && name.equalsIgnoreCase(col)) {
                    return true;
                }
            }
        } catch (Exception e) {
        }
        return false;
    }

    private static int getIntFirstExisting(ResultSet rs, String... cols) throws Exception {
        for (String c : cols) {
            if (hasColumn(rs, c)) {
                return rs.getInt(c);
            }
        }
        Object o = rs.getObject(1);
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        return Integer.parseInt(String.valueOf(o));
    }

    private static String getStringFirstExisting(ResultSet rs, String... cols) throws Exception {
        for (String c : cols) {
            if (hasColumn(rs, c)) {
                return rs.getString(c);
            }
        }
        return rs.getString(2);
    }

    private int insertResponse(Connection con, ResponseTableInfo info, int surveyId, int userId) throws Exception {
        if (info == null || info.idCol == null) {
            throw new IllegalStateException("Response table not found in database");
        }

        if (info.surveyIdCol == null && info.userIdCol == null) {
            String sql = "INSERT INTO " + info.table + " DEFAULT VALUES";
            Statement st = con.createStatement();
            st.executeUpdate(sql);
            st.close();
        } else {
            ArrayList<String> insertCols = new ArrayList<>();
            ArrayList<Object> params = new ArrayList<>();
            if (info.surveyIdCol != null) {
                insertCols.add(info.surveyIdCol);
                params.add(surveyId);
            }
            if (info.userIdCol != null) {
                insertCols.add(info.userIdCol);
                params.add(userId);
            }

            StringBuilder sb = new StringBuilder();
            sb.append("INSERT INTO ").append(info.table).append("(");
            for (int i = 0; i < insertCols.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(insertCols.get(i));
            }
            sb.append(") VALUES (");
            for (int i = 0; i < insertCols.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append("?");
            }
            sb.append(")");

            PreparedStatement ps = con.prepareStatement(sb.toString());
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            ps.executeUpdate();
            ps.close();
        }

        int responseId = -1;
        String sql = "SELECT TOP 1 " + info.idCol + " FROM " + info.table + " ORDER BY " + info.idCol + " DESC";
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(sql);
        if (rs.next()) {
            responseId = rs.getInt(1);
        }
        rs.close();
        st.close();
        return responseId;
    }

    public int insertAnswers(List<Answer> answers) {
        if (answers == null || answers.isEmpty()) {
            return 0;
        }

        int inserted = 0;

        try {
            Connection con = getConnection();
            if (con == null) {
                throw new IllegalStateException("DB connection is null");
            }

            String answerTable = resolveAnswerTable(con);
            if (answerTable == null) {
                throw new IllegalStateException("Answer table not found in database");
            }

            ResponseTableInfo responseInfo = resolveResponseTable(con);
            if (responseInfo == null) {
                throw new IllegalStateException("Response table not found in database");
            }

            Answer first = null;
            for (Answer a : answers) {
                if (a != null) {
                    first = a;
                    break;
                }
            }
            if (first == null) {
                return 0;
            }

            int responseId = insertResponse(con, responseInfo, first.getSurveyId(), first.getUserId());
            if (responseId == -1) {
                return 0;
            }

            Map<String, String> cols = getColumns(con, answerTable);
            String responseIdCol = pickColumn(cols, "responseid", "response_id", "responseId");
            String questionIdCol = pickColumn(cols, "questionid", "question_id", "questionId");
            String optionIdCol = pickColumn(cols, "optionid", "option_id", "choiceid", "choice_id");
            String answerTextCol = pickColumn(cols, "answer", "answertext", "answer_text", "content", "value", "response", "response_text", "text");
            String createdDateCol = pickColumn(cols, "createddate", "created_date", "createdat", "created_at");

            if (questionIdCol == null || responseIdCol == null) {
                throw new IllegalStateException("Answer columns not found in database");
            }

            ArrayList<String> insertCols = new ArrayList<>();
            insertCols.add(responseIdCol);
            insertCols.add(questionIdCol);
            if (optionIdCol != null) {
                insertCols.add(optionIdCol);
            }
            if (answerTextCol != null) {
                insertCols.add(answerTextCol);
            }
            if (createdDateCol != null) {
                insertCols.add(createdDateCol);
            }

            StringBuilder sb = new StringBuilder();
            sb.append("INSERT INTO ").append(answerTable).append("(");
            for (int i = 0; i < insertCols.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(insertCols.get(i));
            }
            sb.append(") VALUES (");
            for (int i = 0; i < insertCols.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append("?");
            }
            sb.append(")");

            PreparedStatement ps = con.prepareStatement(sb.toString());
            QuestionDAO questionDAO = new QuestionDAO();

            for (Answer a : answers) {
                if (a == null) {
                    continue;
                }
                Integer optionId = (a.getOptionId() > 0) ? a.getOptionId() : null;
                if (optionId == null && optionIdCol != null) {
                    optionId = questionDAO.findOptionId(a.getQuestionId(), a.getContent());
                }

                String content = a.getContent();
                boolean hasText = content != null && !content.trim().isEmpty();

                if (optionId == null && hasText && answerTextCol == null) {
                    throw new IllegalStateException("Answer text column not found in database");
                }

                int idx = 1;
                ps.setInt(idx++, responseId);
                ps.setInt(idx++, a.getQuestionId());
                if (optionIdCol != null) {
                    if (optionId == null) {
                        ps.setNull(idx++, Types.INTEGER);
                    } else {
                        ps.setInt(idx++, optionId);
                    }
                }
                if (answerTextCol != null) {
                    if (hasText) {
                        ps.setString(idx++, content.trim());
                    } else {
                        ps.setNull(idx++, Types.NVARCHAR);
                    }
                }
                if (createdDateCol != null) {
                    ps.setTimestamp(idx++, new Timestamp(System.currentTimeMillis()));
                }
                ps.addBatch();
            }

            int[] res = ps.executeBatch();
            ps.close();

            if (res != null) {
                for (int r : res) {
                    if (r == Statement.SUCCESS_NO_INFO) {
                        inserted += 1;
                    } else if (r > 0) {
                        inserted += r;
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return inserted;
    }

    public ArrayList<Answer> getAnswersBySurvey(int surveyId, List<Integer> questionIds) {
        return getAnswersBySurveyInternal(surveyId, questionIds, null);
    }

    public ArrayList<Answer> getAnswersBySurveyForUser(int surveyId, int userId, List<Integer> questionIds) {
        return getAnswersBySurveyInternal(surveyId, questionIds, Integer.valueOf(userId));
    }

    private ArrayList<Answer> getAnswersBySurveyInternal(int surveyId, List<Integer> questionIds, Integer userId) {
        ArrayList<Answer> list = new ArrayList<>();

        try {
            Connection con = getConnection();
            if (con == null) {
                throw new IllegalStateException("DB connection is null");
            }

            String answerTable = resolveAnswerTable(con);
            if (answerTable == null) {
                throw new IllegalStateException("Answer table not found in database");
            }

            ResponseTableInfo responseInfo = resolveResponseTable(con);

            Map<String, String> cols = getColumns(con, answerTable);
            String responseIdCol = pickColumn(cols, "responseid", "response_id", "responseId");
            String questionIdCol = pickColumn(cols, "questionid", "question_id", "questionId");
            String optionIdCol = pickColumn(cols, "optionid", "option_id", "choiceid", "choice_id");
            String answerTextCol = pickColumn(cols, "answer", "answertext", "answer_text", "content", "value", "response", "response_text", "text");
            String surveyIdCol = pickColumn(cols, "surveyid", "survey_id", "surveyId");
            String idCol = pickColumn(cols, "id", "answerid", "answer_id");
            String userIdCol = pickColumn(cols, "userid", "user_id", "createdby", "created_by");
            String createdDateCol = pickColumn(cols, "createddate", "created_date", "createdat", "created_at");

            boolean wantUser = userId != null;
            boolean usedQuestionFilter = false;
            boolean useAlias = false;
            StringBuilder sb = new StringBuilder();
            ArrayList<Object> params = new ArrayList<>();

            if (wantUser) {
                if (surveyIdCol != null && userIdCol != null) {
                    sb.append("SELECT * FROM ").append(answerTable)
                            .append(" WHERE ").append(surveyIdCol).append(" = ? AND ")
                            .append(userIdCol).append(" = ?");
                    params.add(surveyId);
                    params.add(userId);
                } else if (responseIdCol != null && responseInfo != null && responseInfo.idCol != null
                        && responseInfo.surveyIdCol != null && responseInfo.userIdCol != null) {
                    useAlias = true;
                    sb.append("SELECT a.* FROM ").append(answerTable).append(" a INNER JOIN ")
                            .append(responseInfo.table).append(" r ON a.").append(responseIdCol)
                            .append(" = r.").append(responseInfo.idCol)
                            .append(" WHERE r.").append(responseInfo.surveyIdCol).append(" = ? AND r.")
                            .append(responseInfo.userIdCol).append(" = ?");
                    params.add(surveyId);
                    params.add(userId);
                } else {
                    return list;
                }
            } else {
                if (surveyIdCol != null) {
                    sb.append("SELECT * FROM ").append(answerTable)
                            .append(" WHERE ").append(surveyIdCol).append(" = ?");
                    params.add(surveyId);
                } else if (responseIdCol != null && responseInfo != null && responseInfo.idCol != null
                        && responseInfo.surveyIdCol != null) {
                    useAlias = true;
                    sb.append("SELECT a.* FROM ").append(answerTable).append(" a INNER JOIN ")
                            .append(responseInfo.table).append(" r ON a.").append(responseIdCol)
                            .append(" = r.").append(responseInfo.idCol)
                            .append(" WHERE r.").append(responseInfo.surveyIdCol).append(" = ?");
                    params.add(surveyId);
                } else if (questionIdCol != null && questionIds != null && !questionIds.isEmpty()) {
                    sb.append("SELECT * FROM ").append(answerTable).append(" WHERE ").append(questionIdCol).append(" IN (");
                    for (int i = 0; i < questionIds.size(); i++) {
                        if (i > 0) sb.append(",");
                        sb.append("?");
                    }
                    sb.append(")");
                    for (Integer qid : questionIds) {
                        params.add(qid);
                    }
                    usedQuestionFilter = true;
                } else {
                    return list;
                }
            }

            if (!usedQuestionFilter && questionIdCol != null && questionIds != null && !questionIds.isEmpty()) {
                sb.append(" AND ").append(useAlias ? "a." : "").append(questionIdCol).append(" IN (");
                for (int i = 0; i < questionIds.size(); i++) {
                    if (i > 0) sb.append(",");
                    sb.append("?");
                }
                sb.append(")");
                for (Integer qid : questionIds) {
                    params.add(qid);
                }
            }

            PreparedStatement ps = con.prepareStatement(sb.toString());
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            ResultSet rs = ps.executeQuery();
            QuestionDAO questionDAO = new QuestionDAO();

            while (rs.next()) {
                Answer ans = new Answer();
                if (idCol != null) {
                    ans.setId(rs.getInt(idCol));
                }
                if (responseIdCol != null) {
                    ans.setResponseId(rs.getInt(responseIdCol));
                }
                if (questionIdCol != null) {
                    ans.setQuestionId(rs.getInt(questionIdCol));
                } else {
                    ans.setQuestionId(getIntFirstExisting(rs, "questionid", "question_id"));
                }
                if (optionIdCol != null) {
                    int optId = rs.getInt(optionIdCol);
                    if (!rs.wasNull()) {
                        ans.setOptionId(optId);
                    }
                }
                if (surveyIdCol != null) {
                    ans.setSurveyId(rs.getInt(surveyIdCol));
                } else {
                    ans.setSurveyId(surveyId);
                }
                if (userIdCol != null) {
                    ans.setUserId(rs.getInt(userIdCol));
                } else if (userId != null) {
                    ans.setUserId(userId.intValue());
                }
                if (createdDateCol != null) {
                    ans.setCreatedDate(rs.getTimestamp(createdDateCol));
                }

                String content = null;
                if (answerTextCol != null) {
                    content = rs.getString(answerTextCol);
                }
                if ((content == null || content.trim().isEmpty()) && optionIdCol != null) {
                    int optId = rs.getInt(optionIdCol);
                    if (!rs.wasNull()) {
                        content = questionDAO.findOptionTextById(optId);
                    }
                }
                if (content == null) {
                    content = getStringFirstExisting(rs, "answer", "response", "content", "value");
                }
                ans.setContent(content == null ? "" : content);

                list.add(ans);
            }
            rs.close();
            ps.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }
}
