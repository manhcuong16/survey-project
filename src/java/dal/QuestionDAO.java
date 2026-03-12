package dal;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import static java.util.Collections.list;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import models.QuestionDetail;

public class QuestionDAO extends DBContext {

    private static class OptionTableInfo {
        private final String table;
        private final String idCol;
        private final String questionIdCol;
        private final String contentCol;

        private OptionTableInfo(String table, String idCol, String questionIdCol, String contentCol) {
            this.table = table;
            this.idCol = idCol;
            this.questionIdCol = questionIdCol;
            this.contentCol = contentCol;
        }
    }

    private static String qualify(String schema, String table) {
        String t = "[" + table + "]";
        if (schema == null || schema.trim().isEmpty()) {
            return t;
        }
        return "[" + schema + "]." + t;
    }

    private static String resolveQuestionTable(Connection con) throws Exception {
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
            if (table.equalsIgnoreCase("Question")) {
                exact = full;
                break;
            }
            if (plural == null && table.equalsIgnoreCase("Questions")) {
                plural = full;
            }
            if (contains == null && table.toLowerCase().contains("question")) {
                contains = full;
            }
        }
        rs.close();
        if (exact != null) return exact;
        if (plural != null) return plural;
        return contains;
    }

    private static OptionTableInfo resolveOptionTable(Connection con) throws Exception {
        DatabaseMetaData md = con.getMetaData();
        ResultSet rs = md.getTables(null, null, "%", new String[] {"TABLE"});
        while (rs.next()) {
            String table = rs.getString("TABLE_NAME");
            String schema = rs.getString("TABLE_SCHEM");
            if (table == null) {
                continue;
            }
            String lower = table.toLowerCase();
            if (!lower.contains("option") && !lower.contains("choice")) {
                continue;
            }
            String full = qualify(schema, table);
            Map<String, String> cols = getColumns(con, full);
            String idCol = pickColumn(cols, "id", "optionid", "option_id", "choiceid", "choice_id");
            String qidCol = pickColumn(cols, "questionid", "question_id", "questionId", "qid");
            String contentCol = pickColumn(cols, "content", "option", "optiontext", "option_text", "text", "value", "label", "choice");
            if (qidCol != null && contentCol != null) {
                rs.close();
                return new OptionTableInfo(full, idCol, qidCol, contentCol);
            }
        }
        rs.close();
        return null;
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

    private static ArrayList<String> splitOptions(String raw) {
        ArrayList<String> list = new ArrayList<>();
        if (raw == null) {
            return list;
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return list;
        }
        String[] parts;
        if (value.contains("||")) {
            parts = value.split("\\|\\|");
        } else if (value.contains("|")) {
            parts = value.split("\\|");
        } else if (value.contains(";")) {
            parts = value.split(";");
        } else if (value.contains(",")) {
            parts = value.split(",");
        } else {
            parts = new String[] { value };
        }
        for (String p : parts) {
            if (p == null) {
                continue;
            }
            String v = p.trim();
            if (!v.isEmpty()) {
                list.add(v);
            }
        }
        return list;
    }

    private static String joinOptions(ArrayList<String> options) {
        if (options == null || options.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (String opt : options) {
            if (opt == null) {
                continue;
            }
            String v = opt.trim();
            if (v.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append("|");
            }
            sb.append(v);
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private static ArrayList<String> getOptionsByQuestion(Connection con, OptionTableInfo info, int questionId) throws Exception {
        ArrayList<String> list = new ArrayList<>();
        if (info == null) {
            return list;
        }
        String sql = "SELECT " + info.contentCol + " FROM " + info.table + " WHERE " + info.questionIdCol + " = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, questionId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            String val = rs.getString(1);
            if (val != null && !val.trim().isEmpty()) {
                list.add(val.trim());
            }
        }
        rs.close();
        ps.close();
        return list;
    }

    public Integer findOptionId(int questionId, String content) {
        if (content == null || content.trim().isEmpty()) {
            return null;
        }

        try {
            Connection con = getConnection();
            if (con == null) {
                throw new IllegalStateException("DB connection is null");
            }

            OptionTableInfo info = resolveOptionTable(con);
            if (info == null || info.idCol == null) {
                return null;
            }

            String sql = "SELECT TOP 1 " + info.idCol + " FROM " + info.table
                    + " WHERE " + info.questionIdCol + " = ? AND " + info.contentCol + " = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, questionId);
            ps.setString(2, content.trim());
            ResultSet rs = ps.executeQuery();
            Integer result = null;
            if (rs.next()) {
                result = rs.getInt(1);
            }
            rs.close();
            ps.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public String findOptionTextById(int optionId) {
        try {
            Connection con = getConnection();
            if (con == null) {
                throw new IllegalStateException("DB connection is null");
            }

            OptionTableInfo info = resolveOptionTable(con);
            if (info == null || info.idCol == null) {
                return null;
            }

            String sql = "SELECT TOP 1 " + info.contentCol + " FROM " + info.table
                    + " WHERE " + info.idCol + " = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, optionId);
            ResultSet rs = ps.executeQuery();
            String result = null;
            if (rs.next()) {
                result = rs.getString(1);
            }
            rs.close();
            ps.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean createQuestions(int surveyId, List<QuestionDetail> questions) {
        if (questions == null || questions.isEmpty()) {
            return true;
        }

        boolean ok = true;
        for (QuestionDetail q : questions) {
            int id = createQuestionAndReturnId(surveyId, q);
            if (id == -1) {
                ok = false;
            } else if (q != null) {
                q.setId(id);
            }
        }
        return ok;
    }

    public int createQuestionAndReturnId(int surveyId, QuestionDetail question) {
        int questionId = -1;

        try {
            Connection con = getConnection();
            if (con == null) {
                throw new IllegalStateException("DB connection is null");
            }

            String table = resolveQuestionTable(con);
            if (table == null) {
                throw new IllegalStateException("Question table not found in database");
            }

            Map<String, String> cols = getColumns(con, table);
            String idCol = pickColumn(cols, "id", "questionid", "question_id");
            String surveyIdCol = pickColumn(cols, "surveyid", "survey_id", "surveyId");
            String contentCol = pickColumn(cols, "content", "question", "questiontext", "question_text", "text");
            String typeCol = pickColumn(cols, "type", "qtype", "questiontype", "question_type");
            String optionsCol = pickColumn(cols, "options", "choices", "choice", "option", "optiontext", "option_text");

            if (surveyIdCol == null || contentCol == null) {
                throw new IllegalStateException("Question columns not found in database");
            }

            ArrayList<String> insertCols = new ArrayList<>();
            ArrayList<Object> params = new ArrayList<>();

            insertCols.add(surveyIdCol);
            params.add(surveyId);

            insertCols.add(contentCol);
            params.add(question == null ? null : question.getContent());

            if (typeCol != null) {
                insertCols.add(typeCol);
                params.add(question == null ? null : question.getType());
            }

            String optionsValue = null;
            if (optionsCol != null && question != null) {
                optionsValue = joinOptions(question.getOptions());
                if (optionsValue != null) {
                    insertCols.add(optionsCol);
                    params.add(optionsValue);
                }
            }

            if (insertCols.isEmpty()) {
                throw new IllegalStateException("No matching columns to insert into Question");
            }

            StringBuilder sb = new StringBuilder();
            sb.append("INSERT INTO ").append(table).append("(");
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

            if (idCol != null) {
                String sql = "SELECT TOP 1 " + idCol + " FROM " + table;
                if (surveyIdCol != null) {
                    sql += " WHERE " + surveyIdCol + " = ?";
                }
                sql += " ORDER BY " + idCol + " DESC";
                PreparedStatement psId = con.prepareStatement(sql);
                if (surveyIdCol != null) {
                    psId.setInt(1, surveyId);
                }
                ResultSet rs = psId.executeQuery();
                if (rs.next()) {
                    questionId = rs.getInt(1);
                }
                rs.close();
                psId.close();
            }

            if (questionId != -1 && question != null && question.getOptions() != null
                    && !question.getOptions().isEmpty()) {
                OptionTableInfo optionTable = resolveOptionTable(con);
                if (optionTable != null) {
                    String optSql = "INSERT INTO " + optionTable.table + "(" + optionTable.questionIdCol
                            + "," + optionTable.contentCol + ") VALUES (?,?)";
                    PreparedStatement psOpt = con.prepareStatement(optSql);
                    for (String opt : question.getOptions()) {
                        if (opt == null || opt.trim().isEmpty()) {
                            continue;
                        }
                        psOpt.setInt(1, questionId);
                        psOpt.setString(2, opt.trim());
                        psOpt.addBatch();
                    }
                    psOpt.executeBatch();
                    psOpt.close();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return questionId;
    }

    public ArrayList<QuestionDetail> getQuestionsBySurvey(int surveyId) {
        ArrayList<QuestionDetail> list = new ArrayList<>();

        try {
            Connection con = getConnection();
            if (con == null) {
                throw new IllegalStateException("DB connection is null");
            }

            String table = resolveQuestionTable(con);
            if (table == null) {
                throw new IllegalStateException("Question table not found in database");
            }

            Map<String, String> cols = getColumns(con, table);
            String surveyIdCol = pickColumn(cols, "surveyid", "survey_id", "surveyId");
            String idCol = pickColumn(cols, "id", "questionid", "question_id");
            String contentCol = pickColumn(cols, "content", "question", "questiontext", "question_text", "text");
            String typeCol = pickColumn(cols, "type", "qtype", "questiontype", "question_type");
            String optionsCol = pickColumn(cols, "options", "choices", "choice", "option", "optiontext", "option_text");

            if (surveyIdCol == null) {
                return list;
            }

            String sql = "SELECT * FROM " + table + " WHERE " + surveyIdCol + " = ?";
            if (idCol != null) {
                sql += " ORDER BY " + idCol;
            }
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, surveyId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int id = (idCol != null) ? rs.getInt(idCol) : getIntFirstExisting(rs, "id", "questionid", "question_id");
                String content = (contentCol != null) ? rs.getString(contentCol) : getStringFirstExisting(rs, "content", "question");
                String type = (typeCol != null) ? rs.getString(typeCol) : null;
                ArrayList<String> options = null;
                if (optionsCol != null) {
                    options = splitOptions(rs.getString(optionsCol));
                }
                list.add(new QuestionDetail(id, content, type, options));
            }

            rs.close();
            ps.close();

            if (!list.isEmpty()) {
                OptionTableInfo optionTable = resolveOptionTable(con);
                if (optionTable != null) {
                    for (QuestionDetail q : list) {
                        if (q == null) {
                            continue;
                        }
                        ArrayList<String> opts = q.getOptions();
                        if (opts == null || opts.isEmpty()) {
                            opts = getOptionsByQuestion(con, optionTable, q.getId());
                            q.setOptions(opts);
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }
}











