package common;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeUtil {
    //returns current timestamp in IST
    public static String getCurrentTimestamp()
    {
        ZonedDateTime time = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
        System.out.println("Time returning: "+time);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
        String formattedIst = time.format(formatter);
        System.out.println("Formatted IST Time: " + formattedIst);
        return formattedIst;
    }
}
