package com.androsz.electricsleepbeta.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItem;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.androsz.electricsleepbeta.R;
import com.androsz.electricsleepbeta.alarmclock.AlarmClock;
import com.androsz.electricsleepbeta.content.StartSleepReceiver;
import com.androsz.electricsleepbeta.db.SleepSession;
import com.androsz.electricsleepbeta.db.SleepSessions;
import com.androsz.electricsleepbeta.util.MathUtils;
import com.androsz.electricsleepbeta.widget.SleepChart;

/**
 * Front-door {@link Activity} that displays high-level features the application
 * offers to users.
 */
public class HomeFragment extends HostFragment implements LoaderManager.LoaderCallbacks<Cursor> {

	private SleepChart sleepChart;

	@Override
	protected int getContentAreaLayoutId() {
		return R.layout.activity_home;
	}

	public void onAlarmsClick(final View v) {
		startActivity(new Intent(getActivity(), AlarmClock.class));
	}

	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		
		sleepChart = (SleepChart)view.findViewById(R.id.home_sleep_chart);
		((HostActivity) getActivity()).getSupportLoaderManager().initLoader(0, null, this);
		
		return view;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(getActivity(), SleepSessions.MainTable.CONTENT_URI,
				SleepSessions.MainTable.ALL_COLUMNS_PROJECTION, null, null, null);
	}

	/*
	 * Used for overriding default HostActivity behavior..
	 * 
	 * @Override public boolean onCreateOptionsMenu(Menu menu) { final boolean
	 * result = super.onCreateOptionsMenu(menu);
	 * menu.findItem(R.id.menu_item_donate).setShowAsAction(
	 * MenuItem.SHOW_AS_ACTION_WITH_TEXT | MenuItem.SHOW_AS_ACTION_IF_ROOM);
	 * menu.findItem(R.id.menu_item_settings).setShowAsAction(MenuItem.
	 * SHOW_AS_ACTION_IF_ROOM); return result; }
	 */

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		// cancel home as up
		if (item.getItemId() == android.R.id.home) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {

	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {
		Activity activity = getActivity();
		final TextView lastSleepTitleText = (TextView) activity
				.findViewById(R.id.home_last_sleep_title_text);
		final TextView reviewTitleText = (TextView) activity
				.findViewById(R.id.home_review_title_text);
		final ViewGroup container = (ViewGroup) activity.findViewById(R.id.home_stats_container);
		if (cursor == null || cursor.getCount() == 0) {
			container.setVisibility(View.GONE);
			reviewTitleText.setText(getString(R.string.home_review_title_text_empty));
			lastSleepTitleText.setText(getString(R.string.home_last_sleep_title_text_empty));
		} else {

			final TextView avgScoreText = (TextView) activity.findViewById(R.id.value_score_text);
			final TextView avgDurationText = (TextView) activity
					.findViewById(R.id.value_duration_text);
			final TextView avgSpikesText = (TextView) activity.findViewById(R.id.value_spikes_text);
			final TextView avgFellAsleepText = (TextView) activity
					.findViewById(R.id.value_fell_asleep_text);
			cursor.moveToLast();

			sleepChart.sync(cursor);
			sleepChart.setMinimumHeight(MathUtils.getAbsoluteScreenHeightPx(activity) / 2);
			lastSleepTitleText.setText(getString(R.string.home_last_sleep_title_text));

			cursor.moveToFirst();
			int avgSleepScore = 0;
			long avgDuration = 0;
			int avgSpikes = 0;
			long avgFellAsleep = 0;
			int count = 0;
			do {
				count++;
				SleepSession sleepRecord = null;
				try {
					sleepRecord = new SleepSession(cursor);
				} catch (final CursorIndexOutOfBoundsException cioobe) {
					// there are no records!
					return;
				}
				avgSleepScore += sleepRecord.getSleepScore();
				avgDuration += sleepRecord.duration;
				avgSpikes += sleepRecord.spikes;
				avgFellAsleep += sleepRecord.getTimeToFallAsleep();

			} while (cursor.moveToNext());

			final float invCount = 1.0f / count;
			avgSleepScore *= invCount;
			avgDuration *= invCount;
			avgSpikes *= invCount;
			avgFellAsleep *= invCount;

			avgScoreText.setText(avgSleepScore + "%");
			avgDurationText.setText(SleepSession.getTimespanText(avgDuration, getResources()));
			avgSpikesText.setText(avgSpikes + "");
			avgFellAsleepText.setText(SleepSession.getTimespanText(avgFellAsleep, getResources()));

			reviewTitleText.setText(getString(R.string.home_review_title_text));
			container.setVisibility(View.VISIBLE);
			sleepChart.setVisibility(View.VISIBLE);
		}
	}

	public void onSleepClick(final View v) throws Exception {
		getActivity().sendBroadcast(new Intent(StartSleepReceiver.START_SLEEP));
	}
}
