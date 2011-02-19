/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.androsz.electricsleep.widget.calendar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.provider.BaseColumns;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.androsz.electricsleepdonate.R;
import com.androsz.electricsleep.app.HistoryActivity;
import com.androsz.electricsleep.app.ReviewSleepActivity;
import com.androsz.electricsleep.db.SleepContentProvider;
import com.androsz.electricsleep.db.SleepHistoryDatabase;
import com.androsz.electricsleep.db.SleepRecord;

public class MonthView extends View {

	class DismissPopup implements Runnable {
		@Override
		public void run() {
			mPopup.dismiss();
		}
	}

	private static float mScale = 0; // Used for supporting different screen
	// densities
	private static int WEEK_GAP = 0;
	private static int MONTH_DAY_GAP = 1;
	private static float HOUR_GAP = 0f;
	private static float MIN_EVENT_HEIGHT = 1f;
	private static int MONTH_DAY_TEXT_SIZE = 20;
	private static int WEEK_BANNER_HEIGHT = 17;
	private static int WEEK_TEXT_SIZE = 15;
	private static int WEEK_TEXT_PADDING = 3;
	private static int EVENT_DOT_TOP_MARGIN = 5;
	private static int EVENT_DOT_LEFT_MARGIN = 7;
	private static int EVENT_DOT_W_H = 10;
	private static int EVENT_NUM_DAYS = 31;
	private static int TEXT_TOP_MARGIN = 7;
	private static int BUSY_BITS_WIDTH = 10;
	private static int BUSY_BITS_MARGIN = 4;

	private static int DAY_NUMBER_OFFSET = 10;

	private static int HORIZONTAL_FLING_THRESHOLD = 33;
	private int mCellHeight;
	private int mBorder;

	private boolean mLaunchDayView;

	private GestureDetector mGestureDetector;
	private Time mToday;
	private Time mViewCalendar;

	private final Time mSavedTime = new Time(); // the time when we entered this
												// view

	// This Time object is used to set the time for the other Month view.
	private final Time mOtherViewCalendar = new Time();

	// This Time object is used for temporary calculations and is allocated
	// once to avoid extra garbage collection
	private final Time mTempTime = new Time();

	private DayOfMonthCursor mCursor;
	private Drawable mBoxSelected;
	private Drawable mBoxPressed;
	private Drawable mBoxLongPressed;

	private int mCellWidth;
	private Resources mResources;
	private MonthActivity mParentActivity;
	private final Navigator mNavigator;

	private final EventGeometry mEventGeometry;

	// Pre-allocate and reuse
	private final Rect mRect = new Rect();

	// An array of which days have events for quick reference
	private final boolean[] eventDay = new boolean[31];
	private PopupWindow mPopup;
	private View mPopupView;
	private static final int POPUP_HEIGHT = 100;
	private int mPreviousPopupHeight;
	private static final int POPUP_DISMISS_DELAY = 3000;

	private final DismissPopup mDismissPopup = new DismissPopup();
	// For drawing to an off-screen Canvas
	private Bitmap mBitmap;
	private Canvas mCanvas;
	private boolean mRedrawScreen = true;
	private final Rect mBitmapRect = new Rect();
	private final RectF mRectF = new RectF();

	private boolean mAnimating;
	// These booleans disable features that were taken out of the spec.
	private final boolean mShowWeekNumbers = false;

	private final boolean mShowToast = false;

	// Bitmap caches.
	// These improve performance by minimizing calls to NinePatchDrawable.draw()
	// for common
	// drawables for day backgrounds.
	// mDayBitmapCache is indexed by a unique integer constructed from the
	// width/height.
	private final SparseArray<Bitmap> mDayBitmapCache = new SparseArray<Bitmap>(
			4);
	/**
	 * The selection modes are HIDDEN, PRESSED, SELECTED, and LONGPRESS.
	 */
	private static final int SELECTION_HIDDEN = 0;
	private static final int SELECTION_PRESSED = 1;
	private static final int SELECTION_SELECTED = 2;

	private static final int SELECTION_LONGPRESS = 3;

	private int mSelectionMode = SELECTION_HIDDEN;

	/**
	 * The first Julian day of the current month.
	 */
	private int mFirstJulianDay;

	private int mStartDay;

	private final EventLoader mEventLoader;

	private ArrayList<SleepRecord> mEvents = new ArrayList<SleepRecord>();

	private Drawable mTodayBackground;
	// Cached colors
	private int mMonthOtherMonthColor;
	private int mMonthWeekBannerColor;
	private int mMonthOtherMonthBannerColor;
	private int mMonthOtherMonthDayNumberColor;
	private int mMonthDayNumberColor;
	private int mMonthTodayNumberColor;
	private int mMonthSaturdayColor;
	private int mMonthSundayColor;

	// private int mMonthBgColor;

	private int mBusybitsColor;

	public MonthView(MonthActivity activity, Navigator navigator) {
		super(activity);
		if (mScale == 0) {
			mScale = getContext().getResources().getDisplayMetrics().density;
			if (mScale != 1) {
				WEEK_GAP *= mScale;
				MONTH_DAY_GAP *= mScale;
				HOUR_GAP *= mScale;

				MONTH_DAY_TEXT_SIZE *= mScale;
				WEEK_BANNER_HEIGHT *= mScale;
				WEEK_TEXT_SIZE *= mScale;
				WEEK_TEXT_PADDING *= mScale;
				EVENT_DOT_TOP_MARGIN *= mScale;
				EVENT_DOT_LEFT_MARGIN *= mScale;
				EVENT_DOT_W_H *= mScale;
				TEXT_TOP_MARGIN *= mScale;
				HORIZONTAL_FLING_THRESHOLD *= mScale;
				MIN_EVENT_HEIGHT *= mScale;
				BUSY_BITS_WIDTH *= mScale;
				BUSY_BITS_MARGIN *= mScale;
				DAY_NUMBER_OFFSET *= mScale;
			}
		}

		mEventLoader = activity.mEventLoader;
		mNavigator = navigator;
		mEventGeometry = new EventGeometry();
		mEventGeometry.setMinEventHeight(MIN_EVENT_HEIGHT);
		mEventGeometry.setHourGap(HOUR_GAP);
		init(activity);
	}

	void animationFinished() {
		mAnimating = false;
		mRedrawScreen = true;
		invalidate();
	}

	void animationStarted() {
		mAnimating = true;
	}

	/**
	 * Clears the bitmap cache. Generally only needed when the screen size
	 * changed.
	 */
	private void clearBitmapCache() {
		recycleAndClearBitmapCache(mDayBitmapCache);
	}

	// This is called when the activity is paused so that the popup can
	// be dismissed.
	void dismissPopup() {
		if (!mShowToast)
			return;

		// Protect against null-pointer exceptions
		if (mPopup != null) {
			mPopup.dismiss();
		}

		final Handler handler = getHandler();
		if (handler != null) {
			handler.removeCallbacks(mDismissPopup);
		}
	}

	private void doDraw(Canvas canvas) {
		final boolean isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

		final Paint p = new Paint();
		final Rect r = mRect;
		final int columnDay1 = mCursor.getColumnOf(1);

		// Get the Julian day for the date at row 0, column 0.
		int day = mFirstJulianDay - columnDay1;

		int weekNum = 0;
		Calendar calendar = null;
		if (mShowWeekNumbers) {
			calendar = Calendar.getInstance();
			final boolean noPrevMonth = (columnDay1 == 0);

			// Compute the week number for the first row.
			weekNum = getWeekOfYear(0, 0, noPrevMonth, calendar);
		}

		for (int row = 0; row < 6; row++) {
			for (int column = 0; column < 7; column++) {
				drawBox(day, weekNum, row, column, canvas, p, r, isLandscape);
				day += 1;
			}

			if (mShowWeekNumbers) {
				weekNum += 1;
				if (weekNum >= 53) {
					final boolean inCurrentMonth = (day - mFirstJulianDay < 31);
					weekNum = getWeekOfYear(row + 1, 0, inCurrentMonth,
							calendar);
				}
			}
		}

		drawGrid(canvas, p);
	}

	/**
	 * Draw a single box onto the canvas.
	 * 
	 * @param day
	 *            The Julian day.
	 * @param weekNum
	 *            The week number.
	 * @param row
	 *            The row of the box (0-5).
	 * @param column
	 *            The column of the box (0-6).
	 * @param canvas
	 *            The canvas to draw on.
	 * @param p
	 *            The paint used for drawing.
	 * @param r
	 *            The rectangle used for each box.
	 * @param isLandscape
	 *            Is the current orientation landscape.
	 */
	private void drawBox(int day, int weekNum, int row, int column,
			Canvas canvas, Paint p, Rect r, boolean isLandscape) {

		// Only draw the selection if we are in the press state or if we have
		// moved the cursor with key input.
		boolean drawSelection = false;
		if (mSelectionMode != SELECTION_HIDDEN) {
			drawSelection = mCursor.isSelected(row, column);
		}

		final boolean withinCurrentMonth = mCursor.isWithinCurrentMonth(row,
				column);
		boolean isToday = false;
		final int dayOfBox = mCursor.getDayAt(row, column);
		if (dayOfBox == mToday.monthDay && mCursor.getYear() == mToday.year
				&& mCursor.getMonth() == mToday.month) {
			isToday = true;
		}

		final int y = WEEK_GAP + row * (WEEK_GAP + mCellHeight);
		final int x = mBorder + column * (MONTH_DAY_GAP + mCellWidth);

		r.left = x;
		r.top = y;
		r.right = x + mCellWidth;
		r.bottom = y + mCellHeight;

		// Adjust the left column, right column, and bottom row to leave
		// no border.
		if (column == 0) {
			r.left = -1;
		} else if (column == 6) {
			r.right += mBorder + 2;
		}

		if (row == 5) {
			r.bottom = getMeasuredHeight();
		}

		// Draw the cell contents (excluding monthDay number)
		if (!withinCurrentMonth) {
			// Adjust cell boundaries to compensate for the different border
			// style.
			r.top--;
			if (column != 0) {
				r.left--;
			}
			p.setStyle(Style.FILL);
			p.setColor(mMonthOtherMonthColor);
			canvas.drawRect(r, p);
		} else if (drawSelection) {
			if (mSelectionMode == SELECTION_SELECTED) {
				mBoxSelected.setBounds(r);
				mBoxSelected.draw(canvas);
			} else if (mSelectionMode == SELECTION_PRESSED) {
				mBoxPressed.setBounds(r);
				mBoxPressed.draw(canvas);
			} else {
				mBoxLongPressed.setBounds(r);
				mBoxLongPressed.draw(canvas);
			}

			// Places events for that day
			drawEvents(day, canvas, r, p, false /* draw bb background */);
			if (!mAnimating) {
				updateEventDetails(day);
			}
		} else {
			// Today gets a different background
			if (isToday) {
				// We could cache this for a little bit more performance, but
				// it's not on the
				// performance radar...
				final Drawable background = mTodayBackground;
				background.setBounds(r);
				background.draw(canvas);
			}
			// Places events for that day
			drawEvents(day, canvas, r, p, !isToday /* draw bb background */);
		}

		// Draw week number
		if (mShowWeekNumbers && column == 0) {
			// Draw the banner
			p.setStyle(Paint.Style.FILL);
			p.setColor(mMonthWeekBannerColor);
			if (isLandscape) {
				final int bottom = r.bottom;
				r.bottom = r.top + WEEK_BANNER_HEIGHT;
				r.left++;
				canvas.drawRect(r, p);
				r.bottom = bottom;
				r.left--;
			} else {
				final int top = r.top;
				r.top = r.bottom - WEEK_BANNER_HEIGHT;
				r.left++;
				canvas.drawRect(r, p);
				r.top = top;
				r.left--;
			}

			// Draw the number
			p.setColor(mMonthOtherMonthBannerColor);
			p.setAntiAlias(true);
			p.setTypeface(null);
			p.setTextSize(WEEK_TEXT_SIZE);
			p.setTextAlign(Paint.Align.LEFT);

			final int textX = r.left + WEEK_TEXT_PADDING;
			int textY;
			if (isLandscape) {
				textY = r.top + WEEK_BANNER_HEIGHT - WEEK_TEXT_PADDING;
			} else {
				textY = r.bottom - WEEK_TEXT_PADDING;
			}

			canvas.drawText(String.valueOf(weekNum), textX, textY, p);
		}

		// Draw the monthDay number
		p.setStyle(Paint.Style.FILL);
		p.setAntiAlias(true);
		p.setTypeface(null);
		p.setTextSize(MONTH_DAY_TEXT_SIZE);

		if (!withinCurrentMonth) {
			p.setColor(mMonthOtherMonthDayNumberColor);
		} else {
			if (isToday && !drawSelection) {
				p.setColor(mMonthTodayNumberColor);
			} else if (Utils.isSunday(column, mStartDay)) {
				p.setColor(mMonthSundayColor);
			} else if (Utils.isSaturday(column, mStartDay)) {
				p.setColor(mMonthSaturdayColor);
			} else {
				p.setColor(mMonthDayNumberColor);
			}
			// bolds the day if there's an event that day
			p.setFakeBoldText(eventDay[day - mFirstJulianDay]);
		}
		/*
		 * Drawing of day number is done hereeasy to find tags draw number draw
		 * day
		 */
		p.setTextAlign(Paint.Align.CENTER);
		// center of text
		final int textX = r.left
				+ (r.right - BUSY_BITS_MARGIN - BUSY_BITS_WIDTH - r.left) / 2;
		final int textY = (int) (r.top + p.getTextSize() + TEXT_TOP_MARGIN); // bottom
		// of
		// text
		canvas.drawText(String.valueOf(mCursor.getDayAt(row, column)), textX,
				textY, p);
	}

	// Draw busybits for a single event
	private RectF drawEventRect(Rect rect, SleepRecord event, Canvas canvas,
			Paint p) {

		p.setColor(mBusybitsColor);

		final int left = rect.right - BUSY_BITS_MARGIN - BUSY_BITS_WIDTH;
		final int bottom = rect.bottom - BUSY_BITS_MARGIN;

		final RectF rf = mRectF;
		rf.top = event.top;
		// Make sure we don't go below the bottom of the bb bar
		rf.bottom = Math.min(event.bottom, bottom);
		rf.left = left;
		rf.right = left + BUSY_BITS_WIDTH;

		canvas.drawRect(rf, p);

		return rf;
	}

	// /Create and draw the event busybits for this day
	private void drawEvents(int date, Canvas canvas, Rect rect, Paint p,
			boolean drawBg) {
		// The top of the busybits section lines up with the top of the day
		// number
		final int top = rect.top + TEXT_TOP_MARGIN + BUSY_BITS_MARGIN;
		final int left = rect.right - BUSY_BITS_MARGIN - BUSY_BITS_WIDTH;

		final ArrayList<SleepRecord> events = mEvents;
		final int numEvents = events.size();
		final EventGeometry geometry = mEventGeometry;

		if (drawBg) {
			final RectF rf = mRectF;
			rf.left = left;
			rf.right = left + BUSY_BITS_WIDTH;
			rf.bottom = rect.bottom - BUSY_BITS_MARGIN;
			rf.top = top;

			p.setColor(this.mMonthOtherMonthColor);
			p.setStyle(Style.FILL);
			canvas.drawRect(rf, p);
		}

		for (int i = 0; i < numEvents; i++) {
			final SleepRecord event = events.get(i);
			if (!geometry.computeEventRect(date, left, top, BUSY_BITS_WIDTH,
					event)) {
				continue;
			}
			drawEventRect(rect, event, canvas, p);
		}

	}

	/**
	 * Draw the grid lines for the calendar
	 * 
	 * @param canvas
	 *            The canvas to draw on.
	 * @param p
	 *            The paint used for drawing.
	 */
	private void drawGrid(Canvas canvas, Paint p) {
		p.setColor(Color.WHITE);
		p.setAntiAlias(false);

		final int width = getMeasuredWidth();
		final int height = getMeasuredHeight();

		for (int row = 0; row < 6; row++) {
			final int y = WEEK_GAP + row * (WEEK_GAP + mCellHeight) - 1;
			canvas.drawLine(0, y, width, y, p);
		}
		for (int column = 1; column < 7; column++) {
			final int x = mBorder + column * (MONTH_DAY_GAP + mCellWidth) - 1;
			canvas.drawLine(x, WEEK_GAP, x, height, p);
		}
	}

	private void drawingCalc(int width, int height) {
		mCellHeight = (height - (6 * WEEK_GAP)) / 6;
		mEventGeometry
				.setHourHeight((mCellHeight - BUSY_BITS_MARGIN * 2 - TEXT_TOP_MARGIN) / 24.0f);
		mCellWidth = (width - (6 * MONTH_DAY_GAP)) / 7;
		mBorder = (width - 6 * (mCellWidth + MONTH_DAY_GAP) - mCellWidth) / 2;

		if (mShowToast) {
			mPopup.dismiss();
			mPopup.setWidth(width - 20);
			mPopup.setHeight(POPUP_HEIGHT);
		}

		if (((mBitmap == null) || mBitmap.isRecycled()
				|| (mBitmap.getHeight() != height) || (mBitmap.getWidth() != width))
				&& (width > 0) && (height > 0)) {
			if (mBitmap != null) {
				mBitmap.recycle();
			}
			mBitmap = Bitmap.createBitmap(width, height,
					Bitmap.Config.ARGB_8888);
			mCanvas = new Canvas(mBitmap);
		}

		mBitmapRect.top = 0;
		mBitmapRect.bottom = height;
		mBitmapRect.left = 0;
		mBitmapRect.right = width;
	}

	private long getSelectedMillisFor(int x, int y) {
		final int row = (y - WEEK_GAP) / (WEEK_GAP + mCellHeight);
		int column = (x - mBorder) / (MONTH_DAY_GAP + mCellWidth);
		if (column > 6) {
			column = 6;
		}

		final DayOfMonthCursor c = mCursor;
		final Time time = mTempTime;
		time.set(mViewCalendar);
		time.set(0, 0, 0, time.monthDay, time.month, time.year);

		// Compute the day number from the row and column. If the row and
		// column are in a different month from the current one, then the
		// monthDay might be negative or it might be greater than the number
		// of days in this month, but that is okay because the normalize()
		// method will adjust the month (and year) if necessary.
		time.monthDay = 7 * row + column - c.getOffset() + 1;
		return time.normalize(true);
	}

	public long getSelectedTimeInMillis() {
		final Time time = mTempTime;
		time.set(mViewCalendar);

		time.month += mCursor.getSelectedMonthOffset();
		time.monthDay = mCursor.getSelectedDayOfMonth();

		// Restore the saved hour:minute:second offset from when we entered
		// this view.
		time.second = mSavedTime.second;
		time.minute = mSavedTime.minute;
		time.hour = mSavedTime.hour;
		return time.normalize(true);
	}

	public int getSelectionMode() {
		return mSelectionMode;
	}

	Time getTime() {
		return mViewCalendar;
	}

	private int getWeekOfYear(int row, int column,
			boolean isWithinCurrentMonth, Calendar calendar) {
		calendar.set(Calendar.DAY_OF_MONTH, mCursor.getDayAt(row, column));
		if (isWithinCurrentMonth) {
			calendar.set(Calendar.MONTH, mCursor.getMonth());
			calendar.set(Calendar.YEAR, mCursor.getYear());
		} else {
			int month = mCursor.getMonth();
			int year = mCursor.getYear();
			if (row < 2) {
				// Previous month
				if (month == 0) {
					year--;
					month = 11;
				} else {
					month--;
				}
			} else {
				// Next month
				if (month == 11) {
					year++;
					month = 0;
				} else {
					month++;
				}
			}
			calendar.set(Calendar.MONTH, month);
			calendar.set(Calendar.YEAR, year);
		}

		return calendar.get(Calendar.WEEK_OF_YEAR);
	}

	private void init(MonthActivity activity) {
		setFocusable(true);
		setClickable(true);
		mParentActivity = activity;
		mViewCalendar = new Time();
		final long now = System.currentTimeMillis();
		mViewCalendar.set(now);
		mViewCalendar.monthDay = 1;
		final long millis = mViewCalendar.normalize(true /* ignore DST */);
		mFirstJulianDay = Time.getJulianDay(millis, mViewCalendar.gmtoff);
		mStartDay = Utils.getFirstDayOfWeek();
		mViewCalendar.set(now);

		mCursor = new DayOfMonthCursor(mViewCalendar.year, mViewCalendar.month,
				mViewCalendar.monthDay, mParentActivity.getStartDay());
		mToday = new Time();
		mToday.set(System.currentTimeMillis());

		mResources = activity.getResources();
		mBoxSelected = mResources.getDrawable(R.drawable.month_view_selected);
		mBoxPressed = mResources.getDrawable(R.drawable.month_view_pressed);
		mBoxLongPressed = mResources
				.getDrawable(R.drawable.month_view_longpress);

		mTodayBackground = mResources
				.getDrawable(R.drawable.month_view_today_background);

		// Cache color lookups
		final Resources res = getResources();
		mMonthOtherMonthColor = res.getColor(R.color.month_other_month);
		mMonthWeekBannerColor = res.getColor(R.color.month_week_banner);
		mMonthOtherMonthBannerColor = res
				.getColor(R.color.month_other_month_banner);
		mMonthOtherMonthDayNumberColor = res
				.getColor(R.color.month_other_month_day_number);
		mMonthDayNumberColor = res.getColor(R.color.month_day_number);
		mMonthTodayNumberColor = res.getColor(R.color.month_today_number);
		mMonthSaturdayColor = res.getColor(R.color.month_saturday);
		mMonthSundayColor = res.getColor(R.color.month_sunday);
		mBusybitsColor = res.getColor(R.color.primary1);

		if (mShowToast) {
			LayoutInflater inflater;
			inflater = (LayoutInflater) activity
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mPopupView = inflater.inflate(R.layout.month_bubble, null);
			mPopup = new PopupWindow(activity);
			mPopup.setContentView(mPopupView);
			final Resources.Theme dialogTheme = getResources().newTheme();
			dialogTheme.applyStyle(android.R.style.Theme_Dialog, true);
			final TypedArray ta = dialogTheme
					.obtainStyledAttributes(new int[] { android.R.attr.windowBackground });
			mPopup.setBackgroundDrawable(ta.getDrawable(0));
			ta.recycle();
		}

		mGestureDetector = new GestureDetector(getContext(),
				new GestureDetector.SimpleOnGestureListener() {
					@Override
					public boolean onDown(MotionEvent e) {
						// Launch the Day/Agenda view when the finger lifts up,
						// unless the finger moves before lifting up (onFling or
						// onScroll).
						mLaunchDayView = true;
						return true;
					}

					@Override
					public boolean onFling(MotionEvent e1, MotionEvent e2,
							float velocityX, float velocityY) {
						// The user might do a slow "fling" after touching the
						// screen
						// and we don't want the long-press to pop up a context
						// menu.
						// Setting mLaunchDayView to false prevents the
						// long-press.
						mLaunchDayView = false;
						mSelectionMode = SELECTION_HIDDEN;

						final int distanceX = Math.abs((int) e2.getX()
								- (int) e1.getX());
						final int distanceY = Math.abs((int) e2.getY()
								- (int) e1.getY());
						if (distanceY < HORIZONTAL_FLING_THRESHOLD
								|| distanceY < distanceX)
							return false;

						// Switch to a different month
						final Time time = mOtherViewCalendar;
						time.set(mViewCalendar);
						if (velocityY < 0) {
							time.month += 1;
						} else {
							time.month -= 1;
						}
						time.normalize(true);
						mParentActivity.goTo(time, true);

						return true;
					}

					@Override
					public void onLongPress(MotionEvent e) {
						// If mLaunchDayView is true, then we haven't done any
						// scrolling
						// after touching the screen, so allow long-press to
						// proceed
						// with popping up the context menu.
						if (mLaunchDayView) {
							mLaunchDayView = false;
							mSelectionMode = SELECTION_LONGPRESS;
							mRedrawScreen = true;
							invalidate();
							performLongClick();
						}
					}

					@Override
					public boolean onScroll(MotionEvent e1, MotionEvent e2,
							float distanceX, float distanceY) {
						// If the user moves his finger after touching, then do
						// not
						// launch the Day view when he lifts his finger. Also,
						// turn
						// off the selection.
						mLaunchDayView = false;

						if (mSelectionMode != SELECTION_HIDDEN) {
							mSelectionMode = SELECTION_HIDDEN;
							mRedrawScreen = true;
							invalidate();
						}
						return true;
					}

					@Override
					public void onShowPress(MotionEvent e) {
						// Highlight the selected day.
						setSelectedCell(e);
						mSelectionMode = SELECTION_PRESSED;
						mRedrawScreen = true;
						invalidate();
					}

					@Override
					public boolean onSingleTapUp(MotionEvent e) {
						if (mLaunchDayView) {
							setSelectedCell(e);
							mSelectionMode = SELECTION_SELECTED;
							mRedrawScreen = true;
							invalidate();
							mLaunchDayView = false;
							final int x = (int) e.getX();
							final int y = (int) e.getY();
							final long millis = getSelectedMillisFor(x, y);

							reviewSleepIfNecessary(millis);
						}

						return true;
					}

					public void setSelectedCell(MotionEvent e) {
						final int x = (int) e.getX();
						final int y = (int) e.getY();
						int row = (y - WEEK_GAP) / (WEEK_GAP + mCellHeight);
						int col = (x - mBorder) / (MONTH_DAY_GAP + mCellWidth);
						if (row > 5) {
							row = 5;
						}
						if (col > 6) {
							col = 6;
						}

						// Highlight the selected day.
						mCursor.setSelectedRowColumn(row, col);
					}
				});
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		// No need to hang onto the bitmaps...
		clearBitmapCache();
		if (mBitmap != null) {
			mBitmap.recycle();
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (mRedrawScreen) {
			if (mCanvas == null) {
				drawingCalc(getWidth(), getHeight());
			}

			// If we are zero-sized, the canvas will remain null so check again
			if (mCanvas != null) {
				// Clear the background
				final Canvas bitmapCanvas = mCanvas;
				bitmapCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
				doDraw(bitmapCanvas);
				mRedrawScreen = false;
			}
		}

		// If we are zero-sized, the bitmap will be null so guard against this
		if (mBitmap != null) {
			canvas.drawBitmap(mBitmap, mBitmapRect, mBitmapRect, null);
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (mSelectionMode == SELECTION_HIDDEN) {
			if (keyCode == KeyEvent.KEYCODE_ENTER
					|| keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
					|| keyCode == KeyEvent.KEYCODE_DPAD_LEFT
					|| keyCode == KeyEvent.KEYCODE_DPAD_UP
					|| keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
				// Display the selection box but don't move or select it
				// on this key press.
				mSelectionMode = SELECTION_SELECTED;
				mRedrawScreen = true;
				invalidate();
				return true;
			} else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
				// Display the selection box but don't select it
				// on this key press.
				mSelectionMode = SELECTION_PRESSED;
				mRedrawScreen = true;
				invalidate();
				return true;
			}
		}

		mSelectionMode = SELECTION_SELECTED;
		boolean redraw = false;
		Time other = null;

		switch (keyCode) {
		case KeyEvent.KEYCODE_ENTER:
			final long millis = getSelectedTimeInMillis();
			reviewSleepIfNecessary(millis);
			return true;
		case KeyEvent.KEYCODE_DPAD_UP:
			if (mCursor.up()) {
				other = mOtherViewCalendar;
				other.set(mViewCalendar);
				other.month -= 1;
				other.monthDay = mCursor.getSelectedDayOfMonth();

				// restore the calendar cursor for the animation
				mCursor.down();
			}
			redraw = true;
			break;

		case KeyEvent.KEYCODE_DPAD_DOWN:
			if (mCursor.down()) {
				other = mOtherViewCalendar;
				other.set(mViewCalendar);
				other.month += 1;
				other.monthDay = mCursor.getSelectedDayOfMonth();

				// restore the calendar cursor for the animation
				mCursor.up();
			}
			redraw = true;
			break;

		case KeyEvent.KEYCODE_DPAD_LEFT:
			if (mCursor.left()) {
				other = mOtherViewCalendar;
				other.set(mViewCalendar);
				other.month -= 1;
				other.monthDay = mCursor.getSelectedDayOfMonth();

				// restore the calendar cursor for the animation
				mCursor.right();
			}
			redraw = true;
			break;

		case KeyEvent.KEYCODE_DPAD_RIGHT:
			if (mCursor.right()) {
				other = mOtherViewCalendar;
				other.set(mViewCalendar);
				other.month += 1;
				other.monthDay = mCursor.getSelectedDayOfMonth();

				// restore the calendar cursor for the animation
				mCursor.left();
			}
			redraw = true;
			break;
		}

		if (other != null) {
			other.normalize(true /* ignore DST */);
			mNavigator.goTo(other, true);
		} else if (redraw) {
			mRedrawScreen = true;
			invalidate();
		}

		return redraw;
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		final long duration = event.getEventTime() - event.getDownTime();

		switch (keyCode) {
		case KeyEvent.KEYCODE_DPAD_CENTER:
			if (mSelectionMode == SELECTION_HIDDEN) {
				// Don't do anything unless the selection is visible.
				break;
			}

			if (mSelectionMode == SELECTION_PRESSED) {
				// This was the first press when there was nothing selected.
				// Change the selection from the "pressed" state to the
				// the "selected" state. We treat short-press and
				// long-press the same here because nothing was selected.
				mSelectionMode = SELECTION_SELECTED;
				mRedrawScreen = true;
				invalidate();
				break;
			}

			// Check the duration to determine if this was a short press
			if (duration < ViewConfiguration.getLongPressTimeout()) {
				final long millis = getSelectedTimeInMillis();

				reviewSleepIfNecessary(millis);
			} else {
				mSelectionMode = SELECTION_LONGPRESS;
				mRedrawScreen = true;
				invalidate();
				performLongClick();
			}
		}
		return super.onKeyUp(keyCode, event);
	}

	@Override
	protected void onSizeChanged(int width, int height, int oldw, int oldh) {
		drawingCalc(width, height);
		// If the size changed, then we should rebuild the bitmaps...
		clearBitmapCache();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (mGestureDetector.onTouchEvent(event))
			return true;

		return super.onTouchEvent(event);
	}

	private void recycleAndClearBitmapCache(SparseArray<Bitmap> bitmapCache) {
		final int size = bitmapCache.size();
		for (int i = 0; i < size; i++) {
			bitmapCache.valueAt(i).recycle();
		}
		bitmapCache.clear();

	}

	void reloadEvents() {
		// Get the date for the beginning of the month
		final Time monthStart = mTempTime;
		monthStart.set(mViewCalendar);
		monthStart.monthDay = 1;
		monthStart.hour = 0;
		monthStart.minute = 0;
		monthStart.second = 0;
		final long millis = monthStart.normalize(true /* ignore isDst */);

		// Load the days with events in the background
		mParentActivity.showProgress();

		final ArrayList<SleepRecord> events = new ArrayList<SleepRecord>();
		mEventLoader.loadEventsInBackground(EVENT_NUM_DAYS, events, millis,
				new Runnable() {
					@Override
					public void run() {
						mEvents = events;
						mRedrawScreen = true;
						invalidate();
						final int numEvents = events.size();

						// Clear out event days
						for (int i = 0; i < EVENT_NUM_DAYS; i++) {
							eventDay[i] = false;
						}

						// Compute the new set of days with events
						for (int i = 0; i < numEvents; i++) {
							final SleepRecord event = events.get(i);
							int startDay = event.getStartJulianDay()
									- mFirstJulianDay;
							int endDay = event.getEndJulianDay()
									- mFirstJulianDay + 1;
							if (startDay < 31 || endDay >= 0) {
								if (startDay < 0) {
									startDay = 0;
								}
								if (startDay > 31) {
									startDay = 31;
								}
								if (endDay < 0) {
									endDay = 0;
								}
								if (endDay > 31) {
									endDay = 31;
								}
								for (int j = startDay; j < endDay; j++) {
									eventDay[j] = true;
								}
							}
						}
						mParentActivity.hideProgress();
					}
				}, null);
	}

	private void reviewSleepIfNecessary(long millis) {
		final ArrayList<SleepRecord> applicableEvents = new ArrayList<SleepRecord>();
		final long ONE_DAY_IN_MS = 1000 * 60 * 60 * 24;
		for (final SleepRecord event : mEvents) {
			final long startTime = event.getStartTime() - millis;
			final long endTime = event.getEndTime() - millis;
			if ((endTime > 0)
					&& ((startTime <= ONE_DAY_IN_MS && startTime > 0) || startTime < 0)) {

				applicableEvents.add(event);
			}
		}

		// if we have more than one applicable entry, then
		// open the history activity and show all entries
		// for the selected date
		if (applicableEvents.size() == 1) {
			final Intent reviewSleepIntent = new Intent(getContext(),
					ReviewSleepActivity.class);
			final SleepHistoryDatabase shdb = new SleepHistoryDatabase(
					getContext());
			// TODO: hook this into sleep db

			final Cursor c = shdb.getSleepMatches(
					applicableEvents.get(0).title, new String[] {
							BaseColumns._ID, SleepRecord.KEY_TITLE,
							SleepRecord.KEY_ALARM, SleepRecord.KEY_DURATION,
							SleepRecord.KEY_MIN, SleepRecord.KEY_NOTE,
							SleepRecord.KEY_RATING, SleepRecord.KEY_SLEEP_DATA,
							SleepRecord.KEY_SPIKES, SleepRecord.KEY_SLEEP_DATA,
							SleepRecord.KEY_TIME_FELL_ASLEEP });

			shdb.close();

			if (c == null)
				// we may have lost the cursor since the applicableEvents were
				// loaded.
				// do nothing
				return;
			final Uri data = Uri.withAppendedPath(
					SleepContentProvider.CONTENT_URI,
					String.valueOf(c.getLong(0)));
			c.close();
			reviewSleepIntent.setData(data);
			getContext().startActivity(reviewSleepIntent);
		} else if (applicableEvents.size() > 1) {
			final java.text.DateFormat sdf = java.text.DateFormat
					.getDateInstance(java.text.DateFormat.SHORT,
							Locale.getDefault());
			final Calendar calendar = Calendar.getInstance();
			;
			calendar.setTimeInMillis(millis);
			final String formattedMDY = sdf.format((calendar.getTime()));
			getContext().startActivity(
					new Intent(getContext(), HistoryActivity.class).putExtra(
							HistoryActivity.SEARCH_FOR, formattedMDY));
		}
	}

	void setSelectedTime(Time time) {
		// Save the selected time so that we can restore it later when we switch
		// views.
		mSavedTime.set(time);

		mViewCalendar.set(time);
		mViewCalendar.monthDay = 1;
		final long millis = mViewCalendar.normalize(true /* ignore DST */);
		mFirstJulianDay = Time.getJulianDay(millis, mViewCalendar.gmtoff);
		mViewCalendar.set(time);

		mCursor = new DayOfMonthCursor(time.year, time.month, time.monthDay,
				mCursor.getWeekStartDay());

		mRedrawScreen = true;
		invalidate();
	}

	public void setSelectionMode(int selectionMode) {
		mSelectionMode = selectionMode;
	}

	private void updateEventDetails(int date) {
		if (!mShowToast)
			return;

		getHandler().removeCallbacks(mDismissPopup);
		final ArrayList<SleepRecord> events = mEvents;
		final int numEvents = events.size();
		if (numEvents == 0) {
			mPopup.dismiss();
			return;
		}

		int eventIndex = 0;
		for (int i = 0; i < numEvents; i++) {
			final SleepRecord event = events.get(i);

			if (event.getStartJulianDay() > date
					|| event.getEndJulianDay() < date) {
				continue;
			}

			// If we have all the event that we can display, then just count
			// the extra ones.
			if (eventIndex >= 4) {
				eventIndex += 1;
				continue;
			}

			int flags;
			final boolean showEndTime = true;

			flags = DateUtils.FORMAT_SHOW_TIME
					| DateUtils.FORMAT_CAP_NOON_MIDNIGHT;
			if (DateFormat.is24HourFormat(mParentActivity)) {
				flags |= DateUtils.FORMAT_24HOUR;
			}

			String timeRange;
			if (showEndTime) {
				timeRange = DateUtils.formatDateRange(mParentActivity,
						event.getStartTime(), event.getEndTime(), flags);
			} else {
				timeRange = DateUtils.formatDateRange(mParentActivity,
						event.getStartTime(), event.getStartTime(), flags);
			}

			TextView timeView = null;
			TextView titleView = null;
			switch (eventIndex) {
			case 0:
				timeView = (TextView) mPopupView.findViewById(R.id.time0);
				titleView = (TextView) mPopupView
						.findViewById(R.id.event_title0);
				break;
			case 1:
				timeView = (TextView) mPopupView.findViewById(R.id.time1);
				titleView = (TextView) mPopupView
						.findViewById(R.id.event_title1);
				break;
			case 2:
				timeView = (TextView) mPopupView.findViewById(R.id.time2);
				titleView = (TextView) mPopupView
						.findViewById(R.id.event_title2);
				break;
			case 3:
				timeView = (TextView) mPopupView.findViewById(R.id.time3);
				titleView = (TextView) mPopupView
						.findViewById(R.id.event_title3);
				break;
			}

			timeView.setText(timeRange);
			titleView.setText(event.title);
			eventIndex += 1;
		}
		if (eventIndex == 0) {
			// We didn't find any events for this day
			mPopup.dismiss();
			return;
		}

		// Hide the items that have no event information
		View view;
		switch (eventIndex) {
		case 1:
			view = mPopupView.findViewById(R.id.item_layout1);
			view.setVisibility(View.GONE);
			view = mPopupView.findViewById(R.id.item_layout2);
			view.setVisibility(View.GONE);
			view = mPopupView.findViewById(R.id.item_layout3);
			view.setVisibility(View.GONE);
			view = mPopupView.findViewById(R.id.plus_more);
			view.setVisibility(View.GONE);
			break;
		case 2:
			view = mPopupView.findViewById(R.id.item_layout1);
			view.setVisibility(View.VISIBLE);
			view = mPopupView.findViewById(R.id.item_layout2);
			view.setVisibility(View.GONE);
			view = mPopupView.findViewById(R.id.item_layout3);
			view.setVisibility(View.GONE);
			view = mPopupView.findViewById(R.id.plus_more);
			view.setVisibility(View.GONE);
			break;
		case 3:
			view = mPopupView.findViewById(R.id.item_layout1);
			view.setVisibility(View.VISIBLE);
			view = mPopupView.findViewById(R.id.item_layout2);
			view.setVisibility(View.VISIBLE);
			view = mPopupView.findViewById(R.id.item_layout3);
			view.setVisibility(View.GONE);
			view = mPopupView.findViewById(R.id.plus_more);
			view.setVisibility(View.GONE);
			break;
		case 4:
			view = mPopupView.findViewById(R.id.item_layout1);
			view.setVisibility(View.VISIBLE);
			view = mPopupView.findViewById(R.id.item_layout2);
			view.setVisibility(View.VISIBLE);
			view = mPopupView.findViewById(R.id.item_layout3);
			view.setVisibility(View.VISIBLE);
			view = mPopupView.findViewById(R.id.plus_more);
			view.setVisibility(View.GONE);
			break;
		default:
			view = mPopupView.findViewById(R.id.item_layout1);
			view.setVisibility(View.VISIBLE);
			view = mPopupView.findViewById(R.id.item_layout2);
			view.setVisibility(View.VISIBLE);
			view = mPopupView.findViewById(R.id.item_layout3);
			view.setVisibility(View.VISIBLE);
			final TextView tv = (TextView) mPopupView
					.findViewById(R.id.plus_more);
			tv.setVisibility(View.VISIBLE);
			final String format = mResources.getString(R.string.plus_N_more);
			final String plusMore = String.format(format, eventIndex - 4);
			tv.setText(plusMore);
			break;
		}

		if (eventIndex > 5) {
			eventIndex = 5;
		}
		final int popupHeight = 20 * eventIndex + 15;
		mPopup.setHeight(popupHeight);

		if (mPreviousPopupHeight != popupHeight) {
			mPreviousPopupHeight = popupHeight;
			mPopup.dismiss();
		}
		mPopup.showAtLocation(this, Gravity.BOTTOM | Gravity.LEFT, 0, 0);
		postDelayed(mDismissPopup, POPUP_DISMISS_DELAY);
	}
}
