package Filter;

import java.io.IOException;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.*;
import org.json.simple.JSONObject;
import Util.AuthService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;

@WebFilter("/Service/*") // Apply to all requests under /Service/
public class AuthFilter implements Filter {

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        // ✅ Handle CORS
        res.setHeader("Access-Control-Allow-Origin", req.getHeader("Origin"));
        res.setHeader("Access-Control-Allow-Credentials", "true");
        res.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        res.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");

        // ✅ Allow Preflight Requests
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            res.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        // ✅ Extract Token from Cookie
        String token = null;
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("token".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        // ✅ Validate Token
        if (token == null) {
            sendUnauthorizedResponse(res, "Missing authentication token");
            return;
        }

        try {
            String username = AuthService.validateAndExtendToken(token);
            req.setAttribute("username", username); // Store user info for later use
            chain.doFilter(request, response); // Continue request processing
        } catch (ExpiredJwtException e) {
            sendUnauthorizedResponse(res, "Token expired, please log in again.");
        } catch (JwtException e) {
            sendUnauthorizedResponse(res, "Invalid token.");
        }
    }

    // ✅ Helper Method: Send Unauthorized Response
    private void sendUnauthorizedResponse(HttpServletResponse res, String message) throws IOException {
        res.setContentType("application/json");
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        JSONObject json = new JSONObject();
        json.put("error", message);
        res.getWriter().print(json.toJSONString());
    }

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		// TODO Auto-generated method stub
		
	}
}
