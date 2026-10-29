/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package duplexx_gym;


public class UserSession {
      private static int userId;
    private static String username;
    
     public static void setUser(int id, String user) {
        userId = id;
        username = user;
    }

    public static int getUserId() {
        return userId;
    }

    public static String getUsername() {
        return username;
    }

    public static void clearSession() {
        userId = 0;
        username = null;
    }
}

