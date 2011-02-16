package com.androsz.electricsleep.app;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;

import com.androsz.electricsleep.util.SharedWakeLock;

public class CheckForScreenBugAccelerometerService extends Service implements
		SensorEventListener {

	private final class ScreenReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(final Context context, final Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
				serviceHandler.postDelayed(setScreenIsOffRunnable, 6666);
				serviceHandler.postDelayed(turnScreenOnFallbackRunnable, 10000);
			}
		}
	}

	Handler serviceHandler = new Handler();

	Runnable turnScreenOnFallbackRunnable = new Runnable() {
		@Override
		public void run() {
			if (bugPresent) {
				CheckForScreenBugActivity.BUG_PRESENT_INTENT = new Intent(
						BUG_PRESENT);
			}
			turnScreenOn();
		}
	};

	Runnable setScreenIsOffRunnable = new Runnable() {
		@Override
		public void run() {
			screenIsOff = true;
		}
	};

	private static final String LOCK_TAG = CheckForScreenBugAccelerometerService.class
			.getName();
	public static final String BUG_NOT_PRESENT = "BUG_NOT_PRESENT";
	public static final String BUG_PRESENT = "BUG_PRESENT";
	private ScreenReceiver screenOnOffReceiver;

	private boolean bugPresent = true;
	private boolean screenIsOff = false;

	private PowerManager powerManager;

	boolean didNotTurnScreenOn = true;

	@Override
	public void onAccuracyChanged(final Sensor sensor, final int accuracy) {
		// Not used.
	}

	@Override
	public IBinder onBind(final Intent intent) {
		// Not used
		return null;
	}

	@Override
	public void onDestroy() {

		unregisterAccelerometerListener();

		unregisterReceiver(screenOnOffReceiver);

		serviceHandler.removeCallbacks(setScreenIsOffRunnable);
		serviceHandler.removeCallbacks(turnScreenOnFallbackRunnable);

		// check here so that certain devices keep their screen on for at least
		// 5 seconds (from turnScreenOn)
		if (didNotTurnScreenOn) {
			SharedWakeLock.release();
		}
		super.onDestroy();
	}

	@Override
	public void onSensorChanged(final SensorEvent event) {
		if (!powerManager.isScreenOn() && screenIsOff && bugPresent) {
			CheckForScreenBugActivity.BUG_PRESENT_INTENT = new Intent(
					BUG_NOT_PRESENT);
			turnScreenOn();
		}
	}

	@Override
	public int onStartCommand(final Intent intent, final int flags,
			final int startId) {
		if (startId == 1) {
			bugPresent = true;
			screenIsOff = false;
			final IntentFilter filter = new IntentFilter(
					Intent.ACTION_SCREEN_ON);
			filter.addAction(Intent.ACTION_SCREEN_OFF);
			screenOnOffReceiver = new ScreenReceiver();
			registerReceiver(screenOnOffReceiver, filter);
			powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
			SharedWakeLock.acquire(this, PowerManager.PARTIAL_WAKE_LOCK);
			registerAccelerometerListener();
		}
		return startId;
	}

	private void registerAccelerometerListener() {
		final SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_FASTEST);
	}

	private void turnScreenOn() {
		didNotTurnScreenOn = false;
		SharedWakeLock.acquire(this, PowerManager.SCREEN_DIM_WAKE_LOCK
				| PowerManager.ACQUIRE_CAUSES_WAKEUP
				| PowerManager.ON_AFTER_RELEASE, 5000);
		stopSelf();
	}

	private void unregisterAccelerometerListener() {
		final SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		sensorManager.unregisterListener(this);
	}
}
