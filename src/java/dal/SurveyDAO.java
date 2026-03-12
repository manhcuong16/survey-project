package dal;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author ADMIN
 */
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import models.Survey;

public class SurveyDAO extends DBContext {

    public void createSurvey(String title, int userId) {
        createSurveyAndReturnId(title, null, userId);
    }

    private static String qualify(String schema, String table) {
        String t = "[" + table + "]";
        if (schema == null || schema.trim().isEmpty()) {
            return t;
        }
        return "[" + schema + "]." + t;
    }

    private static String resolveSurveyTable(Connection con) throws Exception {
        DatabaseMetaData md = con.getMetaData();
        ResultSet rs = md.getTables(null, null, "%", new String[] {"TABLE"});
        String exact = null;
        String plural = null;
        String contains = null;
        while (rs.next()) {
            String table = rs.getString("TABLE_NAME");
            String schema = rs.getString("TABLE_SCHEM");
            if (table == null) continue;
            String full = qualify(schema, table);
            if (table.equalsIgnoreCase("Survey")) {
                exact = full;
                break;
            }
            if (plural == null && table.equalsIgnoreCase("Surveys")) {
                plural = full;
            }
            if (contains == null && table.toLowerCase().contains("survey")) {
                contains = full;
            }
        }
        rs.close();
        if (exact != null) return exact;
        if (plural != null) return plural;
        return contains;
    }

    private static Map<String, String> getSurveyColumns(Connection con, String table) throws Exception {
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

    public int createSurveyAndReturnId(String title, String description, int userId) {

        int surveyId = -1;

        try {

            Connection con = getConnection();
            if (con == null) {
                throw new IllegalStateException("DB connection is null");
            }

            String table = resolveSurveyTable(con);
            if (table == null) {
                throw new IllegalStateException("Survey table not found in database");
            }

            Map<String, String> cols = getSurveyColumns(con, table);

            String titleCol = pickColumn(cols, "title", "name", "surveytitle", "survey_title");
            String descCol = pickColumn(cols, "description", "desc", "survey_description");
            String createdByCol = pickColumn(cols, "createdby", "created_by", "userid", "user_id", "createdbyid");
            String idCol = pickColumn(cols, "id", "surveyid", "survey_id");

            ArrayList<String> insertCols = new ArrayList<>();
            ArrayList<Object> params = new ArrayList<>();

            if (titleCol != null) {
                insertCols.add(titleCol);
                params.add(title);
            }
            if (descCol != null && description != null) {
                insertCols.add(descCol);
                params.add(description);
            }
            if (createdByCol != null) {
                insertCols.add(createdByCol);
                params.add(userId);
            }

            if (insertCols.isEmpty()) {
                throw new IllegalStateException("No matching columns to insert into Survey");
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
                Statement st = con.createStatement();
                ResultSet rs = st.executeQuery("SELECT TOP 1 " + idCol + " FROM " + table + " ORDER BY " + idCol + " DESC");
                if (rs.next()) {
                    surveyId = rs.getInt(1);
                }
                rs.close();
                st.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return surveyId;
    }

    public boolean deleteSurveyById(int surveyId, Integer userId) {

        try {
            Connection con = getConnection();
            if (con == null) {
                throw new IllegalStateException("DB connection is null");
            }

            String table = resolveSurveyTable(con);
            if (table == null) {
                throw new IllegalStateException("Survey table not found in database");
            }

            Map<String, String> cols = getSurveyColumns(con, table);
            String idCol = pickColumn(cols, "id", "surveyid", "survey_id");
            String createdByCol = pickColumn(cols, "createdby", "created_by", "userid", "user_id", "createdbyid");

            if (idCol == null) {
                throw new IllegalStateException("Survey id column not found in database");
            }

            String sql;
            PreparedStatement ps;

            if (createdByCol != null && userId != null) {
                sql = "DELETE FROM " + table + " WHERE " + idCol + " = ? AND " + createdByCol + " = ?";
                ps = con.prepareStatement(sql);
                ps.setInt(1, surveyId);
                ps.setInt(2, userId);
            } else {
                sql = "DELETE FROM " + table + " WHERE " + idCol + " = ?";
                ps = con.prepareStatement(sql);
                ps.setInt(1, surveyId);
            }

            int affected = ps.executeUpdate();
            ps.close();
            return affected > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public Survey getSurveyById(int surveyId) {

        Survey survey = null;

        try {

            Connection con = getConnection();
            if (con == null) {
                throw new IllegalStateException("DB connection is null");
            }

            String table = resolveSurveyTable(con);
            if (table == null) {
                throw new IllegalStateException("Survey table not found in database");
            }

            Map<String, String> cols = getSurveyColumns(con, table);
            String idCol = pickColumn(cols, "id", "surveyid", "survey_id");
            String titleCol = pickColumn(cols, "title", "name", "surveyTitle", "survey_title");
            String createdByCol = pickColumn(cols, "createdby", "created_by", "userid", "user_id", "createdbyid");
            String createdDateCol = pickColumn(cols, "createddate", "created_date", "createdat", "created_at");
            String lockedCol = pickColumn(cols, "islocked", "locked", "is_lock", "lock");

            if (idCol == null) {
                throw new IllegalStateException("Survey id column not found in database");
            }

            String sql = "SELECT * FROM " + table + " WHERE " + idCol + " = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, surveyId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                survey = new Survey();
                survey.setId(rs.getInt(idCol));
                if (titleCol != null) {
                    survey.setTitle(rs.getString(titleCol));
                }
                if (createdByCol != null) {
                    survey.setCreatedBy(rs.getInt(createdByCol));
                }
                if (createdDateCol != null) {
                    java.sql.Timestamp ts = rs.getTimestamp(createdDateCol);
                    if (ts != null) {
                        survey.setCreatedDate(new java.util.Date(ts.getTime()));
                    }
                }
                if (lockedCol != null) {
                    survey.setLocked(rs.getBoolean(lockedCol));
                }
            }

            rs.close();
            ps.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return survey;
    }

    public ArrayList<Survey> getSurveysByUser(int userId) {

        ArrayList<Survey> list = new ArrayList<>();

        try {

            Connection con = getConnection();
            if (con == null) {
                throw new IllegalStateException("DB connection is null");
            }

            String table = resolveSurveyTable(con);
            if (table == null) {
                throw new IllegalStateException("Survey table not found in database");
            }

            Map<String, String> cols = getSurveyColumns(con, table);
            String idCol = pickColumn(cols, "id", "surveyid", "surveyId", "survey_id");
            String titleCol = pickColumn(cols, "title", "name", "surveyTitle", "survey_title");
            String createdByCol = pickColumn(cols, "createdby", "created_by", "userid", "user_id", "createdbyid");
            String createdDateCol = pickColumn(cols, "createddate", "created_date", "createdat", "created_at");
            String lockedCol = pickColumn(cols, "islocked", "locked", "is_lock", "lock");

            if (createdByCol == null) {
                throw new IllegalStateException("Survey createdBy column not found in database");
            }

            String sql = "SELECT * FROM " + table + " WHERE " + createdByCol + " = ?";
            if (idCol != null) {
                sql += " ORDER BY " + idCol;
            }

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Survey s = new Survey();

                int id = (idCol != null) ? rs.getInt(idCol)
                        : getIntFirstExisting(rs, "id", "surveyid", "surveyId", "survey_id");
                String title = (titleCol != null) ? rs.getString(titleCol)
                        : getStringFirstExisting(rs, "title", "name", "surveyTitle", "survey_title");

                s.setId(id);
                s.setTitle(title);

                if (createdByCol != null) {
                    s.setCreatedBy(rs.getInt(createdByCol));
                }
                if (createdDateCol != null) {
                    java.sql.Timestamp ts = rs.getTimestamp(createdDateCol);
                    if (ts != null) {
                        s.setCreatedDate(new java.util.Date(ts.getTime()));
                    }
                }
                if (lockedCol != null) {
                    s.setLocked(rs.getBoolean(lockedCol));
                }

                list.add(s);
            }

            rs.close();
            ps.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public boolean setSurveyLocked(int surveyId, boolean locked) {
        try {
            Connection con = getConnection();
            if (con == null) {
                throw new IllegalStateException("DB connection is null");
            }

            String table = resolveSurveyTable(con);
            if (table == null) {
                throw new IllegalStateException("Survey table not found in database");
            }

            Map<String, String> cols = getSurveyColumns(con, table);
            String idCol = pickColumn(cols, "id", "surveyid", "surveyId", "survey_id");
            String lockedCol = pickColumn(cols, "islocked", "locked", "is_lock", "lock");

            if (idCol == null || lockedCol == null) {
                throw new IllegalStateException("Survey lock column not found in database");
            }

            String sql = "UPDATE " + table + " SET " + lockedCol + " = ? WHERE " + idCol + " = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setBoolean(1, locked);
            ps.setInt(2, surveyId);
            int affected = ps.executeUpdate();
            ps.close();
            return affected > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public ArrayList<Survey> getAllSurveys() {

        ArrayList<Survey> list = new ArrayList<>();

        try {

            Connection con = getConnection();
            if (con == null) {
                throw new IllegalStateException("DB connection is null");
            }

            String table = resolveSurveyTable(con);
            if (table == null) {
                throw new IllegalStateException("Survey table not found in database");
            }

            Map<String, String> cols = getSurveyColumns(con, table);
            String idCol = pickColumn(cols, "id", "surveyid", "surveyId", "survey_id");
            String titleCol = pickColumn(cols, "title", "name", "surveyTitle", "survey_title");
            String createdByCol = pickColumn(cols, "createdby", "created_by", "userid", "user_id", "createdbyid");
            String createdDateCol = pickColumn(cols, "createddate", "created_date", "createdat", "created_at");
            String lockedCol = pickColumn(cols, "islocked", "locked", "is_lock", "lock");

            String sql = "SELECT * FROM " + table;
            if (idCol != null) {
                sql += " ORDER BY " + idCol;
            }

            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                Survey s = new Survey();

                int id = (idCol != null) ? rs.getInt(idCol)
                        : getIntFirstExisting(rs, "id", "surveyid", "surveyId", "survey_id");
                String title = (titleCol != null) ? rs.getString(titleCol)
                        : getStringFirstExisting(rs, "title", "name", "surveyTitle", "survey_title");

                s.setId(id);
                s.setTitle(title);

                if (createdByCol != null) {
                    s.setCreatedBy(rs.getInt(createdByCol));
                }
                if (createdDateCol != null) {
                    java.sql.Timestamp ts = rs.getTimestamp(createdDateCol);
                    if (ts != null) {
                        s.setCreatedDate(new java.util.Date(ts.getTime()));
                    }
                }
                if (lockedCol != null) {
                    s.setLocked(rs.getBoolean(lockedCol));
                }

                list.add(s);
            }

            rs.close();
            ps.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }
}













