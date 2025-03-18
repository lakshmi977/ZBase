package Servlet;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;

import Model.User;
import Util.AuthService;

/**
 * Servlet implementation class CheckAuthServlet
 */
@WebServlet("/CheckAuthServlet")
public class CheckAuthServlet extends HttpServlet {
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");

		Cookie[] cookies = request.getCookies();
		String token = null;

		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if ("token".equals(cookie.getName())) {
					token = cookie.getValue();
					break;
				}
			}
		}

		JSONObject jsonResponse = new JSONObject();
		if (token != null && AuthService.getInstance().validateAndExtendToken(token) != null) {
			jsonResponse.put("authenticated", true);
		} else {
			jsonResponse.put("authenticated", false);
		}

		response.getWriter().write(jsonResponse.toJSONString());
	}
}
