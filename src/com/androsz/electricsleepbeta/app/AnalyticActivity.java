package com.androsz.electricsleepbeta.app;

import android.app.Application;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.androsz.electricsleepbeta.util.GoogleAnalyticsSessionHelper;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;

public abstract class AnalyticActivity extends FragmentActivity {

	public static final String KEY = "UA-19363335-1";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		GoogleAnalyticsSessionHelper.getInstance(KEY, getApplication()).incrementSession();

		String versionName = "?";
		try {
			versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} catch (final NameNotFoundException e) {
			e.printStackTrace();
		}

		GoogleAnalyticsTracker.getInstance().setProductVersion(getPackageName(), versionName);

		// I have no idea...
		GoogleAnalyticsTracker.getInstance().setCustomVar(1, Integer.toString(VERSION.SDK_INT),
				Build.MODEL);
		GoogleAnalyticsTracker.getInstance().setCustomVar(2, versionName,
				Build.MODEL + "-" + Integer.toString(VERSION.SDK_INT));

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		GoogleAnalyticsTracker.getInstance().dispatch();

		GoogleAnalyticsSessionHelper.getExistingInstance().decrementSession();
	}

	@Override
	protected void onResume() {
		super.onResume();

		// Example of how to track a pageview event
		trackPageView(getClass().getSimpleName());
	}

	protected void trackEvent(final String label, final int value) {
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				try {
					GoogleAnalyticsTracker.getInstance().trackEvent(
							Integer.toString(VERSION.SDK_INT), Build.MODEL, label, value);
				} catch (final Exception whocares) {
				}
				return null;
			}
		}.execute();

	}

	protected void trackPageView(final String pageUrl) {
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				try {
					GoogleAnalyticsTracker.getInstance().trackPageView(pageUrl);
				} catch (final Exception whocares) {
				}
				return null;
			}
		}.execute();
	}

}
