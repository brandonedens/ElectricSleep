package com.androsz.electricsleep.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.androsz.electricsleepdonate.R;
import com.androsz.electricsleep.db.SleepSession;
import com.androsz.electricsleep.widget.SleepChart;

public class ReviewSleepChartFragment extends HostFragment {

	SleepChart sleepChart;

	SleepSession sleepRecord;

	@Override
	protected int getContentAreaLayoutId() {
		return R.layout.fragment_review_sleep_chart;
	}

	@Override
	public void onClick(View v) {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View view = super.onCreateView(inflater, container, savedInstanceState);

		sleepChart = (SleepChart) view.findViewById(R.id.sleep_movement_chart);
		if (sleepRecord != null) {
			setSleepRecord(sleepRecord);
		}

		return view;
	}

	public void setSleepRecord(SleepSession sleepRecord) {
		this.sleepRecord = sleepRecord;
		if (sleepChart != null) {
			sleepChart.sync(sleepRecord);
		}
	}

}
