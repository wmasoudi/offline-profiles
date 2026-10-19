package com.walid.offlineprofiles;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.view.WindowInsets;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.text.DateFormat;

public final class MainActivity extends Activity {
    private static final int INK = Color.rgb(27, 37, 48);
    private static final int MUTED = Color.rgb(89, 101, 114);
    private LinearLayout content;
    private ScrollView scroll;
    private int selectedTab = 0;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (state != null) selectedTab = state.getInt("selectedTab", 0);
        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(246, 248, 251));
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(18), dp(18), dp(36));
        scroll.addView(content);
        if (Build.VERSION.SDK_INT >= 30) {
            scroll.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override public WindowInsets onApplyWindowInsets(View view, WindowInsets insets) {
                    android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                    content.setPadding(dp(18), dp(18) + bars.top, dp(18), dp(36) + bars.bottom);
                    return insets;
                }
            });
        }
        setContentView(scroll);
        TemporaryManager.reconcile(this);
        ScheduleManager.scheduleAll(this);
        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (content != null) {
            TemporaryManager.reconcile(this);
            ScheduleManager.scheduleAll(this);
            refresh();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        state.putInt("selectedTab", selectedTab);
        super.onSaveInstanceState(state);
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private Button button(String label, View.OnClickListener action) {
        Button view = new Button(this);
        view.setText(label);
        view.setAllCaps(false);
        view.setOnClickListener(action);
        return view;
    }

    private Button tabButton(String label, final int tab) {
        Button view = button(label, v -> {
            selectedTab = tab;
            refresh();
            scroll.post(() -> scroll.scrollTo(0, 0));
        });
        boolean active = selectedTab == tab;
        view.setBackgroundTintList(ColorStateList.valueOf(active ?
                Color.rgb(42, 93, 168) : Color.rgb(225, 231, 239)));
        view.setTextColor(active ? Color.WHITE : INK);
        return view;
    }

    private LinearLayout card() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setPadding(dp(16), dp(13), dp(16), dp(13));
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setCornerRadius(dp(14));
        background.setStroke(dp(1), Color.rgb(225, 230, 237));
        view.setBackground(background);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(12);
        content.addView(view, params);
        return view;
    }

    private void heading(String value) {
        TextView view = text(value, 20, INK, true);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(18);
        params.bottomMargin = dp(10);
        content.addView(view, params);
    }

    private void info(LinearLayout parent, String value) {
        TextView view = text(value, 14, MUTED, false);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(4);
        params.bottomMargin = dp(8);
        parent.addView(view, params);
    }

    private void refresh() {
        int oldY = scroll.getScrollY();
        content.removeAllViews();
        TextView title = text("Offline Profiles", 29, INK, true);
        content.addView(title);
        TextView subtitle = text("Sound profiles and local schedules · No ads or network access", 14,
                MUTED, false);
        content.addView(subtitle);

        heading("Access");
        LinearLayout access = card();
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        boolean policy = nm != null && nm.isNotificationPolicyAccessGranted();
        info(access, "Sound mode access: " + (policy ? "Ready" : "Needed for silent mode"));
        if (!policy) access.addView(button("Grant sound mode access", new View.OnClickListener() {
            @Override public void onClick(View view) {
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
            }
        }));
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        boolean exact = Build.VERSION.SDK_INT < 31 ||
                (alarmManager != null && alarmManager.canScheduleExactAlarms());
        info(access, "Precise timers and schedules: " + (exact ? "Ready" :
                "Needs Alarms & reminders access"));
        if (!exact) access.addView(button("Grant precise timing access", new View.OnClickListener() {
            @Override public void onClick(View view) {
                try {
                    Intent request = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    request.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(request);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Open Alarms & reminders in Android settings",
                            Toast.LENGTH_LONG).show();
                }
            }
        }));

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams tabWidth = new LinearLayout.LayoutParams(0, -2, 1);
        tabs.addView(tabButton("Profiles", 0), tabWidth);
        tabs.addView(tabButton("Schedules", 1), new LinearLayout.LayoutParams(0, -2, 1));
        content.addView(tabs);

        if (selectedTab == 0) {
        heading("Profile status");
        LinearLayout current = card();
        int lastId = ProfileStore.lastAppliedProfileId(this);
        Profile lastProfile = ProfileStore.findProfile(this, lastId);
        info(current, "Last applied: " + (lastProfile == null ?
                "No profile applied yet" : lastProfile.name));
        TemporarySession temporary = ProfileStore.temporary(this);
        if (temporary != null) {
            Profile target = ProfileStore.findProfile(this, temporary.temporaryProfileId);
            Profile previous = ProfileStore.findProfile(this, temporary.previousProfileId);
            String until = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                    .format(temporary.untilMillis);
            info(current, "Temporary: " + (target == null ? "profile" : target.name) +
                    " until " + until + ". Then restore " +
                    (previous == null ? "previous sound settings" : previous.name) + ".");
            current.addView(button("Restore now", v -> {
                TemporaryManager.restoreNow(MainActivity.this);
                refresh();
            }));
        }

        heading("Profiles");
        LinearLayout actions = card();
        actions.addView(button("Create a profile", new View.OnClickListener() {
            @Override public void onClick(View view) {
                editProfile(new Profile(ProfileStore.newId(MainActivity.this), "New profile"), true);
            }
        }));
        actions.addView(button("Save current sound levels as a profile", new View.OnClickListener() {
            @Override public void onClick(View view) {
                editProfile(Profile.capture(MainActivity.this, ProfileStore.newId(MainActivity.this)), true);
            }
        }));
        List<Profile> profiles = ProfileStore.profiles(this);
        for (final Profile profile : profiles) {
            LinearLayout row = card();
            row.addView(text(profile.name, 18, INK, true));
            info(row, profile.summary());
            row.addView(button("Apply", new View.OnClickListener() {
                @Override public void onClick(View view) {
                    TemporaryManager.cancel(MainActivity.this);
                    String result = ProfileApplier.apply(MainActivity.this, profile);
                    refresh();
                    new AlertDialog.Builder(MainActivity.this).setTitle("Profile result")
                            .setMessage(result).setPositiveButton("OK", null).show();
                }
            }));
            row.addView(button("Timed apply", v -> timedApply(profile)));
            row.addView(button("Edit", new View.OnClickListener() {
                @Override public void onClick(View view) { editProfile(profile.copy(), false); }
            }));
            row.addView(button("Delete", new View.OnClickListener() {
                @Override public void onClick(View view) { deleteProfile(profile); }
            }));
        }
        } else {
        heading("Schedules");
        LinearLayout scheduleIntro = card();
        info(scheduleIntro, "Recurring time rules run on your phone. Grant precise timing " +
                "access for exact times; without it Android may delay a switch to save battery.");
        scheduleIntro.addView(button("Add schedule", new View.OnClickListener() {
            @Override public void onClick(View view) { editSchedule(null); }
        }));
        List<Schedule> schedules = ProfileStore.schedules(this);
        for (final Schedule schedule : schedules) {
            final Profile target = ProfileStore.findProfile(this, schedule.profileId);
            LinearLayout row = card();
            String label = String.format(java.util.Locale.getDefault(), "%02d:%02d", schedule.hour,
                    schedule.minute) + "  " + (target == null ? "Missing profile" : target.name);
            row.addView(text(label, 17, INK, true));
            info(row, schedule.daysText() + (schedule.enabled ? " · Active" : " · Paused"));
            row.addView(button(schedule.enabled ? "Pause" : "Resume", new View.OnClickListener() {
                @Override public void onClick(View view) {
                    schedule.enabled = !schedule.enabled;
                    ProfileStore.saveSchedules(MainActivity.this,
                            replaceSchedule(ProfileStore.schedules(MainActivity.this), schedule));
                    ScheduleManager.schedule(MainActivity.this, schedule);
                    refresh();
                }
            }));
            row.addView(button("Edit", new View.OnClickListener() {
                @Override public void onClick(View view) { editSchedule(schedule); }
            }));
            row.addView(button("Delete", new View.OnClickListener() {
                @Override public void onClick(View view) { deleteSchedule(schedule); }
            }));
        }
        }

        heading("Last action");
        LinearLayout last = card();
        info(last, ProfileStore.lastResult(this));
        scroll.post(new Runnable() {
            @Override public void run() { scroll.scrollTo(0, oldY); }
        });
    }

    private void timedApply(final Profile profile) {
        String[] choices = {"30 minutes", "1 hour", "1 hour 30 minutes", "2 hours",
                "Custom duration…", "Until a clock time…"};
        new AlertDialog.Builder(this).setTitle("Timed apply: " + profile.name)
                .setItems(choices, (dialog, which) -> {
                    if (which < 4) {
                        int[] minutes = {30, 60, 90, 120};
                        applyTimed(profile, System.currentTimeMillis() + minutes[which] * 60000L);
                    } else if (which == 4) {
                        customDuration(profile);
                    } else {
                        untilTime(profile);
                    }
                }).show();
    }

    private void customDuration(final Profile profile) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(18), dp(8), dp(18), dp(8));
        final EditText hours = new EditText(this);
        hours.setInputType(InputType.TYPE_CLASS_NUMBER);
        hours.setHint("Hours");
        hours.setText("1");
        form.addView(hours);
        final EditText minutes = new EditText(this);
        minutes.setInputType(InputType.TYPE_CLASS_NUMBER);
        minutes.setHint("Minutes (0–59)");
        minutes.setText("0");
        form.addView(minutes);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Apply " + profile.name + " for")
                .setView(form).setNegativeButton("Cancel", null)
                .setPositiveButton("Apply", null).create();
        dialog.setOnShowListener(shown -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    try {
                        int h = Integer.parseInt(hours.getText().toString());
                        int m = Integer.parseInt(minutes.getText().toString());
                        if (h < 0 || h > 168 || m < 0 || m > 59 || h + m == 0) {
                            Toast.makeText(this, "Enter 1 minute to 168 hours 59 minutes",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        long duration = (h * 60L + m) * 60000L;
                        dialog.dismiss();
                        applyTimed(profile, System.currentTimeMillis() + duration);
                    } catch (NumberFormatException ignored) {
                        Toast.makeText(this, "Enter hours and minutes as numbers",
                                Toast.LENGTH_LONG).show();
                    }
                }));
        dialog.show();
    }

    private void untilTime(final Profile profile) {
        final TimePicker picker = new TimePicker(this);
        picker.setIs24HourView(true);
        Calendar suggested = Calendar.getInstance();
        suggested.add(Calendar.HOUR_OF_DAY, 1);
        picker.setHour(suggested.get(Calendar.HOUR_OF_DAY));
        picker.setMinute(suggested.get(Calendar.MINUTE));
        new AlertDialog.Builder(this).setTitle("Apply " + profile.name + " until")
                .setView(picker).setNegativeButton("Cancel", null)
                .setPositiveButton("Apply", (dialog, which) -> {
                    Calendar end = Calendar.getInstance();
                    end.set(Calendar.HOUR_OF_DAY, picker.getHour());
                    end.set(Calendar.MINUTE, picker.getMinute());
                    end.set(Calendar.SECOND, 0);
                    end.set(Calendar.MILLISECOND, 0);
                    if (end.getTimeInMillis() <= System.currentTimeMillis())
                        end.add(Calendar.DAY_OF_YEAR, 1);
                    applyTimed(profile, end.getTimeInMillis());
                }).show();
    }

    private void applyTimed(Profile profile, long untilMillis) {
        String result = TemporaryManager.start(this, profile, untilMillis);
        refresh();
        String until = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(untilMillis);
        new AlertDialog.Builder(this).setTitle("Timed profile active until " + until)
                .setMessage(result).setPositiveButton("OK", null).show();
    }

    private interface IntSetter { void set(int value); }

    private void slider(LinearLayout parent, String name, int current, final IntSetter setter) {
        final CheckBox check = new CheckBox(this);
        check.setChecked(current >= 0);
        final SeekBar bar = new SeekBar(this);
        bar.setMax(100);
        bar.setProgress(current >= 0 ? current : 50);
        bar.setEnabled(current >= 0);
        final Runnable update = new Runnable() {
            @Override public void run() {
                check.setText(name + (check.isChecked() ? ": " + bar.getProgress() + "%" :
                        ": keep current"));
            }
        };
        update.run();
        check.setOnCheckedChangeListener((buttonView, checked) -> {
            bar.setEnabled(checked);
            setter.set(checked ? bar.getProgress() : -1);
            update.run();
        });
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (check.isChecked()) setter.set(progress);
                update.run();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        parent.addView(check);
        parent.addView(bar);
    }

    private void editProfile(final Profile profile, final boolean fresh) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(18), dp(10), dp(18), dp(12));
        final EditText name = new EditText(this);
        name.setSingleLine(true);
        name.setHint("Profile name");
        name.setText(profile.name);
        form.addView(name);
        form.addView(text("Sound mode", 15, INK, true));
        final int[] modes = {-1, AudioManager.RINGER_MODE_NORMAL,
                AudioManager.RINGER_MODE_VIBRATE, AudioManager.RINGER_MODE_SILENT};
        final Spinner mode = new Spinner(this);
        mode.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Keep current", "Normal", "Vibrate", "Silent"}));
        for (int i = 0; i < modes.length; i++) if (modes[i] == profile.ringerMode) mode.setSelection(i);
        form.addView(mode);
        slider(form, "Ringtone volume", profile.ring, value -> profile.ring = value);
        slider(form, "Notification volume", profile.notification,
                value -> profile.notification = value);
        slider(form, "Media volume", profile.media, value -> profile.media = value);
        slider(form, "Alarm volume", profile.alarm, value -> profile.alarm = value);
        slider(form, "System volume", profile.system, value -> profile.system = value);
        ScrollView container = new ScrollView(this);
        container.addView(form);
        new AlertDialog.Builder(this).setTitle(fresh ? "New profile" : "Edit profile")
                .setView(container).setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> {
                    profile.name = name.getText().toString().trim();
                    if (profile.name.isEmpty()) profile.name = "Profile";
                    profile.ringerMode = modes[mode.getSelectedItemPosition()];
                    List<Profile> values = ProfileStore.profiles(MainActivity.this);
                    boolean replaced = false;
                    for (int i = 0; i < values.size(); i++) if (values.get(i).id == profile.id) {
                        values.set(i, profile);
                        replaced = true;
                        break;
                    }
                    if (!replaced) values.add(profile);
                    ProfileStore.saveProfiles(MainActivity.this, values);
                    refresh();
                }).show();
    }

    private void deleteProfile(final Profile profile) {
        new AlertDialog.Builder(this).setTitle("Delete " + profile.name + "?")
                .setMessage("Schedules using this profile will also be removed.")
                .setNegativeButton("Cancel", null).setPositiveButton("Delete", (dialog, which) -> {
                    List<Profile> profiles = ProfileStore.profiles(MainActivity.this);
                    profiles.removeIf(p -> p.id == profile.id);
                    ProfileStore.saveProfiles(MainActivity.this, profiles);
                    if (ProfileStore.lastAppliedProfileId(MainActivity.this) == profile.id)
                        ProfileStore.setLastAppliedProfileId(MainActivity.this, -1);
                    List<Schedule> schedules = ProfileStore.schedules(MainActivity.this);
                    for (Schedule s : schedules) if (s.profileId == profile.id)
                        ScheduleManager.cancel(MainActivity.this, s.id);
                    schedules.removeIf(s -> s.profileId == profile.id);
                    ProfileStore.saveSchedules(MainActivity.this, schedules);
                    refresh();
                }).show();
    }

    private static List<Schedule> replaceSchedule(List<Schedule> values, Schedule updated) {
        for (int i = 0; i < values.size(); i++) if (values.get(i).id == updated.id) {
            values.set(i, updated);
            return values;
        }
        values.add(updated);
        return values;
    }

    private void editSchedule(final Schedule existing) {
        final List<Profile> profiles = ProfileStore.profiles(this);
        if (profiles.isEmpty()) {
            Toast.makeText(this, "Create a profile first", Toast.LENGTH_SHORT).show();
            return;
        }
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(18), dp(10), dp(18), dp(12));
        form.addView(text("Apply profile", 15, INK, true));
        final Spinner target = new Spinner(this);
        List<String> names = new ArrayList<>();
        for (Profile p : profiles) names.add(p.name);
        target.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names));
        if (existing != null) for (int i = 0; i < profiles.size(); i++)
            if (profiles.get(i).id == existing.profileId) target.setSelection(i);
        form.addView(target);
        final TimePicker time = new TimePicker(this);
        time.setIs24HourView(true);
        if (existing != null) {
            time.setHour(existing.hour);
            time.setMinute(existing.minute);
        }
        form.addView(time);
        form.addView(text("Repeat on", 15, INK, true));
        String[] namesOfDays = {"Sunday", "Monday", "Tuesday", "Wednesday",
                "Thursday", "Friday", "Saturday"};
        final CheckBox[] days = new CheckBox[7];
        for (int i = 0; i < 7; i++) {
            days[i] = new CheckBox(this);
            days[i].setText(namesOfDays[i]);
            days[i].setChecked(existing == null || (existing.days & (1 << i)) != 0);
            form.addView(days[i]);
        }
        ScrollView container = new ScrollView(this);
        container.addView(form);
        new AlertDialog.Builder(this).setTitle(existing == null ? "New schedule" : "Edit schedule")
                .setView(container).setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> {
                    int mask = 0;
                    for (int i = 0; i < 7; i++) if (days[i].isChecked()) mask |= 1 << i;
                    if (mask == 0) {
                        Toast.makeText(MainActivity.this, "Select at least one day", Toast.LENGTH_LONG).show();
                        return;
                    }
                    Schedule schedule = existing == null ? new Schedule(ProfileStore.newId(
                            MainActivity.this), profiles.get(0).id, time.getHour(), time.getMinute())
                            : existing;
                    schedule.profileId = profiles.get(target.getSelectedItemPosition()).id;
                    schedule.hour = time.getHour();
                    schedule.minute = time.getMinute();
                    schedule.days = mask;
                    ProfileStore.saveSchedules(MainActivity.this,
                            replaceSchedule(ProfileStore.schedules(MainActivity.this), schedule));
                    ScheduleManager.schedule(MainActivity.this, schedule);
                    refresh();
                }).show();
    }

    private void deleteSchedule(final Schedule schedule) {
        new AlertDialog.Builder(this).setTitle("Delete schedule?")
                .setNegativeButton("Cancel", null).setPositiveButton("Delete", (dialog, which) -> {
                    ScheduleManager.cancel(MainActivity.this, schedule.id);
                    List<Schedule> values = ProfileStore.schedules(MainActivity.this);
                    values.removeIf(s -> s.id == schedule.id);
                    ProfileStore.saveSchedules(MainActivity.this, values);
                    refresh();
                }).show();
    }
}
