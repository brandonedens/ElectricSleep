package com.androsz.electricsleepbeta.app;

import android.app.Activity;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.androsz.electricsleepbeta.util.GoogleAnalyticsSessionHelper;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;

public abstract class AnalyticFragment extends Fragment {

	protected abstract int getContentAreaLayoutId();

	public abstract void onClick(View v);

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		String versionName = "?";
		final Activity a = getActivity();
		try {
			versionName = a.getPackageManager().getPackageInfo(a.getPackageName(), 0).versionName;
		} catch (final NameNotFoundException e) {
			e.printStackTrace();
		}

		GoogleAnalyticsTracker.getInstance().setProductVersion(a.getPackageName(), versionName);

		// Need to do this for every activity that uses google analytics
		GoogleAnalyticsSessionHelper.getInstance(AnalyticActivity.KEY, getActivity().getApplication())
				.incrementSession();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View view = inflater.inflate(getContentAreaLayoutId(), container, false);

		view.setBackgroundColor(Color.BLACK);
		//view.setBackgroundResource(R.drawable.gradient_background_vert);
		return view;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		GoogleAnalyticsTracker.getInstance().dispatch();

		GoogleAnalyticsSessionHelper.getExistingInstance().decrementSession();
	}

	@Override
	public void onResume() {
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
