package com.androsz.electricsleepbeta.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.format.Time;
import android.view.View;
import android.widget.ViewSwitcher;

import com.androsz.electricsleepbeta.R;
import com.androsz.electricsleepbeta.widget.calendar.MonthView;
import com.androsz.electricsleepbeta.widget.calendar.Utils;
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
		
		((ViewPager)findViewById(R.id.viewpager)).setAdapter(new HomePagerAdapter(getSupportFragmentManager()));
	}

	private class HomePagerAdapter extends FragmentPagerAdapter implements TitleProvider {

		public HomePagerAdapter(FragmentManager fm) {
			super(fm);
		}
		
		private String[] titles = new String[] { "", "", "" };

		public String[] getTitles() {
			return titles;
		}

		public void setTitles(String[] titles) {
			this.titles = titles.clone();
		}

		@Override
		public String getTitle(int position) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Fragment getItem(int position) {
			switch (position) {
			case 0:
				return new HomeFragment();
			default:
				return null;
			}
		}

		@Override
		public int getCount() {
			return 1;
		}
	}

	private static class ViewpagerAdapter extends FragmentPagerAdapter {
		protected static final String[] HEADERS = new String[] { "This", "Is", "A", "Test", };

		private int mCount = HEADERS.length;

		public ViewpagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			Fragment fragment = null;
			switch (position) {

			}
			return fragment;
		}

		@Override
		public int getCount() {
			return mCount;
		}

		public void setCount(int count) {
			if (count > 0 && count <= 10) {
				mCount = count;
				notifyDataSetChanged();
			}
		}
	}

}
