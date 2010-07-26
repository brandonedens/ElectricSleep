package com.androsz.electricsleep.service;

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;

import com.androsz.electricsleep.R;
import com.androsz.electricsleep.ui.SleepActivity;

public class SleepAccelerometerService extends Service implements
		SensorEventListener {
	public static final String POKE_SYNC_CHART = "com.androsz.electricsleep.POKE_SYNC_CHART";

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

	private int maximumSensitivity;
	private int minimumSensitivity;
	private int alarmTriggerSensitivity;

	public static final int SENSOR_DELAY = SensorManager.SENSOR_DELAY_UI;
	public static final long UPDATE_FREQUENCY = 10000;

	private final BroadcastReceiver pokeSyncChartReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (currentSeriesX.size() > 0 && currentSeriesY.size() > 0) {
				final Intent i = new Intent(SleepActivity.SYNC_CHART);
				i.putExtra("currentSeriesX", currentSeriesX);
				i.putExtra("currentSeriesY", currentSeriesY);
				i.putExtra("max", maximumSensitivity);
				i.putExtra("min", minimumSensitivity);
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
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
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

		try {
			SharedPreferences userPrefs = PreferenceManager
					.getDefaultSharedPreferences(getBaseContext());
			maximumSensitivity = userPrefs.getInt(
					getString(R.string.pref_maximum_sensitivity), 100);
			minimumSensitivity = userPrefs.getInt(
					getString(R.string.pref_minimum_sensitivity), 0);
			alarmTriggerSensitivity = userPrefs.getInt(
					getString(R.string.pref_alarm_trigger_sensitivity), 30);
		} catch (Exception e) {
			stopSelf();
		}

		registerReceiver(pokeSyncChartReceiver, new IntentFilter(
				POKE_SYNC_CHART));

		registerAccelerometerListener();

		obtainWakeLock();

		createNotification();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		unregisterReceiver(pokeSyncChartReceiver);

		sensorManager.unregisterListener(this);

		partialWakeLock.release();

		notificationManager.cancel(notificationId);

		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
			// We can read and write the media
			mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			// We can only read the media
			mExternalStorageAvailable = true;
			mExternalStorageWriteable = false;
		} else {
			// Something else is wrong. It may be one of many other states, but
			// all we need
			// to know is we can neither read nor write
			mExternalStorageAvailable = mExternalStorageWriteable = false;
		}
	}

	boolean mExternalStorageAvailable = false;
	boolean mExternalStorageWriteable = false;

	@Override
	public void onSensorChanged(SensorEvent event) {
		final long currentTime = System.currentTimeMillis();
		final long timeSinceLastSensorChange = currentTime
				- lastOnSensorChangedTime;

		totalNumberOfSensorChanges++;
		totalTimeBetweenSensorChanges += timeSinceLastSensorChange;

		final long deltaTime = currentTime - lastChartUpdateTime;
		if (deltaTime > UPDATE_FREQUENCY) {
			final double averageTimeBetweenUpdates = totalTimeBetweenSensorChanges
					/ totalNumberOfSensorChanges;
			final double x = currentTime;
			final double y = java.lang.Math.max(minimumSensitivity,
					java.lang.Math.min(maximumSensitivity, deltaTime
							/ averageTimeBetweenUpdates));

			currentSeriesX.add(x);
			currentSeriesY.add(y);

			final Intent i = new Intent(SleepActivity.UPDATE_CHART);
			i.putExtra("x", x);
			i.putExtra("y", y);
			i.putExtra("max", maximumSensitivity);
			i.putExtra("min", minimumSensitivity);
			sendBroadcast(i);

			totalNumberOfSensorChanges = 0;
			totalTimeBetweenSensorChanges = 0;

			lastChartUpdateTime = currentTime;

//			if (currentTime > 1280125500000L && y > MIN_SENSITIVITY * 1.5f) {
		/*if (currentTime > 12801395400L && y > alarmTriggerSensitivity) {
			stopSelf();
			}*/
		}

		lastOnSensorChangedTime = currentTime;
	}

	private void registerAccelerometerListener() {
		sensorManager = (SensorManager) getApplicationContext()
				.getSystemService(Context.SENSOR_SERVICE);

		sensorManager.registerListener(this, sensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SENSOR_DELAY);
	}
}
