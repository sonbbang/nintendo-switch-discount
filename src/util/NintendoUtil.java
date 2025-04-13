package util;


import model.GameInfo;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.regex.Pattern;

public class NintendoUtil {

    public static String isSupportedKorea(String l) {
        return l.contains("Korea") ? "O" : "X";
    }


    static int monthEnglishToInteger(String monthEnglish) {

        int month = 0;

        if ("Jan".equals(monthEnglish) || "January".equals(monthEnglish)) {
            month = 1;
        } else if ("Feb".equals(monthEnglish) || "February".equals(monthEnglish)) {
            month = 2;
        } else if ("Mar".equals(monthEnglish) || "March".equals(monthEnglish)) {
            month = 3;
        } else if ("Apr".equals(monthEnglish) || "April".equals(monthEnglish)) {
            month = 4;
        } else if ("May".equals(monthEnglish)) {
            month = 5;
        } else if ("Jun".equals(monthEnglish) || "June".equals(monthEnglish)) {
            month = 6;
        } else if ("Jul".equals(monthEnglish) || "July".equals(monthEnglish)) {
            month = 7;
        } else if ("Aug".equals(monthEnglish) || "August".equals(monthEnglish)) {
            month = 8;
        } else if ("Sep".equals(monthEnglish) || "September".equals(monthEnglish)) {
            month = 9;
        } else if ("Oct".equals(monthEnglish) || "October".equals(monthEnglish)) {
            month = 10;
        } else if ("Nov".equals(monthEnglish) || "November".equals(monthEnglish)) {
            month = 11;
        } else if ("Dec".equals(monthEnglish) || "December".equals(monthEnglish)) {
            month = 12;
        }
        return month;
    }

    public static String imageTagMax(String imageUrl, String width, String height) {
        return "<img src=\"" + imageUrl + "\" width=\"" + width + "\" height=\"" + height
                + "\" alt=\"gameScreenShot\" style = \"max-width: 100%\">";
    }


    public static String discountPercentColoring(Integer discountPercent) {
        String tag = null;

        if (discountPercent != null) {
            tag = Integer.toString(discountPercent);

            // 빨강
            if (discountPercent >= 75) {
                tag = "<span style=\"color: #ee2323;\">" + discountPercent + "</span>";
            } else if (discountPercent >= 25) { // 주황
                tag = "<span style=\"color: #f89009;\">" + discountPercent + "</span>";
            } else { // 그린
                tag = "<span style=\"color: #409d00;\">" + discountPercent + "</span>";
            }
        }
        return tag;
    }

    private static Pattern pattern = Pattern.compile("-?\\d+(\\.\\d+)?");

}
