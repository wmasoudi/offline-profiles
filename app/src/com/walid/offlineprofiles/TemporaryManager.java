package com.walid.offlineprofiles;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

final class TemporaryManager {
    static final String ACTION_RESTORE = "com.walid.offlineprofiles.RESTORE_TEMPORARY";

    private TemporaryManager() { }

    static synchronized String start(Context context, Profile target, long untilMillis) {
        reconcile(context);
        if (target == null || untilMillis <= System.currentTimeMillis()) {
            return "Choose a future end time.";
        }
        TemporarySession old = ProfileStore.temporary(context);
        int previousId;
        Profile previousSound;
        if (old != null) {
            previousId = old.previousProfileId;
            previousSound = old.previousSound;
        } else {
            previousId = ProfileStore.lastAppliedProfileId(context);
            previousSound = Profile.capture(context, -1);
            previousSound.name = "previous sound settings";
        }
        cancelAlarm(context);
        ProfileStore.setTemporary(context, new TemporarySession(target.id, previousId,
                untilMillis, previousSound));
        String result = ProfileApplier.apply(context, target);
        scheduleReturn(context);
        return result;
    }

    static synchronized void cancel(Context context) {
        cancelAlarm(context);
        ProfileStore.setTemporary(context, null);
    }

    static synchronized void reconcile(Context context) {
        TemporarySession session = ProfileStore.temporary(context);
        if (session == null) {
            cancelAlarm(context);
        } else if (session.untilMillis <= System.currentTimeMillis()) {
            finish(context);
        } else {
            scheduleReturn(context);
        }
    }

    static synchronized boolean isActive(Context context) {
        TemporarySession session = ProfileStore.temporary(context);
        return session != null && session.untilMillis > System.currentTimeMillis();
    }

    static synchronized void finish(Context context) {
        TemporarySession session = ProfileStore.temporary(context);
        if (session == null) return;
        if (session.untilMillis > System.currentTimeMillis()) {
            scheduleReturn(context);
            return;
        }
        cancelAlarm(context);
        ProfileStore.setTemporary(context, null);
        String result = ProfileApplier.apply(context, session.previousSound);
        int previousId = ProfileStore.findProfile(context, session.previousProfileId) == null ?
                -1 : session.previousProfileId;
        ProfileStore.setLastAppliedProfileId(context, previousId);
        ProfileStore.recordResult(context, "Temporary profile ended. " + result);
    }

    static synchronized void restoreNow(Context context) {
        TemporarySession session = ProfileStore.temporary(context);
        if (session == null) return;
        cancelAlarm(context);
        ProfileStore.setTemporary(context, null);
        String result = ProfileApplier.apply(context, session.previousSound);
        int previousId = ProfileStore.findProfile(context, session.previousProfileId) == null ?
                -1 : session.previousProfileId;
        ProfileStore.setLastAppliedProfileId(context, previousId);
        ProfileStore.recordResult(context, "Previous sound settings restored. " + result);
    }

    private static void scheduleReturn(Context context) {
        TemporarySession session = ProfileStore.temporary(context);
        if (session == null) return;
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms == null) return;
        PendingIntent pending = intent(context,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        alarms.cancel(pending);
        if (Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()) {
            try {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                        session.untilMillis, pending);
                return;
            } catch (SecurityException ignored) { }
        }
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, session.untilMillis, pending);
    }

    private static void cancelAlarm(Context context) {
        PendingIntent pending = intent(context,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pending == null) return;
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms != null) alarms.cancel(pending);
        pending.cancel();
    }

    private static PendingIntent intent(Context context, int flags) {
        Intent action = new Intent(context, ScheduleReceiver.class);
        action.setAction(ACTION_RESTORE);
        return PendingIntent.getBroadcast(context, 0, action, flags);
    }
}
