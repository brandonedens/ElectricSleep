package com.androsz.electricsleep.service;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.widget.Toast;

import com.androsz.electricsleep.R;
import com.androsz.electricsleep.db.SleepHistoryDatabase;
import com.androsz.electricsleep.ui.SaveSleepActivity;
import com.androsz.electricsleep.ui.SleepActivity;
import com.androsz.electricsleep.util.Alarm;
import com.androsz.electricsleep.util.AlarmDatabase;

public class SleepAccelerometerService extends Service implements
		SensorEventListener {
	public static final String POKE_SYNC_CHART = "com.androsz.electricsleep.POKE_SYNC_CHART";
	public static final String ALARM_TRIGGERED = "com.androsz.electricsleep.ALARM_TRIGGERED";

	private final int notificationId = 0x1337;

	public ArrayList<Double> currentSeriesX = new ArrayList<Double>();
	public ArrayList<Double> currentSeriesY = new ArrayList<Double>();

	private SensorManager sensorManager;

	private PowerManager powerManager;
	private WakeLock partialWakeLock;

	private NotificationManager notificationManager;

	private long lastChartUpdateTime = System.currentTimeMillis();
	private long lastOnSensorChangedTime = System.currentTimeMillis();
	private long totalTimeBetweenSensorChanges = 0;
	private int totalNumberOfSensorChanges = 0;

	private int minSensitivity = 0;
	private int maxSensitivity = 100;
	private int alarmTriggerSensitivity = -1;

	private boolean useAlarm = false;
	private int alarmWindow = 30;

	private int updateInterval = 60000;

	private Date dateStarted;

	public static final int SENSOR_DELAY = SensorManager.SENSOR_DELAY_UI;

	private final BroadcastReceiver pokeSyncChartReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (currentSeriesX.size() > 0 && currentSeriesY.size() > 0) {
				final Intent i = new Intent(SleepActivity.SYNC_CHART);
				i.putExtra("currentSeriesX", currentSeriesX);
				i.putExtra("currentSeriesY", currentSeriesY);
				i.putExtra("min", minSensitivity);
				i.putExtra("max", maxSensitivity);
				sendBroadcast(i);
			}
		}
	};

	private void createNotification() {
		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		final int icon = R.drawable.icon;
		final CharSequence tickerText = getText(R.string.notification_sleep_started);
		final long when = System.currentTimeMillis();

		final Notification notification = new Notification(icon, tickerText,
				when);

		notification.flags = Notification.FLAG_ONGOING_EVENT;

		final Context context = getApplicationContext();
		final CharSequence contentTitle = getText(R.string.notification_sleep_title);
		final CharSequence contentText = getText(R.string.notification_sleep_text);
		final Intent notificationIntent = new Intent(this, SleepActivity.class);
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
				| Intent.FLAG_ACTIVITY_NEW_TASK);
		final PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);

		notification.setLatestEventInfo(context, contentTitle, contentText,
				contentIntent);

		notificationManager.notify(notificationId, notification);
	}

	private void obtainWakeLock() {
		powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		partialWakeLock = powerManager.newWakeLock(
				PowerManager.PARTIAL_WAKE_LOCK, toString());
		partialWakeLock.acquire();
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// not used
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		registerReceiver(pokeSyncChartReceiver, new IntentFilter(
				POKE_SYNC_CHART));

		registerAccelerometerListener();

		obtainWakeLock();

		createNotification();

		dateStarted = new Date();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		unregisterReceiver(pokeSyncChartReceiver);

		sensorManager.unregisterListener(SleepAccelerometerService.this);

		partialWakeLock.release();

		notificationManager.cancel(notificationId);

		saveSleepData();
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		final long currentTime = System.currentTimeMillis();
		final long timeSinceLastSensorChange = currentTime
				- lastOnSensorChangedTime;

		totalNumberOfSensorChanges++;
		totalTimeBetweenSensorChanges += timeSinceLastSensorChange;

		final long deltaTime = currentTime - lastChartUpdateTime;
		if (deltaTime > updateInterval) {
			final double averageTimeBetweenUpdates = totalTimeBetweenSensorChanges
					/ totalNumberOfSensorChanges;
			final double x = currentTime;
			final double y = java.lang.Math
					.max(minSensitivity, java.lang.Math.min(maxSensitivity,
							deltaTime / averageTimeBetweenUpdates));

			// this should help reduce both runtime memory and saved data memory
			// later on.
			final int yOneAgoIndex = currentSeriesY.size() - 1;
			final int yTwoAgoIndex = currentSeriesY.size() - 2;
			boolean syncChart = false;
			if (yTwoAgoIndex > 0) {
				final double oneAgo = currentSeriesY.get(yOneAgoIndex);
				final double twoAgo = currentSeriesY.get(yTwoAgoIndex);
				if (Math.round(oneAgo) == Math.round(twoAgo)
						&& Math.round(oneAgo) == Math.round(y)) {
					currentSeriesX.remove(yOneAgoIndex);
					currentSeriesY.remove(yOneAgoIndex);
					// flag to sync instead of update
					syncChart = true;
				}
			}

			currentSeriesX.add(x);
			currentSeriesY.add(y);

			if (syncChart) {
				pokeSyncChartReceiver.onReceive(this, null);
			} else {
				final Intent i = new Intent(SleepActivity.UPDATE_CHART);
				i.putExtra("x", x);
				i.putExtra("y", y);
				i.putExtra("min", minSensitivity);
				i.putExtra("max", maxSensitivity);
				sendBroadcast(i);
			}

			totalNumberOfSensorChanges = 0;
			totalTimeBetweenSensorChanges = 0;

			lastChartUpdateTime = currentTime;

			if (useAlarm) {
				final AlarmDatabase adb = new AlarmDatabase(
						getContentResolver(), "com.android.deskclock");
				final Alarm alarm = adb.getNearestEnabledAlarm();
				final Calendar alarmTime = alarm.getNearestAlarmDate();
				alarmTime.add(Calendar.MINUTE, alarmWindow * -1);
				final long alarmMillis = alarmTime.getTimeInMillis();
				if (currentTime >= alarmMillis && y >= alarmTriggerSensitivity) {
					alarm.time = currentTime;
					com.androsz.electricsleep.util.AlarmDatabase.triggerAlarm(
							this, alarm);
					stopSelf();
				}
			}
		}
		lastOnSensorChangedTime = currentTime;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		updateInterval = intent.getIntExtra("interval", updateInterval);
		minSensitivity = intent.getIntExtra("min", minSensitivity);
		maxSensitivity = intent.getIntExtra("max", maxSensitivity);
		alarmTriggerSensitivity = intent.getIntExtra("alarm",
				alarmTriggerSensitivity);

		useAlarm = intent.getBooleanExtra("useAlarm", useAlarm);
		alarmWindow = intent.getIntExtra("alarmWindow", alarmWindow);

		return startId;
	}

	private void registerAccelerometerListener() {
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

		sensorManager.registerListener(this, sensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SENSOR_DELAY);
	}

	private void saveSleepData() {
		if (currentSeriesX.size() > 1 && currentSeriesY.size() > 1) {
			Intent saveIntent = new Intent(this, SaveSleepActivity.class);
			final SleepHistoryDatabase shdb = new SleepHistoryDatabase(this);
			try {
				final DateFormat sdf = DateFormat
						.getDateTimeInstance(DateFormat.SHORT,
								DateFormat.SHORT, Locale.getDefault());
				DateFormat sdf2 = DateFormat
						.getDateTimeInstance(DateFormat.SHORT,
								DateFormat.SHORT, Locale.getDefault());
				final Date now = new Date();
				if (dateStarted.getDate() == now.getDate()) {
					sdf2 = DateFormat.getTimeInstance(DateFormat.SHORT);
				}

				shdb
						.addSleep(sdf.format(dateStarted) + " to "
								+ sdf2.format(now), currentSeriesX,
								currentSeriesY, minSensitivity, maxSensitivity,
								alarmTriggerSensitivity);
			} catch (final IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
