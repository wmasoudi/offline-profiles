package com.walid.offlineprofiles;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class ProfileStore {
    private static final String FILE = "offline_profiles";
    private static final String PROFILES = "profiles";
    private static final String SCHEDULES = "schedules";
    private static final String NEXT_ID = "next_id";
    private static final String LAST_RESULT = "last_result";
    private static final String LAST_APPLIED_ID = "last_applied_id";
    private static final String TEMPORARY = "temporary_session";

    private ProfileStore() { }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    static synchronized List<Profile> profiles(Context context) {
        SharedPreferences p = prefs(context);
        if (!p.contains(PROFILES)) {
            List<Profile> defaults = new ArrayList<>();
            Profile normal = new Profile(1, "Normal");
            normal.ringerMode = AudioManager.RINGER_MODE_NORMAL;
            defaults.add(normal);
            Profile vibrate = new Profile(2, "Vibrate");
            vibrate.ringerMode = AudioManager.RINGER_MODE_VIBRATE;
            defaults.add(vibrate);
            Profile silent = new Profile(3, "Silent");
            silent.ringerMode = AudioManager.RINGER_MODE_SILENT;
            defaults.add(silent);
            saveProfiles(context, defaults);
            p.edit().putInt(NEXT_ID, 4).commit();
            return defaults;
        }
        List<Profile> result = new ArrayList<>();
        try {
            JSONArray values = new JSONArray(p.getString(PROFILES, "[]"));
            for (int i = 0; i < values.length(); i++) {
                JSONObject value = values.optJSONObject(i);
                if (value != null) result.add(Profile.fromJson(value));
            }
        } catch (JSONException ignored) { }
        return result;
    }

    static synchronized void saveProfiles(Context context, List<Profile> profiles) {
        JSONArray values = new JSONArray();
        try {
            for (Profile p : profiles) values.put(p.toJson());
            prefs(context).edit().putString(PROFILES, values.toString()).commit();
        } catch (JSONException e) {
            throw new IllegalStateException("Could not save profiles", e);
        }
    }

    static synchronized List<Schedule> schedules(Context context) {
        List<Schedule> result = new ArrayList<>();
        try {
            JSONArray values = new JSONArray(prefs(context).getString(SCHEDULES, "[]"));
            for (int i = 0; i < values.length(); i++) {
                JSONObject value = values.optJSONObject(i);
                if (value != null) result.add(Schedule.fromJson(value));
            }
        } catch (JSONException ignored) { }
        return result;
    }

    static synchronized void saveSchedules(Context context, List<Schedule> schedules) {
        JSONArray values = new JSONArray();
        try {
            for (Schedule s : schedules) values.put(s.toJson());
            prefs(context).edit().putString(SCHEDULES, values.toString()).commit();
        } catch (JSONException e) {
            throw new IllegalStateException("Could not save schedules", e);
        }
    }

    static synchronized int newId(Context context) {
        profiles(context);
        SharedPreferences p = prefs(context);
        int id = p.getInt(NEXT_ID, 4);
        p.edit().putInt(NEXT_ID, id + 1).commit();
        return id;
    }

    static Profile findProfile(Context context, int id) {
        for (Profile p : profiles(context)) if (p.id == id) return p;
        return null;
    }

    static void recordResult(Context context, String value) {
        prefs(context).edit().putString(LAST_RESULT, value).apply();
    }

    static String lastResult(Context context) {
        return prefs(context).getString(LAST_RESULT, "No profile applied yet.");
    }

    static int lastAppliedProfileId(Context context) {
        return prefs(context).getInt(LAST_APPLIED_ID, -1);
    }

    static void setLastAppliedProfileId(Context context, int id) {
        prefs(context).edit().putInt(LAST_APPLIED_ID, id).commit();
    }

    static TemporarySession temporary(Context context) {
        String saved = prefs(context).getString(TEMPORARY, null);
        if (saved == null) return null;
        try {
            return TemporarySession.fromJson(new JSONObject(saved));
        } catch (JSONException ignored) {
            return null;
        }
    }

    static void setTemporary(Context context, TemporarySession session) {
        if (session == null) {
            prefs(context).edit().remove(TEMPORARY).commit();
        } else {
            try {
                prefs(context).edit().putString(TEMPORARY, session.toJson().toString()).commit();
            } catch (JSONException e) {
                throw new IllegalStateException("Could not save temporary profile", e);
            }
        }
    }
}
