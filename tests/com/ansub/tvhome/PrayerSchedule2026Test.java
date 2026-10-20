package com.ansub.tvhome;

import java.util.Calendar;

/** Offline source validation: fail the build rather than ship missing/guessed dates. */
public final class PrayerSchedule2026Test {
    private static void check(boolean ok, String description) {
        if (!ok) throw new AssertionError(description);
    }

    private static void expect(int month, int day, String... expected) {
        int[] times = PrayerSchedule2026.get(2026, month, day);
        check(times != null && times.length == expected.length, "Missing " + month + "/" + day);
        for (int i = 0; i < times.length; i++) {
            check(PrayerSchedule2026.format(times[i]).equals(expected[i]),
                    month + "/" + day + " index " + i + ": got " + PrayerSchedule2026.format(times[i])
                    + " expected " + expected[i]);
        }
    }

    public static void main(String[] args) {
        check("Asia/Brunei".equals(PrayerSchedule2026.ZONE.getID()), "Brunei timezone");
        // Exact printed PDI 2026 October table:
        expect(10, 1, "04:51", "06:08", "12:11", "15:23", "18:12", "19:21");
        expect(10, 21, "04:47", "06:05", "12:06", "15:24", "18:05", "19:15");
        expect(10, 31, "04:46", "06:05", "12:05", "15:25", "18:02", "19:13");
        // Exact printed PDI November table:
        expect(11, 1, "04:46", "06:05", "12:05", "15:25", "18:02", "19:13");
        expect(11, 15, "04:47", "06:08", "12:06", "15:27", "18:02", "19:14");
        expect(11, 30, "04:50", "06:13", "12:10", "15:32", "18:05", "19:18");
        // Exact printed PDI December table:
        expect(12, 1, "04:51", "06:13", "12:10", "15:32", "18:05", "19:19");
        expect(12, 21, "04:59", "06:23", "12:19", "15:41", "18:13", "19:28");
        expect(12, 31, "05:04", "06:28", "12:24", "15:46", "18:18", "19:33");

        // Every Oct-Dec date must exist; all prayers must remain in strict order.
        int count = 0;
        for (int month = 10; month <= 12; month++) {
            int days = month == 11 ? 30 : 31;
            for (int day = 1; day <= days; day++) {
                int[] t = PrayerSchedule2026.get(2026, month, day);
                check(t != null, "Missing verified row " + month + "/" + day);
                check(t.length == 6, "Invalid row size " + month + "/" + day);
                check(t[0] >= 270 && t[0] <= 330, "Implausible Subuh");
                check(t[1] >= 350 && t[1] <= 430, "Implausible Syuruk");
                check(t[2] >= 710 && t[2] <= 760, "Implausible Zuhur");
                for (int i = 1; i < t.length; i++) {
                    check(t[i] > t[i - 1], "Times out of order " + month + "/" + day);
                }
                count++;
            }
        }
        check(count == 92, "Expected 92 verified rows");
        check(PrayerSchedule2026.get(2026, 9, 30) == null, "No unverified Sept data");
        check(PrayerSchedule2026.get(2026, 10, 32) == null, "Reject invalid Oct date");
        check(PrayerSchedule2026.get(2026, 11, 31) == null, "Reject invalid Nov date");
        check(PrayerSchedule2026.get(2027, 1, 1) == null, "No fabricated 2027 dates");

        Calendar brunei = Calendar.getInstance(PrayerSchedule2026.ZONE);
        brunei.clear();
        brunei.set(2026, Calendar.OCTOBER, 31, 23, 50, 0);
        Calendar tomorrow = PrayerSchedule2026.nextDay(brunei);
        check(tomorrow.get(Calendar.MONTH) == Calendar.NOVEMBER
                && tomorrow.get(Calendar.DAY_OF_MONTH) == 1, "Oct 31 rollover");
        check(PrayerSchedule2026.get(tomorrow)[0] == 286, "Tomorrow next Subuh 04:46");
        System.out.println("PASS: 92 Oct-Dec 2026 verified entries, source dates, timezone, midnight rollover and unavailable-range guard");
    }
}
