package dal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import models.User;

public class UserDAO extends DBContext {

    private Map<String, String> getColumns(Connection con) throws Exception {
        Map<String, String> map = new HashMap<>();
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery("SELECT TOP 0 * FROM Users");
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

    private String pickColumn(Map<String, String> map, String... candidates) {
        for (String c : candidates) {
            String v = map.get(c.toLowerCase());
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    private boolean hasColumn(ResultSet rs, String col) {
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
            // ignore
        }
        return false;
    }

    private int getIntFirstExisting(ResultSet rs, String... cols) throws Exception {
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

    private String getStringFirstExisting(ResultSet rs, String... cols) throws Exception {
        for (String c : cols) {
            if (hasColumn(rs, c)) {
                return rs.getString(c);
            }
        }
        return rs.getString(2);
    }

    public User login(String username, String password) {
        try {
            Connection con = getConnection();
            if (con == null) {
                throw new IllegalStateException("DB connection is null");
            }

            Map<String, String> cols = getColumns(con);
            String idCol = pickColumn(cols, "userid", "id", "user_id");
            String usernameCol = pickColumn(cols, "username", "user", "user_name");
            String passwordCol = pickColumn(cols, "password", "pass", "pwd");
            String roleCol = pickColumn(cols, "role", "userrole", "user_role");

            if (usernameCol == null || passwordCol == null) {
                throw new IllegalStateException("User columns not found in database");
            }

            String sql = "SELECT * FROM Users WHERE " + usernameCol + "=? AND " + passwordCol + "=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                User user = new User();
                if (idCol != null) {
                    user.setId(rs.getInt(idCol));
                } else {
                    user.setId(getIntFirstExisting(rs, "userid", "id", "user_id"));
                }
                user.setUsername(rs.getString(usernameCol));
                user.setPassword(rs.getString(passwordCol));
                if (roleCol != null) {
                    user.setRole(rs.getString(roleCol));
                }
                rs.close();
                ps.close();
                return user;
            }
            rs.close();
            ps.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean usernameExists(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        try {
            Connection con = getConnection();
            if (con == null) {
                throw new IllegalStateException("DB connection is null");
            }

            Map<String, String> cols = getColumns(con);
            String usernameCol = pickColumn(cols, "username", "user", "user_name");
            if (usernameCol == null) {
                throw new IllegalStateException("User columns not found in database");
            }

            String sql = "SELECT TOP 1 1 FROM Users WHERE " + usernameCol + "=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, username.trim());
            ResultSet rs = ps.executeQuery();
            boolean exists = rs.next();
            rs.close();
            ps.close();
            return exists;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean usernameExistsForOtherUser(int userId, String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        try {
            Connection con = getConnection();
            if (con == null) {
                throw new IllegalStateException("DB connection is null");
            }

            Map<String, String> cols = getColumns(con);
            String usernameCol = pickColumn(cols, "username", "user", "user_name");
            String idCol = pickColumn(cols, "userid", "id", "user_id");
            if (usernameCol == null || idCol == null) {
                throw new IllegalStateException("User columns not found in database");
            }

            String sql = "SELECT TOP 1 1 FROM Users WHERE " + usernameCol + "=? AND " + idCol + "<>?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, username.trim());
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            boolean exists = rs.next();
            rs.close();
            ps.close();
            return exists;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean createUser(String username, String password) {
        return createUser(username, password, "USER");
    }

    public boolean createUser(String username, String password, String role) {
        if (username == null || username.trim().isEmpty() || password == null) {
            return false;
        }

        try {
            Connection con = getConnection();
            if (con == null) {
                throw new IllegalStateException("DB connection is null");
            }

            Map<String, String> cols = getColumns(con);
            String usernameCol = pickColumn(cols, "username", "user", "user_name");
            String passwordCol = pickColumn(cols, "password", "pass", "pwd");
            String roleCol = pickColumn(cols, "role", "userrole", "user_role");

            if (usernameCol == null || passwordCol == null) {
                throw new IllegalStateException("User columns not found in database");
            }

            ArrayList<String> insertCols = new ArrayList<>();
            ArrayList<Object> params = new ArrayList<>();

            insertCols.add(usernameCol);
            params.add(username.trim());

            insertCols.add(passwordCol);
            params.add(password);

            if (roleCol != null) {
                insertCols.add(roleCol);
                params.add(role == null ? "USER" : role.trim());
            }

            StringBuilder sb = new StringBuilder();
            sb.append("INSERT INTO Users(");
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

            int rows = ps.executeUpdate();
            ps.close();
            return rows > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public ArrayList<User> getAllUsers() {
        ArrayList<User> list = new ArrayList<>();

        try {
            Connection con = getConnection();
            if (con == null) {
                throw new IllegalStateException("DB connection is null");
            }

            Map<String, String> cols = getColumns(con);
            String idCol = pickColumn(cols, "userid", "id", "user_id");
            String usernameCol = pickColumn(cols, "username", "user", "user_name");
            String passwordCol = pickColumn(cols, "password", "pass", "pwd");
            String roleCol = pickColumn(cols, "role", "userrole", "user_role");

            String sql = "SELECT * FROM Users";
            if (idCol != null) {
                sql += " ORDER BY " + idCol;
            }

            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                User user = new User();
                int id = (idCol != null) ? rs.getInt(idCol)
                        : getIntFirstExisting(rs, "userid", "id", "user_id");
                user.setId(id);
                if (usernameCol != null) {
                    user.setUsername(rs.getString(usernameCol));
                } else {
                    user.setUsername(getStringFirstExisting(rs, "username", "user", "user_name"));
                }
                if (passwordCol != null) {
                    user.setPassword(rs.getString(passwordCol));
                }
                if (roleCol != null) {
                    user.setRole(rs.getString(roleCol));
                }
                list.add(user);
            }

            rs.close();
            ps.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public User getUserById(int userId) {
        try {
            Connection con = getConnection();
            if (con == null) {
                throw new IllegalStateException("DB connection is null");
            }

            Map<String, String> cols = getColumns(con);
            String idCol = pickColumn(cols, "userid", "id", "user_id");
            String usernameCol = pickColumn(cols, "username", "user", "user_name");
            String passwordCol = pickColumn(cols, "password", "pass", "pwd");
            String roleCol = pickColumn(cols, "role", "userrole", "user_role");

            if (idCol == null) {
                throw new IllegalStateException("User id column not found in database");
            }

            String sql = "SELECT * FROM Users WHERE " + idCol + "=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt(idCol));
                if (usernameCol != null) {
                    user.setUsername(rs.getString(usernameCol));
                }
                if (passwordCol != null) {
                    user.setPassword(rs.getString(passwordCol));
                }
                if (roleCol != null) {
                    user.setRole(rs.getString(roleCol));
                }
                rs.close();
                ps.close();
                return user;
            }

            rs.close();
            ps.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean updateUser(int userId, String username, String password, String role) {
        if (userId <= 0 || username == null || username.trim().isEmpty()) {
            return false;
        }

        try {
            Connection con = getConnection();
            if (con == null) {
                throw new IllegalStateException("DB connection is null");
            }

            Map<String, String> cols = getColumns(con);
            String idCol = pickColumn(cols, "userid", "id", "user_id");
            String usernameCol = pickColumn(cols, "username", "user", "user_name");
            String passwordCol = pickColumn(cols, "password", "pass", "pwd");
            String roleCol = pickColumn(cols, "role", "userrole", "user_role");

            if (idCol == null || usernameCol == null) {
                throw new IllegalStateException("User columns not found in database");
            }

            ArrayList<String> setCols = new ArrayList<>();
            ArrayList<Object> params = new ArrayList<>();

            setCols.add(usernameCol + "=?");
            params.add(username.trim());

            if (password != null && passwordCol != null) {
                setCols.add(passwordCol + "=?");
                params.add(password);
            }

            if (role != null && roleCol != null) {
                setCols.add(roleCol + "=?");
                params.add(role.trim());
            }

            if (setCols.isEmpty()) {
                return false;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("UPDATE Users SET ");
            for (int i = 0; i < setCols.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(setCols.get(i));
            }
            sb.append(" WHERE ").append(idCol).append("=?");

            PreparedStatement ps = con.prepareStatement(sb.toString());
            int idx = 1;
            for (Object p : params) {
                ps.setObject(idx++, p);
            }
            ps.setInt(idx, userId);

            int rows = ps.executeUpdate();
            ps.close();
            return rows > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean deleteUser(int userId) {
        if (userId <= 0) {
            return false;
        }
        try {
            Connection con = getConnection();
            if (con == null) {
                throw new IllegalStateException("DB connection is null");
            }

            Map<String, String> cols = getColumns(con);
            String idCol = pickColumn(cols, "userid", "id", "user_id");
            if (idCol == null) {
                throw new IllegalStateException("User id column not found in database");
            }

            String sql = "DELETE FROM Users WHERE " + idCol + "=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, userId);
            int rows = ps.executeUpdate();
            ps.close();
            return rows > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public int countAdmins() {
        try {
            Connection con = getConnection();
            if (con == null) {
                throw new IllegalStateException("DB connection is null");
            }

            Map<String, String> cols = getColumns(con);
            String roleCol = pickColumn(cols, "role", "userrole", "user_role");
            if (roleCol == null) {
                return 0;
            }

            String sql = "SELECT COUNT(*) FROM Users WHERE UPPER(" + roleCol + ") = 'ADMIN'";
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            int count = 0;
            if (rs.next()) {
                count = rs.getInt(1);
            }
            rs.close();
            ps.close();
            return count;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
}
