/**
 * Copyright (C) 2009, 2010 SC 4ViewSoft SRL
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
package com.androsz.electricsleepbeta.achartengine.chart;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.RectF;

import com.androsz.electricsleepbeta.achartengine.model.CategorySeries;
import com.androsz.electricsleepbeta.achartengine.renderer.DefaultRenderer;
import com.androsz.electricsleepbeta.achartengine.renderer.SimpleSeriesRenderer;

/**
 * The pie chart rendering class.
 */
public class PieChart extends AbstractChart {
	/**
	 * 
	 */
	private static final long serialVersionUID = 4218161795702587563L;
	/** The legend shape width. */
	private static final int SHAPE_WIDTH = 10;
	/** The series dataset. */
	private final CategorySeries mDataset;
	/** The series renderer. */
	private final DefaultRenderer mRenderer;

	/**
	 * Builds a new pie chart instance.
	 * 
	 * @param dataset
	 *            the series dataset
	 * @param renderer
	 *            the series renderer
	 */
	public PieChart(CategorySeries dataset, DefaultRenderer renderer) {
		mDataset = dataset;
		mRenderer = renderer;
	}

	/**
	 * The graphical representation of the pie chart.
	 * 
	 * @param canvas
	 *            the canvas to paint to
	 * @param x
	 *            the top left x value of the view to draw to
	 * @param y
	 *            the top left y value of the view to draw to
	 * @param width
	 *            the width of the view to draw to
	 * @param height
	 *            the height of the view to draw to
	 * @param the
	 *            paint
	 */
	@Override
	public void draw(Canvas canvas, int x, int y, int width, int height, Paint paint) {
		paint.setAntiAlias(mRenderer.isAntialiasing());
		paint.setStyle(Style.FILL);
		paint.setTextSize(mRenderer.getLabelsTextSize());
		int legendSize = mRenderer.getLegendHeight();
		if (mRenderer.isShowLegend() && legendSize == 0) {
			legendSize = height / 5;
		}
		final int left = x + 15;
		final int top = y + 5;
		final int right = x + width - 5;
		final int bottom = y + height - legendSize;
		drawBackground(mRenderer, canvas, x, y, width, height, paint, false,
				DefaultRenderer.NO_COLOR);

		final int sLength = mDataset.getItemCount();
		double total = 0;
		final String[] titles = new String[sLength];
		for (int i = 0; i < sLength; i++) {
			total += mDataset.getValue(i);
			titles[i] = mDataset.getCategory(i);
		}
		float currentAngle = 0;
		final int mRadius = Math.min(Math.abs(right - left), Math.abs(bottom - top));
		final int radius = (int) (mRadius * 0.35);
		final int centerX = (left + right) / 2;
		final int centerY = (bottom + top) / 2;
		final float shortRadius = radius * 0.9f;
		final float longRadius = radius * 1.1f;
		final RectF oval = new RectF(centerX - radius, centerY - radius, centerX + radius, centerY
				+ radius);
		for (int i = 0; i < sLength; i++) {
			paint.setColor(mRenderer.getSeriesRendererAt(i).getColor());
			final float value = (float) mDataset.getValue(i);
			final float angle = (float) (value / total * 360);
			canvas.drawArc(oval, currentAngle, angle, true, paint);
			if (mRenderer.isShowLabels()) {
				paint.setColor(mRenderer.getLabelsColor());
				final double rAngle = Math.toRadians(90 - (currentAngle + angle / 2));
				final double sinValue = Math.sin(rAngle);
				final double cosValue = Math.cos(rAngle);
				final int x1 = Math.round(centerX + (float) (shortRadius * sinValue));
				final int y1 = Math.round(centerY + (float) (shortRadius * cosValue));
				final int x2 = Math.round(centerX + (float) (longRadius * sinValue));
				final int y2 = Math.round(centerY + (float) (longRadius * cosValue));
				canvas.drawLine(x1, y1, x2, y2, paint);
				int extra = 10;
				paint.setTextAlign(Align.LEFT);
				if (x1 > x2) {
					extra = -extra;
					paint.setTextAlign(Align.RIGHT);
				}
				canvas.drawLine(x2, y2, x2 + extra, y2, paint);
				canvas.drawText(mDataset.getCategory(i), x2 + extra, y2 + 5, paint);
			}
			currentAngle += angle;
		}
		drawLegend(canvas, mRenderer, titles, left, right, y, width, height, legendSize, paint);
	}

	/**
	 * The graphical representation of the legend shape.
	 * 
	 * @param canvas
	 *            the canvas to paint to
	 * @param renderer
	 *            the series renderer
	 * @param x
	 *            the x value of the point the shape should be drawn at
	 * @param y
	 *            the y value of the point the shape should be drawn at
	 * @param paint
	 *            the paint to be used for drawing
	 */
	@Override
	public void drawLegendShape(Canvas canvas, SimpleSeriesRenderer renderer, float x, float y,
			Paint paint) {
		canvas.drawRect(x, y - SHAPE_WIDTH / 2, x + SHAPE_WIDTH, y + SHAPE_WIDTH / 2, paint);
	}

	/**
	 * Returns the legend shape width.
	 * 
	 * @return the legend shape width
	 */
	@Override
	public int getLegendShapeWidth() {
		return SHAPE_WIDTH;
	}

}
