package com.walid.offlineprofiles;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.app.AlarmManager;

public final class ScheduleReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if (TemporaryManager.ACTION_RESTORE.equals(action)) {
            TemporaryManager.finish(context);
        } else if (ScheduleManager.ACTION_APPLY.equals(action)) {
            TemporaryManager.reconcile(context);
            int id = intent.getIntExtra(ScheduleManager.EXTRA_ID, -1);
            for (Schedule schedule : ProfileStore.schedules(context)) {
                if (schedule.id == id && schedule.enabled) {
                    Profile profile = ProfileStore.findProfile(context, schedule.profileId);
                    if (profile != null && !TemporaryManager.isActive(context))
                        ProfileApplier.apply(context, profile);
                    ScheduleManager.schedule(context, schedule);
                    return;
                }
            }
        } else if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
                Intent.ACTION_MY_PACKAGE_REPLACED.equals(action) ||
                Intent.ACTION_TIME_CHANGED.equals(action) ||
                Intent.ACTION_TIMEZONE_CHANGED.equals(action) ||
                AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED.equals(action)) {
            TemporaryManager.reconcile(context);
            ScheduleManager.scheduleAll(context);
        }
    }
}
