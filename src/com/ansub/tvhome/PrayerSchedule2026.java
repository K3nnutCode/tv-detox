package com.ansub.tvhome;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Brunei-Muara 2026, five daily prayer times + sunrise (Syuruk).
 * OFFLINE, immutable, exact times; no astronomy calculations or geo-location.
 *
 * Sources:
 *  - Pusat Da'wah Islamiah (PDI), Ministry of Religious Affairs,
 *    TAQWIM 2026 ©PDI2025 (October, November, December 2026 printed tables)
 *  - https://www.survey.gov.bn/jadual-waktu-sembahyang/ (official survey listing)
 *  - https://www.mora.gov.bn/SitePages/WaktuSembahyang.aspx
 *
 * Covered range in this initial verified build: 2026-10-01 through
 * 2026-12-31 ONLY. Outside this range get() returns null, never a fabricated
 * prayer time. Extend ONLY after original published tables are checked.
 *
 * Each row: Subuh Syuruk Zuhur Asar Maghrib Isyak in local HHmm, Brunei +08:00.
 */
final class PrayerSchedule2026 {
    static final TimeZone ZONE = TimeZone.getTimeZone("Asia/Brunei");
    static final String[] PRAYERS = {"Subuh", "Zuhur", "Asar", "Maghrib", "Isyak"};

    private static final String[] OCT = {
            "0451 0608 1211 1523 1812 1921",
            "0451 0608 1211 1523 1812 1921",
            "0450 0607 1210 1523 1811 1920",
            "0450 0607 1210 1523 1811 1920",
            "0450 0607 1210 1523 1811 1920",
            "0450 0607 1210 1523 1810 1919",
            "0450 0607 1209 1523 1810 1919",
            "0449 0607 1209 1523 1809 1918",
            "0449 0606 1209 1523 1809 1918",
            "0449 0606 1208 1523 1808 1918",
            "0449 0606 1208 1524 1808 1917",
            "0449 0606 1208 1524 1808 1917",
            "0448 0606 1208 1524 1807 1917",
            "0448 0606 1207 1524 1807 1916",
            "0448 0606 1207 1524 1807 1916",
            "0448 0606 1207 1524 1806 1916",
            "0448 0605 1207 1524 1806 1916",
            "0448 0605 1207 1524 1806 1915",
            "0447 0605 1206 1524 1805 1915",
            "0447 0605 1206 1524 1805 1915",
            "0447 0605 1206 1524 1805 1915",
            "0447 0605 1206 1524 1804 1914",
            "0447 0605 1206 1524 1804 1914",
            "0447 0605 1206 1524 1804 1914",
            "0447 0605 1205 1524 1804 1914",
            "0447 0605 1205 1525 1803 1914",
            "0446 0605 1205 1525 1803 1914",
            "0446 0605 1205 1525 1803 1914",
            "0446 0605 1205 1525 1803 1913",
            "0446 0605 1205 1525 1803 1913",
            "0446 0605 1205 1525 1802 1913",
    };
    private static final String[] NOV = {
            "0446 0605 1205 1525 1802 1913",
            "0446 0606 1205 1525 1802 1913",
            "0446 0606 1205 1525 1802 1913",
            "0446 0606 1205 1525 1802 1913",
            "0446 0606 1205 1526 1802 1913",
            "0446 0606 1205 1526 1802 1913",
            "0446 0606 1205 1526 1802 1913",
            "0446 0606 1205 1526 1802 1913",
            "0446 0606 1205 1526 1802 1913",
            "0446 0607 1205 1526 1802 1914",
            "0446 0607 1205 1526 1802 1914",
            "0447 0607 1205 1527 1802 1914",
            "0447 0607 1206 1527 1802 1914",
            "0447 0608 1206 1527 1802 1914",
            "0447 0608 1206 1527 1802 1914",
            "0447 0608 1206 1527 1802 1914",
            "0447 0608 1206 1528 1802 1915",
            "0447 0609 1206 1528 1802 1915",
            "0448 0609 1207 1528 1802 1915",
            "0448 0609 1207 1528 1802 1915",
            "0448 0610 1207 1529 1802 1916",
            "0448 0610 1207 1529 1803 1916",
            "0448 0610 1208 1529 1803 1916",
            "0449 0611 1208 1530 1803 1916",
            "0449 0611 1208 1530 1803 1917",
            "0449 0611 1208 1530 1803 1917",
            "0449 0612 1209 1531 1804 1917",
            "0450 0612 1209 1531 1804 1918",
            "0450 0612 1209 1531 1804 1918",
            "0450 0613 1210 1532 1805 1918",
    };
    private static final String[] DEC = {
            "0451 0613 1210 1532 1805 1919",
            "0451 0614 1211 1532 1805 1919",
            "0451 0614 1211 1533 1806 1920",
            "0452 0615 1211 1533 1806 1920",
            "0452 0615 1212 1534 1806 1921",
            "0453 0616 1212 1534 1807 1921",
            "0453 0616 1213 1534 1807 1921",
            "0453 0616 1213 1535 1807 1922",
            "0454 0617 1213 1535 1808 1922",
            "0454 0617 1214 1536 1808 1923",
            "0455 0618 1214 1536 1809 1923",
            "0455 0618 1215 1537 1809 1924",
            "0455 0619 1215 1537 1809 1924",
            "0456 0619 1216 1538 1810 1925",
            "0456 0620 1216 1538 1810 1925",
            "0457 0620 1217 1539 1811 1926",
            "0457 0621 1217 1539 1811 1926",
            "0458 0621 1218 1539 1812 1927",
            "0458 0622 1218 1540 1812 1927",
            "0459 0622 1219 1540 1813 1928",
            "0459 0623 1219 1541 1813 1928",
            "0500 0623 1220 1541 1814 1929",
            "0500 0624 1220 1542 1814 1929",
            "0501 0624 1221 1542 1815 1930",
            "0501 0625 1221 1543 1815 1930",
            "0502 0625 1222 1543 1816 1931",
            "0502 0626 1222 1544 1816 1931",
            "0503 0626 1223 1544 1817 1932",
            "0503 0627 1223 1545 1817 1932",
            "0504 0627 1224 1545 1818 1933",
            "0504 0628 1224 1546 1818 1933",
    };

    /** Local wall clock minutes after midnight: Subuh, Syuruk, Zuhur, Asar, Maghrib, Isyak. */
    static int[] get(int year, int month, int day) {
        if (year != 2026 || day < 1) return null;
        String[] src = month == 10 ? OCT : month == 11 ? NOV : month == 12 ? DEC : null;
        if (src == null || day > src.length) return null;
        String[] hhmm = src[day - 1].split(" ");
        if (hhmm.length != 6) return null;
        int[] result = new int[6];
        for (int i = 0; i < 6; i++) {
            String v = hhmm[i];
            if (v.length() != 4) return null;
            try {
                int hour = Integer.parseInt(v.substring(0, 2));
                int minute = Integer.parseInt(v.substring(2, 4));
                if (hour > 23 || minute > 59) return null;
                result[i] = hour * 60 + minute;
                if (i > 0 && result[i] <= result[i - 1]) return null;
            } catch (NumberFormatException e) { return null; }
        }
        return result;
    }

    static int[] get(Calendar brunei) {
        return get(brunei.get(Calendar.YEAR), brunei.get(Calendar.MONTH) + 1,
                brunei.get(Calendar.DAY_OF_MONTH));
    }

    static Calendar nextDay(Calendar brunei) {
        Calendar next = (Calendar) brunei.clone();
        next.set(Calendar.HOUR_OF_DAY, 12);
        next.set(Calendar.MINUTE, 0);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        next.add(Calendar.DAY_OF_MONTH, 1);
        return next;
    }

    static String format(int minutes) {
        return String.format(java.util.Locale.US, "%02d:%02d", minutes / 60, minutes % 60);
    }

    private PrayerSchedule2026() {}
}
