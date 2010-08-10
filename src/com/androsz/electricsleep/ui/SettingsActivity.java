package com.androsz.electricsleep.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ListView;

import com.androsz.electricsleep.R;

public class SettingsActivity extends CustomTitlebarPreferenceActivity {

	@Override
	protected void onDestroy() {
		super.onDestroy();

		final SharedPreferences.Editor ed = getSharedPreferences(
				getString(R.string.prefs_version), Context.MODE_PRIVATE).edit();
		ed.putInt(getString(R.string.prefs_version), getResources().getInteger(
				R.integer.prefs_version));
		ed.commit();
	}

	public static boolean areSensitivitiesValid(int minSensitivity,
			int maxSensitivity, int alarmSensitivity) {
		if (maxSensitivity < 0 || minSensitivity < 0 || alarmSensitivity < 0) {
			return false;
		}

		if (minSensitivity > alarmSensitivity
				|| minSensitivity > maxSensitivity) {
			return false;
		}

		if (alarmSensitivity > maxSensitivity
				|| alarmSensitivity < minSensitivity) {
			return false;
		}

		if (maxSensitivity < alarmSensitivity
				|| maxSensitivity < minSensitivity) {
			return false;
		}

		return true;
	}

	@Override
	protected int getContentAreaLayoutId() {
		// TODO Auto-generated method stub
		return R.xml.settings;
	}
}
