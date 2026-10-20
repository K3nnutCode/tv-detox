package com.ansub.tvhome;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * 2026-10 official Brunei-Muara timetable (MoRA).
 * https://www.mora.gov.bn/Lists/WaktuSolat/AllItems.aspx
 * Fields: day, Subuh, Syuruk, Zohor, Asar, Maghrib, Isyak.
 * Other dates deliberately show Unavailable. No estimated prayer times.
 */
final class BruneiPrayerWidget extends LinearLayout {
    private static final TimeZone TZ = TimeZone.getTimeZone("Asia/Brunei");
    private static final String[] NAMES = {"Subuh", "Zohor", "Asar", "Maghrib", "Isyak"};
    private static final String[] DATA = {
      "", // index zero
      "04:51 06:08 12:11 15:23 18:12 19:21",
      "04:51 06:08 12:11 15:23 18:12 19:21",
      "04:50 06:07 12:10 15:23 18:11 19:20",
      "04:50 06:07 12:10 15:23 18:11 19:20",
      "04:50 06:07 12:10 15:23 18:11 19:20",
      "04:50 06:07 12:10 15:23 18:10 19:19",
      "04:50 06:07 12:09 15:23 18:10 19:19",
      "04:49 06:07 12:09 15:23 18:09 19:18",
      "04:49 06:06 12:09 15:23 18:09 19:18",
      "04:49 06:06 12:08 15:23 18:08 19:18",
      "04:49 06:06 12:08 15:24 18:08 19:17",
      "04:49 06:06 12:08 15:24 18:08 19:17",
      "04:48 06:06 12:08 15:24 18:07 19:17",
      "04:48 06:06 12:07 15:24 18:07 19:16",
      "04:48 06:06 12:07 15:24 18:07 19:16",
      "04:48 06:06 12:07 15:24 18:06 19:16",
      "04:48 06:05 12:07 15:24 18:06 19:16",
      "04:48 06:05 12:07 15:24 18:06 19:15",
      "04:47 06:05 12:06 15:24 18:05 19:15",
      "04:47 06:05 12:06 15:24 18:05 19:15",
      "04:47 06:05 12:06 15:24 18:05 19:15",
      "04:47 06:05 12:06 15:24 18:04 19:14",
      "04:47 06:05 12:06 15:24 18:04 19:14",
      "04:47 06:05 12:06 15:24 18:04 19:14",
      "04:47 06:05 12:05 15:24 18:04 19:14",
      "04:47 06:05 12:05 15:25 18:03 19:14",
      "04:46 06:05 12:05 15:25 18:03 19:14",
      "04:46 06:05 12:05 15:25 18:03 19:14",
      "04:46 06:05 12:05 15:25 18:03 19:13",
      "04:46 06:05 12:05 15:25 18:03 19:13",
      "04:46 06:05 12:05 15:25 18:02 19:13"
    };
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final TextView title, summary;
    private final TextView[] cells = new TextView[5];
    private boolean started = false;
    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            if (!started) return;
            render();
            handler.postDelayed(this, 30000);
        }
    };

    BruneiPrayerWidget(Context context, float scale) {
        super(context);
        setOrientation(VERTICAL);
        setPadding(px(22,scale),px(13,scale),px(22,scale),px(12,scale));
        setGravity(Gravity.CENTER_VERTICAL);
        title = label(context, "☪  WAKTU SOLAT  ·  BRUNEI–MUARA", px(25,scale), true, Color.WHITE);
        addView(title);
        summary = label(context, "Official timetable · 2026", px(19,scale), false, 0xFFE2C795);
        LinearLayout.LayoutParams ss = new LinearLayout.LayoutParams(-1,-2);
        ss.topMargin=px(4,scale);
        addView(summary,ss);
        LinearLayout row = new LinearLayout(context);
        row.setGravity(Gravity.CENTER);
        row.setOrientation(HORIZONTAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1,-2);
        rp.topMargin=px(9,scale);
        addView(row,rp);
        for(int i=0;i<5;i++){
            cells[i]=label(context, "", px(20,scale), false, Color.WHITE);
            cells[i].setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,px(63,scale),1f);
            p.leftMargin=px(2,scale); p.rightMargin=px(2,scale);
            row.addView(cells[i],p);
        }
        render();
    }
    void start(){ started=true; handler.removeCallbacks(refresh); handler.post(refresh); }
    void stop(){ started=false;handler.removeCallbacks(refresh); }
    private static int px(int a,float scale){return Math.round(a*scale);}
    private static TextView label(Context c,String str,int size,boolean bold,int color){
        TextView v=new TextView(c);v.setText(str);v.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,size);
        v.setTextColor(color);v.setIncludeFontPadding(false);
        if(bold)v.setTypeface(null,1);
        return v;
    }
    private static int minute(String time){
        String[] p=time.split(":");return Integer.parseInt(p[0])*60+Integer.parseInt(p[1]);
    }
    private static String remaining(int amount) {
        return amount/60+"h "+(amount%60)+"m";
    }
    private void render() {
        Calendar now=Calendar.getInstance(TZ);
        int year=now.get(Calendar.YEAR), month=now.get(Calendar.MONTH), day=now.get(Calendar.DAY_OF_MONTH);
        if(year!=2026 || month!=Calendar.OCTOBER || day<1 || day>=DATA.length){
            summary.setText("Official data unavailable for this date · no estimates");
            for(int i=0;i<5;i++){ cells[i].setText(NAMES[i]+"\n—");cells[i].setBackground(null);}
            return;
        }
        String[] clock=DATA[day].split(" ");
        if(clock.length!=6) {
            summary.setText("Timetable unavailable");
            return;
        }
        int[] mins=new int[5];
        mins[0]=minute(clock[0]);
        for(int i=1;i<5;i++)mins[i]=minute(clock[i+1]);
        int sunrise=minute(clock[1]);
        int current=now.get(Calendar.HOUR_OF_DAY)*60+now.get(Calendar.MINUTE);
        int active=-1;
        for(int i=4;i>=0;i--)if(current>=mins[i]){active=i;break;}
        if(active==0 && current>=sunrise)active=-1; // Between sunrise and Zohor.
        // After midnight the current time is BEFORE Subuh; do not claim Isyak
        // is the current prayer. It remains an overnight interval.
        int next=0;
        while(next<5 && current>=mins[next])next++;
        if(next==5){
            // Tomorrow's timetable needs validation; no invented countdown.
            summary.setText("After Isyak · Tomorrow's Subuh follows official timetable");
        }else{
            String state=active>=0 ? "Current: "+NAMES[active] : "Between prayers";
            summary.setText(state+"  ·  Next: "+NAMES[next]+" in "+remaining(mins[next]-current));
        }
        for(int i=0;i<5;i++){
            cells[i].setText(NAMES[i]+"\n"+clock[i==0?0:i+1]);
            if(i==active) {
                GradientDrawable fill=new GradientDrawable();
                fill.setColor(0xA471572A);fill.setCornerRadius(12);fill.setStroke(2,0xFFDEBF76);
                cells[i].setBackground(fill);
            } else cells[i].setBackground(null);
        }
    }
}
