package com.walid.offlineprofiles;

import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioManager;

final class ProfileApplier {
    private ProfileApplier() { }

    static String apply(Context context, Profile profile) {
        if (profile == null) return "The selected profile no longer exists.";
        StringBuilder result = new StringBuilder("Profile: ").append(profile.name);
        int applied = 0;
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audio != null) {
            applied += volume(audio, AudioManager.STREAM_RING, profile.ring, "ring", result);
            applied += volume(audio, AudioManager.STREAM_NOTIFICATION, profile.notification,
                    "notification", result);
            applied += volume(audio, AudioManager.STREAM_MUSIC, profile.media, "media", result);
            applied += volume(audio, AudioManager.STREAM_ALARM, profile.alarm, "alarm", result);
            applied += volume(audio, AudioManager.STREAM_SYSTEM, profile.system, "system", result);
            if (profile.ringerMode >= 0) {
                try {
                    NotificationManager manager = (NotificationManager) context.getSystemService(
                            Context.NOTIFICATION_SERVICE);
                    if ((profile.ringerMode == AudioManager.RINGER_MODE_SILENT ||
                            audio.getRingerMode() == AudioManager.RINGER_MODE_SILENT) &&
                            (manager == null || !manager.isNotificationPolicyAccessGranted())) {
                        result.append("\nSound mode needs Do Not Disturb access.");
                    } else {
                        audio.setRingerMode(profile.ringerMode);
                        applied++;
                    }
                } catch (SecurityException | IllegalArgumentException e) {
                    result.append("\nSound mode was blocked by Android.");
                }
            }
        } else {
            result.append("\nAudio service is unavailable.");
        }

        result.append("\nSettings applied: ").append(applied);
        String text = result.toString();
        ProfileStore.recordResult(context, text);
        if (profile.id > 0) ProfileStore.setLastAppliedProfileId(context, profile.id);
        return text;
    }

    private static int volume(AudioManager audio, int stream, int percent,
            String label, StringBuilder result) {
        if (percent < 0) return 0;
        try {
            int max = audio.getStreamMaxVolume(stream);
            int value = Math.max(0, Math.min(max, Math.round(max * percent / 100f)));
            audio.setStreamVolume(stream, value, 0);
            return 1;
        } catch (SecurityException | IllegalArgumentException e) {
            result.append("\n").append(label).append(" volume was blocked by Android.");
            return 0;
        }
    }
}
