package Util;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;


import Model.User;

public class SessionUtil {

	public static User getLoggedInUser(HttpServletRequest req) {
	    Cookie[] cookies = req.getCookies();
	    if (cookies != null) {
	        for (Cookie cookie : cookies) {
	            if ("token".equals(cookie.getName())) {
	                return AuthService.getInstance().getUserFromToken(cookie.getValue());
	            }
	        }
	    }
	    return null; // No user found
	}

    
}
