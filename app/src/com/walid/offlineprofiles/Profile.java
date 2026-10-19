package com.walid.offlineprofiles;

import android.content.Context;
import android.media.AudioManager;
import org.json.JSONException;
import org.json.JSONObject;

final class Profile {
    int id;
    String name;
    int ringerMode = -1;
    int ring = -1;
    int notification = -1;
    int media = -1;
    int alarm = -1;
    int system = -1;

    Profile(int id, String name) {
        this.id = id;
        this.name = name;
    }

    JSONObject toJson() throws JSONException {
        JSONObject value = new JSONObject();
        value.put("id", id);
        value.put("name", name);
        value.put("ringerMode", ringerMode);
        value.put("ring", ring);
        value.put("notification", notification);
        value.put("media", media);
        value.put("alarm", alarm);
        value.put("system", system);
        return value;
    }

    static Profile fromJson(JSONObject value) {
        Profile p = new Profile(value.optInt("id", 0), value.optString("name", "Profile"));
        p.ringerMode = value.optInt("ringerMode", -1);
        p.ring = value.optInt("ring", -1);
        p.notification = value.optInt("notification", -1);
        p.media = value.optInt("media", -1);
        p.alarm = value.optInt("alarm", -1);
        p.system = value.optInt("system", -1);
        return p;
    }

    Profile copy() {
        Profile p = new Profile(id, name);
        p.ringerMode = ringerMode;
        p.ring = ring;
        p.notification = notification;
        p.media = media;
        p.alarm = alarm;
        p.system = system;
        return p;
    }

    static Profile capture(Context context, int id) {
        Profile p = new Profile(id, "New profile");
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audio != null) {
            p.ringerMode = audio.getRingerMode();
            p.ring = percent(audio, AudioManager.STREAM_RING);
            p.notification = percent(audio, AudioManager.STREAM_NOTIFICATION);
            p.media = percent(audio, AudioManager.STREAM_MUSIC);
            p.alarm = percent(audio, AudioManager.STREAM_ALARM);
            p.system = percent(audio, AudioManager.STREAM_SYSTEM);
        }
        return p;
    }

    private static int percent(AudioManager audio, int stream) {
        int max = audio.getStreamMaxVolume(stream);
        return max > 0 ? Math.round(100f * audio.getStreamVolume(stream) / max) : -1;
    }

    String summary() {
        String mode = ringerMode == AudioManager.RINGER_MODE_NORMAL ? "Normal" :
                ringerMode == AudioManager.RINGER_MODE_VIBRATE ? "Vibrate" :
                ringerMode == AudioManager.RINGER_MODE_SILENT ? "Silent" : "Keep sound mode";
        StringBuilder text = new StringBuilder(mode);
        if (ring >= 0) text.append(" · Ring ").append(ring).append('%');
        if (notification >= 0) text.append(" · Notification ").append(notification).append('%');
        if (media >= 0) text.append(" · Media ").append(media).append('%');
        if (alarm >= 0) text.append(" · Alarm ").append(alarm).append('%');
        if (system >= 0) text.append(" · System ").append(system).append('%');
        return text.toString();
    }
}
