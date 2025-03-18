package Util;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// Store blacklisted tokens
public class TokenBlacklist {
    private static final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();

    public static void add(String token) {
        blacklistedTokens.add(token);
    }

    public static boolean isBlacklisted(String token) {
        return blacklistedTokens.contains(token);
    }
}
