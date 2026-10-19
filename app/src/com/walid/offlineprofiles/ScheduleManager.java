package com.walid.offlineprofiles;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

final class ScheduleManager {
    static final String ACTION_APPLY = "com.walid.offlineprofiles.APPLY_SCHEDULE";
    static final String EXTRA_ID = "schedule_id";

    private ScheduleManager() { }

    static void scheduleAll(Context context) {
        for (Schedule schedule : ProfileStore.schedules(context)) schedule(context, schedule);
    }

    static void schedule(Context context, Schedule schedule) {
        cancel(context, schedule.id);
        long next = schedule.nextTimeMillis(System.currentTimeMillis());
        if (next < 0 || ProfileStore.findProfile(context, schedule.profileId) == null) return;
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms != null) {
            PendingIntent pending = intent(context, schedule.id,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            if (Build.VERSION.SDK_INT >= 31 && alarms.canScheduleExactAlarms()) {
                try {
                    alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pending);
                    return;
                } catch (SecurityException ignored) { }
            }
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pending);
        }
    }

    static void cancel(Context context, int id) {
        PendingIntent pending = intent(context, id,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pending != null) {
            AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarms != null) alarms.cancel(pending);
            pending.cancel();
        }
    }

    private static PendingIntent intent(Context context, int id, int flags) {
        Intent action = new Intent(context, ScheduleReceiver.class);
        action.setAction(ACTION_APPLY);
        action.putExtra(EXTRA_ID, id);
        return PendingIntent.getBroadcast(context, id, action, flags);
    }
}
