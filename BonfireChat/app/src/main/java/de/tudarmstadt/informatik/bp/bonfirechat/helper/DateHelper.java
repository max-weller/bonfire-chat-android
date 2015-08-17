package de.tudarmstadt.informatik.bp.bonfirechat.helper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by mw on 22.05.15.
 */
public class DateHelper {

    public static String getNowString() {
        return formatTime(new Date());
    }

    public static String formatTime(Date date) {
        return new SimpleDateFormat("HH:mm:ss").format(date);
    }
    public static String formatDateTime(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }
    public static Date parseDateTime(String string) throws ParseException {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(string);
    }

    public static String formatTimeDelta(Date start, Date end) {
        long interval = end.getTime() - start.getTime();
        long seconds = TimeUnit.SECONDS.convert(interval, TimeUnit.MILLISECONDS);
        return new StringBuilder().append("+ ").append(seconds).append("s").toString();
    }

}
