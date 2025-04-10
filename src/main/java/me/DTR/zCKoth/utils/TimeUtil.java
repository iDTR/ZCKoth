package me.DTR.zCKoth.utils;

public class TimeUtil {

    public static String formatTime(int seconds) {
        if (seconds < 0) {
            return "0s";
        }

        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;

        if (minutes == 0) {
            return remainingSeconds + "s";
        } else {
            return minutes + "m " + remainingSeconds + "s";
        }
    }

    public static String formatTimeHMS(int seconds) {
        if (seconds < 0) {
            return "0s";
        }

        int hours = seconds / 3600;
        int remainingSeconds = seconds % 3600;
        int minutes = remainingSeconds / 60;
        remainingSeconds = remainingSeconds % 60;

        StringBuilder sb = new StringBuilder();

        if (hours > 0) {
            sb.append(hours).append("h ");
        }

        if (minutes > 0 || hours > 0) {
            sb.append(minutes).append("m ");
        }

        sb.append(remainingSeconds).append("s");

        return sb.toString();
    }
}