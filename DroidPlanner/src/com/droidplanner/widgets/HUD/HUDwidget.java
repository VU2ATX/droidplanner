package com.droidplanner.widgets.HUD;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.MAVLink.Messages.ApmModes;

/**
 * Widget for a HUD Originally copied from http://code.google.com/p/copter-gcs/
 * 
 */
public class HUDwidget extends SurfaceView implements SurfaceHolder.Callback {
	private static final float SCROLLER_HEIGHT_PERCENT = .30f;
	private static final float SCROLLER_WIDTH = 60;
	private static final int SCROLLER_ARROW_HEIGTH = 26;

	private static final int SCROLLER_VSI_RANGE = 12;
	private static final int SCROLLER_ALT_RANGE = 26;
	private static final int SCROLLER_SPEED_RANGE = 26;

	private ScopeThread renderer;
	private int width;
	private int height;

	// Drone Status
	double roll = 0, pitch = 0, yaw = 0, altitude = 0, disttowp = 0,
			verticalSpeed = 0, groundSpeed = 0, airSpeed = 0, targetSpeed = 0,
			targetAltitude = 0;
	int wpno = -1;
	private String remainBatt = "";
	private String battVolt = "";
	private String gpsFix = "";
	private String mode = "Unknown";

	// Paints
	Paint grid_paint = new Paint();
	Paint ground = new Paint();
	Paint sky = new Paint();
	Paint white = new Paint();
	Paint whiteCenter = new Paint();
	Paint whitebar = new Paint();
	Paint whiteStroke = new Paint();
	Paint statusText = new Paint();
	Paint ScrollerText = new Paint();
	Paint ScrollerTextLeft = new Paint();
	Paint yawText = new Paint();

	Paint plane = new Paint();
	Paint redSolid = new Paint();
	Paint blackSolid = new Paint();
	Paint blueVSI = new Paint();
	Paint greenPen;
	Paint greenLightPen;

	public HUDwidget(Context context, AttributeSet attributeSet) {
		super(context, attributeSet);
		getHolder().addCallback(this);

		grid_paint.setColor(Color.rgb(100, 100, 100));
		ground.setARGB(220, 148, 193, 31);
		sky.setARGB(220, 0, 113, 188);
		whitebar.setARGB(64, 255, 255, 255);

		white.setColor(Color.WHITE);
		white.setTextSize(15.0f * context.getResources().getDisplayMetrics().density);

		whiteCenter.setColor(Color.WHITE);
		whiteCenter.setTextSize(15.0f * context.getResources()
				.getDisplayMetrics().density);
		whiteCenter.setTextAlign(Align.CENTER);

		statusText.setColor(Color.WHITE);
		statusText.setTextSize(25.0f * context.getResources()
				.getDisplayMetrics().density);

		ScrollerText = new Paint(statusText);
		ScrollerText.setTextAlign(Paint.Align.LEFT);
		ScrollerTextLeft = new Paint(ScrollerText);
		ScrollerTextLeft.setTextAlign(Paint.Align.RIGHT);

		whiteStroke.setColor(Color.WHITE);
		whiteStroke.setStyle(Style.STROKE);
		whiteStroke.setStrokeWidth(3);
		whiteStroke.setAntiAlias(true); // Shouldn't affect performance

		yawText = new Paint(whiteStroke);
		yawText.setStrokeWidth(2);
		yawText.setTextSize(20);
		yawText.setTextAlign(Paint.Align.CENTER);

		greenPen = new Paint(whiteStroke);
		greenPen.setColor(Color.GREEN);
		greenPen.setStrokeWidth(6);
		greenLightPen = new Paint(greenPen);
		greenLightPen.setAlpha(128);

		plane.setColor(Color.RED);
		plane.setStyle(Style.STROKE);
		plane.setStrokeWidth(3);

		redSolid.setColor(Color.RED);
		blackSolid.setColor(Color.BLACK);
		blueVSI.setARGB(255, 0, 50, 250);
	}

	@Override
	protected void onDraw(Canvas canvas) {

		// clear screen
		canvas.drawColor(Color.rgb(20, 20, 20));
		canvas.translate(width / 2, height / 2);

		canvas.save();
		drawPitch(canvas);
		canvas.restore();
		canvas.save();
		drawRoll(canvas);
		canvas.restore();
		canvas.save();
		drawYaw(canvas);
		canvas.restore();
		canvas.save();
		drawText(canvas);
		canvas.restore();
		canvas.save();
		drawPlane(canvas);
		canvas.restore();
		canvas.save();
		drawRightScroller(canvas);
		canvas.restore();
		canvas.save();
		drawLeftScroller(canvas);
		canvas.restore();

	}

	private void drawPlane(Canvas canvas) {
		canvas.drawCircle(0, 0, 15, plane);

		canvas.drawLine(-15, 0, -25, 0, plane);
		canvas.drawLine(15, 0, 25, 0, plane);
		canvas.drawLine(0, -15, 0, -25, plane);

	}

	// Draw text aligned correctly.
	private void drawText(Canvas canvas, int i, String text, Paint p,
			boolean left) {
		Rect bounds = new Rect();
		p.getTextBounds(text, 0, text.length(), bounds);

		float y = (float) (height / 2.0 - i * bounds.height() * 1.2)
				- (float) bounds.height() * 0.3f;

		if (left)
			canvas.drawText(text,
					(float) (-width / 2.0 + bounds.height() * .2f), y, p);
		else
			canvas.drawText(
					text,
					(float) (width / 2.0 - bounds.width() - bounds.height() * .2f),
					y, p);

	}

	private void drawText(Canvas canvas) {
		drawText(canvas, 1, gpsFix, statusText, true);
		// drawText(canvas, 0, altitude, statusText, true);

		drawText(canvas, 1, remainBatt, statusText, false);
		drawText(canvas, 0, battVolt, statusText, false);

	}

	private void drawYaw(Canvas canvas) {
		canvas.drawRect(-width, -height / 2, width, -height / 2 + 30, sky);
		canvas.drawLine(-width, -height / 2 + 30, width, -height / 2 + 30,
				white);

		// width / 2 == yawPosition
		// then round to nearest 5 degrees, and draw it.

		double centerDegrees = yaw;
		double numDegreesToShow = 120;
		double degreesPerPixel = (double) width / numDegreesToShow;

		double mod = yaw % 5;
		for (double angle = (centerDegrees - mod) - numDegreesToShow / 2.0; angle <= (centerDegrees - mod)
				+ numDegreesToShow / 2.0; angle += 5) {

			// protect from wraparound
			double workAngle = (angle + 360.0);
			while (workAngle >= 360)
				workAngle -= 360.0;

			// need to draw "angle"
			// How many pixels from center should it be?
			int distanceToCenter = (int) ((angle - centerDegrees) * degreesPerPixel);

			if (workAngle % 45 == 0) {
				String compass[] = { "N", "NE", "E", "SE", "S", "SW", "W", "NW" };
				int index = (int) workAngle / 45;
				canvas.drawText(compass[index], distanceToCenter,
						-height / 2 + 20, yawText);
				canvas.drawLine(distanceToCenter, -height / 2 + 25,
						distanceToCenter, -height / 2 + 30, white);
			} else if (workAngle % 15 == 0) {
				canvas.drawLine(distanceToCenter, -height / 2 + 20,
						distanceToCenter, -height / 2 + 30, white);
				canvas.drawText((int) (workAngle) + "", distanceToCenter,
						-height / 2 + 18, whiteCenter);
			} else {
				canvas.drawLine(distanceToCenter, -height / 2 + 25,
						distanceToCenter, -height / 2 + 30, white);
			}
		}

		// Draw the center line
		canvas.drawLine(0, -height / 2, 0, -height / 2 + 40, plane);

	}

	private void drawRoll(Canvas canvas) {
		int r = (int) ((double) width * 0.35); // 250;
		RectF rec = new RectF(-r, -height / 2 + 60, r, -height / 2 + 60 + 2 * r);

		// Draw the arc
		canvas.drawArc(rec, -180 + 45, 90, false, whiteStroke);

		// draw the ticks
		// The center of the circle is at:
		// 0, -height/2 + 60 + r

		float centerY = -height / 2 + 60 + r;
		for (int i = -45; i <= 45; i += 15) {
			// Draw ticks
			float dx = (float) Math.sin(i * Math.PI / 180) * r;
			float dy = (float) Math.cos(i * Math.PI / 180) * r;
			canvas.drawLine(dx, centerY - dy, (dx - (dx / 25)), centerY
					- (dy - dy / 25), whiteStroke);

			// Draw the labels
			if (i != 0) {
				dx = (float) Math.sin(i * Math.PI / 180) * (r + 10);
				dy = (float) Math.cos(i * Math.PI / 180) * (r + 10);
				canvas.drawText(Math.abs(i) + "", dx, centerY - dy, whiteCenter);

			}
		}

		float dx = (float) Math.sin(roll * Math.PI / 180) * r;
		float dy = (float) Math.cos(-roll * Math.PI / 180) * r;
		canvas.drawCircle(dx, centerY - dy, 10, redSolid);
	}

	private void drawPitch(Canvas canvas) {

		int step = 40; // Pixels per 5 degree step

		canvas.translate(0, (int) (pitch * (step / 5)));
		canvas.rotate(-(int) roll);

		// Draw the background box
		canvas.drawRect(-width, 0, width, 5 * height /* Go plenty low */, ground);
		canvas.drawRect(-width, -5 * height /* Go plenty high */, width, 0, sky);

		// Draw the vertical grid
		canvas.drawLine(-width, 0, width, 0, white);
		// canvas.f

		for (int i = -step * 20; i < step * 20; i += step) {
			if (i != 0) {
				if (i % (2 * step) == 0) {
					canvas.drawLine(-50, i, 50, i, white);
					canvas.drawText((5 * i / -step) + "", -90, i + 5, white);

				} else
					canvas.drawLine(-20, i, 20, i, white);
			}
		}
	}

	private void drawRightScroller(Canvas canvas) {
		final float textHalfSize = ScrollerText.getTextSize() / 2 - 1;

		// Outside box
		RectF scroller = new RectF(width * 0.5f - SCROLLER_WIDTH, -height
				* SCROLLER_HEIGHT_PERCENT, width * .50f, height
				* SCROLLER_HEIGHT_PERCENT);

		// Draw Vertical speed indicator
		final float vsi_width = scroller.width() / 4;
		float linespace = scroller.height() / SCROLLER_VSI_RANGE;
		Path vsiBox = new Path();
		vsiBox.moveTo(scroller.left, scroller.top); // draw outside box
		vsiBox.lineTo(scroller.left - vsi_width, scroller.top + vsi_width);
		vsiBox.lineTo(scroller.left - vsi_width, scroller.bottom - vsi_width);
		vsiBox.lineTo(scroller.left, scroller.bottom);
		for (int a = 1; a < SCROLLER_VSI_RANGE; a++) { // draw ticks
			float lineHeight = scroller.top + linespace * a;
			vsiBox.moveTo(scroller.left - vsi_width, lineHeight);
			vsiBox.lineTo(scroller.left - vsi_width / 2, lineHeight);
		}

		float vsiFillTrim = 0;
		if (verticalSpeed > 1) { // TODO Vertical Speed indicator must be tested
			vsiFillTrim = -1;
		} else if (verticalSpeed < -1) {
			vsiFillTrim = 1;
		}

		Path vsiFill = new Path();
		vsiFill.moveTo(scroller.left, scroller.centerY());
		vsiFill.lineTo(scroller.left - vsi_width, scroller.centerY());
		vsiFill.lineTo(scroller.left - vsi_width,
				(scroller.centerY() - (((float) verticalSpeed) + vsiFillTrim)
						* linespace));
		vsiFill.lineTo(scroller.left,
				(scroller.centerY() - ((float) verticalSpeed) * linespace));
		vsiFill.lineTo(scroller.left, scroller.centerY());
		canvas.drawPath(vsiFill, blueVSI);
		canvas.drawPath(vsiBox, whiteStroke);

		// Draw Altitude Scroller
		canvas.drawRect(scroller, whiteStroke);
		canvas.drawRect(scroller, whitebar);

		float space = scroller.height() / (float) SCROLLER_ALT_RANGE;
		int start = ((int) altitude - SCROLLER_ALT_RANGE / 2);

		if (start > targetAltitude) {
			canvas.drawLine(scroller.left, scroller.bottom, scroller.right,
					scroller.bottom, greenLightPen);
		} else if ((altitude + SCROLLER_SPEED_RANGE / 2) < targetAltitude) {
			canvas.drawLine(scroller.left, scroller.top, scroller.right,
					scroller.top, greenLightPen);
		}

		for (int a = start; a <= (altitude + SCROLLER_ALT_RANGE / 2); a += 1) { // go
																				// trough
																				// 1m
																				// steps
			float lineHeight = scroller.centerY() - space
					* (a - (int) altitude);

			if (a == ((int) targetAltitude) && targetAltitude != 0) {
				canvas.drawLine(scroller.left, lineHeight, scroller.right,
						lineHeight, greenPen);
			}
			if (a % 5 == 0) {
				canvas.drawLine(scroller.left, lineHeight, scroller.left + 10,
						lineHeight, whiteStroke);
				canvas.drawText(Integer.toString(a), scroller.left + 15,
						lineHeight + textHalfSize, ScrollerText);
			}
		}

		// Arrow with current altitude
		Path arrow = new Path();
		arrow.moveTo(scroller.right, -SCROLLER_ARROW_HEIGTH / 2);
		arrow.lineTo(scroller.left + SCROLLER_ARROW_HEIGTH / 2,
				-SCROLLER_ARROW_HEIGTH / 2);
		arrow.lineTo(scroller.left + 5, 0);
		arrow.lineTo(scroller.left + SCROLLER_ARROW_HEIGTH / 2,
				SCROLLER_ARROW_HEIGTH / 2);
		arrow.lineTo(scroller.right, SCROLLER_ARROW_HEIGTH / 2);
		canvas.drawPath(arrow, blackSolid);
		canvas.drawText(Integer.toString((int) altitude), scroller.left + 15,
				textHalfSize, ScrollerText);

		// Draw mode and wp distance
		canvas.drawText(mode, scroller.left - scroller.width() / 4,
				scroller.bottom + 25, ScrollerText);
		canvas.drawText(Integer.toString((int) disttowp) + ">" + wpno,
				scroller.left - scroller.width() / 4, scroller.bottom + 45,
				ScrollerText);

	}

	private void drawLeftScroller(Canvas canvas) {
		final float textHalfSize = ScrollerText.getTextSize() / 2 - 1;

		double speed = airSpeed; // TODO test airSpeed
		if (speed == 0)
			speed = groundSpeed;

		// Outside box
		RectF scroller = new RectF(-width * .50f, -height
				* SCROLLER_HEIGHT_PERCENT, -width * 0.5f + SCROLLER_WIDTH,
				height * SCROLLER_HEIGHT_PERCENT);

		// Draw Scroll
		canvas.drawRect(scroller, whiteStroke);
		canvas.drawRect(scroller, whitebar);

		float space = scroller.height() / (float) SCROLLER_SPEED_RANGE;
		int start = ((int) speed - SCROLLER_SPEED_RANGE / 2);

		if (start > targetSpeed) {
			canvas.drawLine(scroller.left, scroller.bottom, scroller.right,
					scroller.bottom, greenLightPen);
		} else if ((speed + SCROLLER_SPEED_RANGE / 2) < targetSpeed) {
			canvas.drawLine(scroller.left, scroller.top, scroller.right,
					scroller.top, greenLightPen);
		}

		for (int a = start; a <= (speed + SCROLLER_SPEED_RANGE / 2); a += 1) {
			float lineHeight = scroller.centerY() - space * (a - (int) speed);

			if (a == ((int) targetSpeed) && targetSpeed != 0) {
				canvas.drawLine(scroller.left, lineHeight, scroller.right,
						lineHeight, greenPen);
			}
			if (a % 5 == 0) {
				canvas.drawLine(scroller.right, lineHeight,
						scroller.right - 10, lineHeight, whiteStroke);
				canvas.drawText(Integer.toString(a), scroller.right - 15,
						lineHeight + textHalfSize, ScrollerTextLeft);
			}
		}

		// Arrow with current speed
		Path arrow = new Path();
		arrow.moveTo(scroller.left, -SCROLLER_ARROW_HEIGTH / 2);
		arrow.lineTo(scroller.right - SCROLLER_ARROW_HEIGTH / 2,
				-SCROLLER_ARROW_HEIGTH / 2);
		arrow.lineTo(scroller.right - 5, 0);
		arrow.lineTo(scroller.right - SCROLLER_ARROW_HEIGTH / 2,
				SCROLLER_ARROW_HEIGTH / 2);
		arrow.lineTo(scroller.left, SCROLLER_ARROW_HEIGTH / 2);
		canvas.drawPath(arrow, blackSolid);
		canvas.drawText(Integer.toString((int) speed), scroller.right - 15,
				textHalfSize, ScrollerTextLeft);

		// Draw mode and wp distance
		canvas.drawText("AS " + Integer.toString((int) airSpeed),
				scroller.left + 5, scroller.bottom + 25, ScrollerText);
		canvas.drawText("GS " + Integer.toString((int) groundSpeed),
				scroller.left + 5, scroller.bottom + 45, ScrollerText);

	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		this.width = width;
		this.height = height;
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		renderer = new ScopeThread(getHolder(), this);
		if (!renderer.isRunning()) {
			renderer.setRunning(true);
			renderer.start();
		}
	}

	private void setDirty() {
		if (renderer != null)
			renderer.setDirty();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		boolean retry = true;
		renderer.setRunning(false);
		while (retry) {
			try {
				renderer.join();
				renderer = null;
				retry = false;
			} catch (InterruptedException e) {
				// we will try it again and again...
			}
		}
	}

	private class ScopeThread extends Thread {
		private SurfaceHolder _surfaceHolder;
		private HUDwidget scope;
		private volatile boolean running = false;
		private Object dirty = new Object();

		public ScopeThread(SurfaceHolder surfaceHolder, HUDwidget panel) {
			_surfaceHolder = surfaceHolder;
			scope = panel;
		}

		public boolean isRunning() {
			return running;

		}

		public void setRunning(boolean run) {
			running = run;
			setDirty();
		}

		/** We may need to redraw */
		public void setDirty() {
			synchronized (dirty) {
				dirty.notify();
			}
		}

		@SuppressLint("WrongCall")
		// TODO fix error
		@Override
		public void run() {
			Canvas c;
			while (running) {
				synchronized (dirty) {
					c = null;
					try {
						c = _surfaceHolder.lockCanvas(null);
						synchronized (_surfaceHolder) {
							if (c != null) {
								scope.onDraw(c);
							}
						}
					} finally {
						// do this in a finally so that if an exception is
						// thrown
						// during the above, we don't leave the Surface in an
						// inconsistent state
						if (c != null) {
							_surfaceHolder.unlockCanvasAndPost(c);
						}
					}

					// We do this wait at the _end_ to ensure we always draw at
					// least one frame of
					// HUD data
					try {
						// Log.d("HUD", "Waiting for change");
						dirty.wait(); // TODO - not quite ready
						// Log.d("HUD", "Handling change");
					} catch (InterruptedException e) {
						// We will try again and again
					}
				}
			}
		}
	}

	/**
	 * Receive current copter orientation
	 * 
	 * @param roll
	 * @param pitch
	 * @param yaw
	 */
	public void newFlightData(float roll, float pitch, float yaw) {
		double r = (roll * 180.0 / Math.PI), p = (pitch * 180.0 / Math.PI), y = (yaw * 180.0 / Math.PI);

		if (r != roll || p != pitch || y != yaw) {
			this.roll = r;
			this.pitch = p;
			this.yaw = y;
			setDirty();
		}
	}

	public void setAltitude(double alt) {
		if (alt != altitude) {
			altitude = alt;
			setDirty();
		}
	}

	public void setBatteryRemaining(String d) {
		if (!remainBatt.equals(d)) {
			remainBatt = d;
			setDirty();
		}
	}

	public void setMode(int m) {
		String newMode = ApmModes.toString(m);
		if (!mode.equals(newMode)) {
			mode = newMode;
			setDirty();
		}
	}

	public void setDistanceToWaypoint(double d) {
		if (disttowp != d) {
			disttowp = d;
			setDirty();
		}
	}

	public void setVerticalSpeed(double d) {
		if (verticalSpeed != d) {
			verticalSpeed = d;
			setDirty();
		}
	}

	public void setGroundSpeed(double d) {
		if (groundSpeed != d) {
			groundSpeed = d;
			setDirty();
		}
	}

	public void setAirSpeed(double d) {
		if (airSpeed != d) {
			airSpeed = d;
			setDirty();
		}
	}

	public void setAltitudeError(double d) {
		setTargetAltitude(d + altitude);
	}

	public void setSpeedError(double d) {
		setTargetSpeed(d + airSpeed);
	}

	public void setTargetSpeed(double d) {
		if (targetSpeed != d) {
			targetSpeed = d;
			setDirty();
		}
	}

	public void setTargetAltitude(double d) {
		if (targetAltitude != d) {
			targetAltitude = d;
			setDirty();
		}
	}

	public void setWaypointNumber(int d) {
		if (wpno != d) {
			wpno = d;
			setDirty();
		}
	}

	public void setBatteryMVolt(String vbat) {
		if (!battVolt.equals(vbat)) {
			battVolt = vbat;
			setDirty();
		}
	}

	public void setGPSFix(String s) {
		if (!gpsFix.equals(s)) {
			gpsFix = s;
			setDirty();
		}
	}

}
