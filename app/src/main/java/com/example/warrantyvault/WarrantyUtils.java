package com.example.warrantyvault;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class WarrantyUtils {

    public static final String ACTIVE = "ACTIVE";
    public static final String EXPIRING = "EXPIRING SOON";
    public static final String EXPIRED = "EXPIRED";

    private static SimpleDateFormat fmt() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        sdf.setLenient(false);
        return sdf;
    }

    /** Days from today (midnight) until the expiry date. Negative = already expired. Null if unparsable. */
    public static Long daysLeft(String expiry) {
        if (expiry == null || expiry.isEmpty()) return null;
        try {
            Date d = fmt().parse(expiry);
            if (d == null) return null;
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);
            long diff = d.getTime() - today.getTimeInMillis();
            return Math.floorDiv(diff, 24L * 60 * 60 * 1000);
        } catch (ParseException e) {
            return null;
        }
    }

    public static String computeStatus(String expiry) {
        Long days = daysLeft(expiry);
        if (days == null) return ACTIVE;
        if (days < 0) return EXPIRED;
        if (days <= 30) return EXPIRING;
        return ACTIVE;
    }

    public static String calculateExpiry(String purchaseDate, int months) {
        SimpleDateFormat sdf = fmt();
        try {
            Date date = sdf.parse(purchaseDate);
            Calendar c = Calendar.getInstance();
            if (date != null) c.setTime(date);
            c.add(Calendar.MONTH, months);
            return sdf.format(c.getTime());
        } catch (ParseException e) {
            return purchaseDate;
        }
    }

    /** Human friendly line such as "Expires in 12 days" / "Expired 3 days ago". */
    public static String describe(String expiry) {
        Long d = daysLeft(expiry);
        if (d == null) return "";
        if (d < 0) return "Expired " + (-d) + (d == -1 ? " day ago" : " days ago");
        if (d == 0) return "Expires today";
        if (d < 60) return "Expires in " + d + (d == 1 ? " day" : " days");
        long months = d / 30;
        return "Expires in ~" + months + " months";
    }
}
