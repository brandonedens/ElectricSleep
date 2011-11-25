package com.androsz.electricsleepbeta.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

import com.androsz.electricsleepbeta.R;
import com.androsz.electricsleepbeta.alarmclock.AlarmClock;
import com.viewpagerindicator.TitlePageIndicator;
import com.viewpagerindicator.TitleProvider;

public class HomeActivity extends HostActivity {

	@Override
	protected int getContentAreaLayoutId() {
		// TODO Auto-generated method stub
		return R.layout.activity_newhome;
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		final ActionBar bar = getSupportActionBar();
		bar.setDisplayHomeAsUpEnabled(false);
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				PreferenceManager.setDefaultValues(HomeActivity.this, R.xml.settings, false);
				final SharedPreferences userPrefs = getSharedPreferences(
						SettingsActivity.PREFERENCES_ENVIRONMENT, Context.MODE_PRIVATE);
				final int prefsVersion = userPrefs.getInt(SettingsActivity.PREFERENCES_ENVIRONMENT,
						0);
				if (prefsVersion == 0) {
					startActivity(new Intent(HomeActivity.this, WelcomeTutorialWizardActivity.class)
							.putExtra("required", true));
				} else {

					if (WelcomeTutorialWizardActivity
							.enforceCalibrationBeforeStartingSleep(HomeActivity.this)) {
					}
				}
				return null;
			}
		}.execute();
		ViewPager viewPager = ((ViewPager)findViewById(R.id.viewpager));
		viewPager.setAdapter(new HomePagerAdapter(getSupportFragmentManager()));

		final TitlePageIndicator indicator = (TitlePageIndicator) findViewById(R.id.indicator);
		indicator.setFooterColor(getResources().getColor(R.color.primary1));
		indicator.setViewPager(viewPager, 1);
	}

	private class HomePagerAdapter extends FragmentPagerAdapter implements TitleProvider {

		public HomePagerAdapter(FragmentManager fm) {
			super(fm);
		}
		
		private int[] titles = new int[] { R.string.app_name, R.string.title_history, R.string.pref_alarms};

		@Override
		public String getTitle(int position) {
			// TODO Auto-generated method stub
			return getString(titles[position]);
		}

		@Override
		public Fragment getItem(int position) {
			switch (position) {
			case 0:
				return new HistoryMonthFragment();
			case 1:
				return new HomeFragment();
			case 2:
				return new AlarmClock();
			default:
				return null;
			}
		}

		@Override
		public int getCount() {
			return titles.length;
		}
	}
}
