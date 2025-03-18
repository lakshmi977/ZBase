package Util;

import Model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import org.mindrot.jbcrypt.BCrypt;

import Controller.UserDataHandler;

import java.awt.RenderingHints.Key;
import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

public class AuthService {
	private static final String FILE_NAME = "/home/naga-zstk392/ZBase/Users_Metadata.db";
	private static AuthService instance;
	private static final String SECRET = "uLD6znrRh/pz1+pnrgcuSgvNG5rNReeuRBeh+EydeR8=";
	private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
	private static final long EXPIRATION_TIME = 86400000; // 1 day (24 hours)
	private static final long RENEW_THRESHOLD = 10 * 60 * 1000 * 12; // 2 hours

	private AuthService() {
	}

	public static synchronized AuthService getInstance() {
		if (instance == null) {
			instance = new AuthService();
		}
		return instance;
	}

	public synchronized void signUp(String username, String email, String password) throws IOException {
		UserDataHandler.saveUser(username, email, password);
	}

	@SuppressWarnings("unchecked")
	private synchronized List<String[]> readAllUsers() {
		File file = new File(FILE_NAME);
		if (!file.exists())
			return new ArrayList<>();

		try (FileInputStream fis = new FileInputStream(FILE_NAME); ObjectInputStream ois = new ObjectInputStream(fis)) {
			return (List<String[]>) ois.readObject();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
			return new ArrayList<>();
		}
	}

	public boolean checkUser(String email, String password) {
		List<String[]> users = readAllUsers();
		for (String[] user : users) {
			if (user[1].equals(email) && BCrypt.checkpw(password, user[2])) {
				return true;
			}
		}
		return false;
	}

	public User getUser(String email) {
		List<String[]> users = readAllUsers();
		for (String[] user : users) {
			if (user[1].equals(email)) {
				return new User(user[0], user[1], user[2]);
			}
		}
		return null;
	}

	public User getUserFromToken(String token) {
		if (token == null || !token.startsWith("MYDB_")) {
			return null; // Invalid token
		}
		 try {
		        // ✅ Extract actual JWT (remove "MYDB_")
		        token = token.substring(5);

		        // ✅ Parse JWT to extract claims
		        Claims claims = Jwts.parserBuilder()
		                .setSigningKey(SECRET_KEY)
		                .build()
		                .parseClaimsJws(token)
		                .getBody();

		        // ✅ Extract the subject (which is the username)
		        String username = claims.getSubject();

		        // ✅ Find user by username
		        List<String[]> users = readAllUsers();
		        for (String[] user : users) {
		            if (user[0].equals(username)) { // Assuming user[0] is the username
		                return new User(user[0], user[1], user[2]); // ✅ Return User object
		            }
		        }
		    } catch (ExpiredJwtException e) {
		        System.out.println("Token expired.");
		    } catch (SignatureException e) {
		        System.out.println("Invalid token signature.");
		    } catch (Exception e) {
		        System.out.println("Error parsing token: " + e.getMessage());
		    }

		    return null;

	}

	// ✅ Login logic
	public User login(String email, String password) {
		if (UserDataHandler.checkUser(email, password)) {
			return UserDataHandler.getUser(email);
		}
		return null;
	}

	public static String generateToken(String username) {
		System.out.println( "MYDB_" + Jwts.builder().setSubject(username).setIssuedAt(new Date())
				.setExpiration(new Date(System.currentTimeMillis() +EXPIRATION_TIME)) // 1 hour expiry
				.signWith(SECRET_KEY) // ✅ Properly signing with SecretKey
				.compact()  +"+++++++++++++++++++++++++++++++++++++++++++");
		return "MYDB_" + Jwts.builder().setSubject(username).setIssuedAt(new Date())
				.setExpiration(new Date(System.currentTimeMillis() +EXPIRATION_TIME)) // 1 hour expiry
				.signWith(SECRET_KEY) // ✅ Properly signing with SecretKey
				.compact();
	}

	public static String validateAndExtendToken(String token) {
	    if (token.startsWith("MYDB_")) {
	        token = token.substring(5); // Remove "MYDB_" prefix
	    }
	    try {
	        Claims claims = Jwts.parserBuilder()
	            .setSigningKey(SECRET_KEY)
	            .build()
	            .parseClaimsJws(token)
	            .getBody();

	        String username = claims.getSubject();
	        Date expiration = claims.getExpiration();
	        long remainingTime = expiration.getTime() - System.currentTimeMillis();

	        if (remainingTime < RENEW_THRESHOLD) {
	            return generateToken(username);
	        }

	        return token;
	    } catch (ExpiredJwtException e) {
	       System.err.println("Token expired, please log in again.");
	       return "";
	    } catch (SignatureException e) {
	        throw new RuntimeException("Invalid token.");
	    } catch (Exception e) {
	        throw new RuntimeException("Malformed Token: " + e.getMessage());
	    }
	}


	public static String getTokenFromCookies(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if ("token".equals(cookie.getName())) { // ✅ Get token from 'token' cookie
					return cookie.getValue();
				}
			}
		}
		return null; // ❌ No token found
	}

}
