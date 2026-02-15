package Client.controller;

/**
 * SessionData — static holder for the currently logged-in user.
 * Set once at login, read by all dashboard controllers.
 */
public class SessionData {

    private static String username = "";
    private static String role     = "";

    public static String getUsername() { return username; }
    public static String getRole()     { return role;     }

    public static void setUsername(String u) { username = u; }
    public static void setRole(String r)     { role = r;     }

    public static void clear() {
        username = "";
        role     = "";
    }
}