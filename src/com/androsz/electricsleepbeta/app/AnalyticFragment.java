package com.androsz.electricsleepbeta.app;

import android.app.Activity;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.AttributeSet;
import android.view.View;

import com.androsz.electricsleepbeta.util.GoogleAnalyticsSessionManager;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;

public abstract class AnalyticFragment extends Fragment {

	public abstract void onClick(View v);


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		String versionName = "?";
		Activity a = getActivity();
		try {
			versionName = a.getPackageManager().getPackageInfo(a.getPackageName(),
					0).versionName;
		} catch (final NameNotFoundException e) {
			e.printStackTrace();
		}

		GoogleAnalyticsTracker.getInstance().setProductVersion(
				a.getPackageName(), versionName);
		
		// Need to do this for every activity that uses google analytics
		GoogleAnalyticsSessionManager.getInstance(getActivity().getApplication())
				.incrementActivityCount();
	}

	@Override
	public void onResume() {
		super.onResume();

		// Example of how to track a pageview event
		trackPageView(getClass().getSimpleName());
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		// Purge analytics so they don't hold references to this activity
		GoogleAnalyticsTracker.getInstance().dispatch();

		// Need to do this for every activity that uses google analytics
		GoogleAnalyticsSessionManager.getInstance().decrementActivityCount();
	}
	

	protected void trackEvent(final String label, final int value) {
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				try {
					GoogleAnalyticsTracker.getInstance().trackEvent(
							Integer.toString(VERSION.SDK_INT), Build.MODEL, label, value);
				} catch (Throwable whocares) {
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
				} catch (Throwable whocares) {
				}
				return null;
			}
		}.execute();
	}
}
