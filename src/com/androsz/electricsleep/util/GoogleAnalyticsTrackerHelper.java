package com.androsz.electricsleep.util;

public interface GoogleAnalyticsTrackerHelper {
	public void trackEvent(final String label, final int value);
	public void trackPageView(final String pageUrl);
}
