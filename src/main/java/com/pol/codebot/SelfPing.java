package com.pol.codebot;


import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SelfPing extends TimerTask {
    public static String url = "https://codebot123.herokuapp.com/";

    public SelfPing() {

    }

    public void run() {
        String server1 = "https://codebot123.herokuapp.com/";
        String server2 = "https://codebot122.herokuapp.com/";
        Logger logger = Logger.getLogger(SelfPing.class.getName());
        if (testUrlNotWorking(server1)) {
            logger.log(Level.INFO, String.format("%s is offline", server1));
            url = server2;
        }
        if (testUrlNotWorking(server2)) {
            logger.log(Level.INFO, String.format("%s is offline", server2));
            url = server1;
        }
    }

    private static boolean testUrlNotWorking(String urlString) {
        try {
            URL url = new URL(urlString);
            URLConnection urlcon = url.openConnection();
            urlcon.setRequestProperty("User-Agent", "java:com.pol.codebot:v1.0.0");
            urlcon.getInputStream();
            return false;
        } catch (IOException e) {
            return true;
        }
    }
}
