package com.walid.offlineprofiles;

import java.util.Calendar;
import org.json.JSONException;
import org.json.JSONObject;

final class Schedule {
    int id;
    int profileId;
    int hour;
    int minute;
    int days = 127;
    boolean enabled = true;

    Schedule(int id, int profileId, int hour, int minute) {
        this.id = id;
        this.profileId = profileId;
        this.hour = hour;
        this.minute = minute;
    }

    JSONObject toJson() throws JSONException {
        JSONObject value = new JSONObject();
        value.put("id", id);
        value.put("profileId", profileId);
        value.put("hour", hour);
        value.put("minute", minute);
        value.put("days", days);
        value.put("enabled", enabled);
        return value;
    }

    static Schedule fromJson(JSONObject value) {
        Schedule s = new Schedule(value.optInt("id"), value.optInt("profileId"),
                value.optInt("hour"), value.optInt("minute"));
        s.days = value.optInt("days", 127);
        s.enabled = value.optBoolean("enabled", true);
        return s;
    }

    long nextTimeMillis(long nowMillis) {
        if (!enabled || days == 0) return -1;
        Calendar now = Calendar.getInstance();
        now.setTimeInMillis(nowMillis);
        for (int offset = 0; offset < 8; offset++) {
            Calendar candidate = (Calendar) now.clone();
            candidate.add(Calendar.DAY_OF_YEAR, offset);
            candidate.set(Calendar.HOUR_OF_DAY, hour);
            candidate.set(Calendar.MINUTE, minute);
            candidate.set(Calendar.SECOND, 0);
            candidate.set(Calendar.MILLISECOND, 0);
            int dayBit = 1 << (candidate.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY);
            if ((days & dayBit) != 0 && candidate.getTimeInMillis() > nowMillis) {
                return candidate.getTimeInMillis();
            }
        }
        return -1;
    }

    String daysText() {
        if (days == 127) return "Every day";
        String[] names = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            if ((days & (1 << i)) != 0) {
                if (text.length() > 0) text.append(' ');
                text.append(names[i]);
            }
        }
        return text.toString();
    }
}
