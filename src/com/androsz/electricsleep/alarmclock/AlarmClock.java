/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.androsz.electricsleep.alarmclock;

import java.util.Calendar;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.androsz.electricsleepdonate.R;
import com.androsz.electricsleep.app.SettingsActivity;

/**
 * AlarmClock application.
 */
public class AlarmClock extends
		com.androsz.electricsleep.app.CustomTitlebarActivity implements
		OnItemClickListener {

	private class AlarmTimeAdapter extends CursorAdapter {
		public AlarmTimeAdapter(final Context context, final Cursor cursor) {
			super(context, cursor);
		}

		@Override
		public void bindView(final View view, final Context context,
				final Cursor cursor) {
			final Alarm alarm = new Alarm(cursor);

			final View indicator = view.findViewById(R.id.indicator);
			indicator.setBackgroundColor(android.R.color.transparent);

			// Set the initial resource for the bar image.
			final ImageView barOnOff = (ImageView) indicator
					.findViewById(R.id.bar_onoff);
			barOnOff.setImageResource(alarm.enabled ? R.drawable.ic_indicator_on
					: R.drawable.ic_indicator_off);

			// Set the initial state of the clock "checkbox"
			final CheckBox clockOnOff = (CheckBox) indicator
					.findViewById(R.id.clock_onoff);
			clockOnOff.setChecked(alarm.enabled);

			// Clicking outside the "checkbox" should also change the state.
			indicator.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(final View v) {
					clockOnOff.toggle();
					updateIndicatorAndAlarm(clockOnOff.isChecked(), barOnOff,
							alarm);
				}
			});

			final DigitalClock digitalClock = (DigitalClock) view
					.findViewById(R.id.digitalClock);

			// set the alarm text
			final Calendar c = Calendar.getInstance();
			c.set(Calendar.HOUR_OF_DAY, alarm.hour);
			c.set(Calendar.MINUTE, alarm.minutes);
			digitalClock.updateTime(c);
			digitalClock.setTypeface(Typeface.DEFAULT);

			// Set the repeat text or leave it blank if it does not repeat.
			final TextView daysOfWeekView = (TextView) digitalClock
					.findViewById(R.id.daysOfWeek);
			final String daysOfWeekStr = alarm.daysOfWeek.toString(
					AlarmClock.this, false);
			if (daysOfWeekStr != null && daysOfWeekStr.length() != 0) {
				daysOfWeekView.setText(daysOfWeekStr);
				daysOfWeekView.setVisibility(View.VISIBLE);
			} else {
				daysOfWeekView.setVisibility(View.GONE);
			}

			// Display the label
			final TextView labelView = (TextView) view.findViewById(R.id.label);
			if (alarm.label != null && alarm.label.length() != 0) {
				labelView.setText(alarm.label);
				labelView.setVisibility(View.VISIBLE);
			} else {
				labelView.setVisibility(View.GONE);
			}
		}

		@Override
		public View newView(final Context context, final Cursor cursor,
				final ViewGroup parent) {
			final View ret = mFactory.inflate(R.layout.alarm_time, parent,
					false);

			final DigitalClock digitalClock = (DigitalClock) ret
					.findViewById(R.id.digitalClock);
			digitalClock.setLive(false);
			return ret;
		}
	}

	/**
	 * This must be false for production. If true, turns on logging, test code,
	 * etc.
	 */
	static final boolean DEBUG = false;
	private SharedPreferences mPrefs;
	private LayoutInflater mFactory;
	private ListView mAlarmsList;

	private Cursor mCursor;

	private void addNewAlarm() {
		startActivity(new Intent(this, SetAlarm.class));
	};

	@Override
	protected int getContentAreaLayoutId() {
		// TODO Auto-generated method stub
		return R.layout.alarm_clock;
	}

	@Override
	public boolean onContextItemSelected(final MenuItem item) {
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		final int id = (int) info.id;
		switch (item.getItemId()) {
		case R.id.delete_alarm:
			// Confirm that the alarm will be deleted.
			new AlertDialog.Builder(this)
					.setTitle(getString(R.string.delete_alarm))
					.setMessage(getString(R.string.delete_alarm_confirm))
					.setPositiveButton(android.R.string.ok,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(final DialogInterface d,
										final int w) {
									Alarms.deleteAlarm(AlarmClock.this, id);
								}
							}).setNegativeButton(android.R.string.cancel, null)
					.show();
			return true;

		case R.id.enable_alarm:
			final Cursor c = (Cursor) mAlarmsList.getAdapter().getItem(
					info.position);
			final Alarm alarm = new Alarm(c);
			Alarms.enableAlarm(this, alarm.id, !alarm.enabled);
			if (!alarm.enabled) {
				SetAlarm.popAlarmSetToast(this, alarm.hour, alarm.minutes,
						alarm.daysOfWeek);
			}
			return true;

		case R.id.edit_alarm:
			final Intent intent = new Intent(this, SetAlarm.class);
			intent.putExtra(Alarms.ALARM_ID, id);
			startActivity(intent);
			return true;

		default:
			break;
		}
		return super.onContextItemSelected(item);
	}

	@Override
	protected void onCreate(final Bundle icicle) {
		super.onCreate(icicle);

		mFactory = LayoutInflater.from(this);
		mPrefs = getSharedPreferences(SettingsActivity.PREFERENCES, 0);
		mCursor = Alarms.getAlarmsCursor(getContentResolver());
		mCursor.deactivate();

		updateLayout();
	}

	@Override
	public void onCreateContextMenu(final ContextMenu menu, final View view,
			final ContextMenuInfo menuInfo) {
		// Inflate the menu from xml.
		getMenuInflater().inflate(R.menu.context_menu, menu);

		// Use the current item to create a custom view for the header.
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		final Cursor c = (Cursor) mAlarmsList.getAdapter().getItem(
				info.position);
		final Alarm alarm = new Alarm(c);

		// Construct the Calendar to compute the time.
		final Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, alarm.hour);
		cal.set(Calendar.MINUTE, alarm.minutes);
		final String time = Alarms.formatTime(this, cal);

		// Inflate the custom view and set each TextView's text.
		final View v = mFactory.inflate(R.layout.context_menu_header, null);
		TextView textView = (TextView) v.findViewById(R.id.header_time);
		textView.setText(time);
		textView = (TextView) v.findViewById(R.id.header_label);
		textView.setText(alarm.label);

		// Set the custom view on the menu.
		menu.setHeaderView(v);
		// Change the text based on the state of the alarm.
		if (alarm.enabled) {
			menu.findItem(R.id.enable_alarm).setTitle(R.string.disable_alarm);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		ToastMaster.cancelToast();
		mCursor.close();
	}

	@Override
	public void onItemClick(final AdapterView parent, final View v,
			final int pos, final long id) {
		final Intent intent = new Intent(this, SetAlarm.class);
		intent.putExtra(Alarms.ALARM_ID, (int) id);
		startActivity(intent);
	}

	private void updateIndicatorAndAlarm(final boolean enabled,
			final ImageView bar, final Alarm alarm) {
		bar.setImageResource(enabled ? R.drawable.ic_indicator_on
				: R.drawable.ic_indicator_off);
		Alarms.enableAlarm(this, alarm.id, enabled);
		if (enabled) {
			SetAlarm.popAlarmSetToast(this, alarm.hour, alarm.minutes,
					alarm.daysOfWeek);
		}
	}

	private void updateLayout() {
		mAlarmsList = (ListView) findViewById(R.id.alarms_list);
		mCursor.requery();
		final AlarmTimeAdapter adapter = new AlarmTimeAdapter(this, mCursor);
		mAlarmsList.setAdapter(adapter);
		mAlarmsList.setVerticalScrollBarEnabled(true);
		mAlarmsList.setOnItemClickListener(this);
		mAlarmsList.setOnCreateContextMenuListener(this);
		mAlarmsList.setCacheColorHint(0);
		mAlarmsList.setBackgroundDrawable(getResources().getDrawable(
				R.drawable.gradient_background_vert));

		final View addAlarm = findViewById(R.id.add_alarm);
		addAlarm.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				addNewAlarm();
			}
		});
		// Make the entire view selected when focused.
		addAlarm.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(final View v, final boolean hasFocus) {
				v.setSelected(hasFocus);
			}
		});
	}
}
