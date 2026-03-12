package controllers;

import dal.SurveyDAO;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import models.Survey;
import models.User;

public class ShareController extends HttpServlet {

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

    private String findLanIp() {
        try {
            Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
            while (nics.hasMoreElements()) {
                NetworkInterface nic = nics.nextElement();
                if (!nic.isUp() || nic.isLoopback() || nic.isVirtual()) {
                    continue;
                }
                Enumeration<InetAddress> addrs = nic.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof Inet4Address && addr.isSiteLocalAddress() && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    private String buildBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String server = request.getServerName();
        int port = request.getServerPort();
        String ctx = request.getContextPath();

        String host = server;
        if ("localhost".equalsIgnoreCase(server) || "127.0.0.1".equals(server)) {
            String lanIp = findLanIp();
            if (lanIp != null && !lanIp.trim().isEmpty()) {
                host = lanIp.trim();
            }
        }

        String portPart = (port == 80 || port == 443) ? "" : (":" + port);
        return scheme + "://" + host + portPart + ctx;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Integer surveyId = tryParseInt(request.getParameter("id"));
        if (surveyId == null) {
            response.sendRedirect(request.getContextPath() + "/survey-list");
            return;
        }

        SurveyDAO surveyDAO = new SurveyDAO();
        Survey survey = surveyDAO.getSurveyById(surveyId);
        if (survey == null) {
            response.sendRedirect(request.getContextPath() + "/survey-list");
            return;
        }

        HttpSession session = request.getSession(false);
        User user = (session == null) ? null : (User) session.getAttribute("user");

        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login?next=/share?id=" + surveyId);
            return;
        }

        boolean isCreator = survey.getCreatedBy() > 0 && survey.getCreatedBy() == user.getId();
        if (isCreator) {
            String shareLink = buildBaseUrl(request) + "/share?id=" + surveyId;
            request.setAttribute("survey", survey);
            request.setAttribute("shareLink", shareLink);
            request.getRequestDispatcher("views/share.jsp").forward(request, response);
            return;
        }

        response.sendRedirect(request.getContextPath() + "/do-survey?id=" + surveyId);
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
