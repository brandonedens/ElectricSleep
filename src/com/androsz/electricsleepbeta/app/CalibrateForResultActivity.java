package com.androsz.electricsleepbeta.app;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.support.v4.app.Fragment;

public abstract class CalibrateForResultActivity extends Fragment {
	public static final int CALIBRATION_FAILED = -0x1337;

	public static final int CALIBRATION_SUCCEEDED = 1;

	protected abstract Intent getAssociatedServiceIntent();

	//@Override
	//public void onBackPressed() {
	//	setFailed();
	//}

	@Override
	public void onConfigurationChanged(final Configuration newConfig) {
		// getParent().onConfigurationChanged(newConfig);
		// do nothing to prevent onWindowFocusChanged to be called and
		// subsequent failure of the test
	}

	//private void setFailed() {
	//	this.setResult(CALIBRATION_FAILED);
	//	getActivity().stopService(getAssociatedServiceIntent());
	//	finish();
	//}
}
