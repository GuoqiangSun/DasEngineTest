package cn.com.swain169.dasenginetest;

import com.neusoft.adas.DasLaneMarkings;
import com.neusoft.adas.DasPedestrians;
import com.neusoft.adas.DasTrafficEnvironment;
import com.neusoft.adas.DasVehicles;
import com.neusoft.adas.DasZebraCrossings;
import com.neusoft.adas.DasLaneMarkings.DasLaneMarking;
import com.neusoft.adas.DasPedestrians.DasPedestrian;
import com.neusoft.adas.DasVehicles.DasVehicle;
import com.neusoft.adas.DasZebraCrossings.DasZebraCrossing;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.PorterDuff.Mode;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class DisplayView extends SurfaceView implements SurfaceHolder.Callback,
		Runnable {

	private SurfaceHolder mSurfaceHolder;
	private DasTrafficEnvironment mTrafficEnvionment;
	private DasLaneMarkings mLaneMarkings;
	private DasLaneMarking mLaneMarking;
	private DasVehicles mVehicles;
	private DasVehicle mVehicle;
	private DasPedestrians mPedestrains;
	private DasPedestrian mPedestrain;
	private DasZebraCrossings mZebraCrossings;
	private DasZebraCrossing mZebraCrossing;
	private Paint mLayerClearPaint = new Paint();
	private Paint mVanishingLinePaint = new Paint();
	private Paint mPaveRoadPaint = new Paint();
	private Paint mRectangePaint = new Paint();
	private Paint mTextPaint = new Paint();

	// static String TAG = "vispect";

	private Rect mRectIcon = new Rect();
	private Rect mRectLabel = new Rect();
	private Rect mRect = new Rect();
	private Path mRoadPath = new Path();

	private PorterDuffXfermode mPorterDuffXfermodeClear = new PorterDuffXfermode(
			Mode.CLEAR);
	private PorterDuffXfermode mPorterDuffXfermodeSrc = new PorterDuffXfermode(
			Mode.SRC);

	private int[] mGradualColors = new int[] { Color.argb(200, 0, 0, 0),
			Color.argb(120, 0, 0, 0), Color.argb(50, 0, 0, 0),
			Color.argb(20, 0, 0, 0), Color.argb(0, 0, 0, 0) };
	private int mPreviewHeight;
	private int mPreviewWidth;
	private int mCanvasWidth;
	private int mCanvasHeight;
	private int mCanvasWidthHalf;
	private int mCanvasHeightHalf;
	private int mCanvasWidthQuarter;
	private int mCanvasHeightQuarter;
	private int mCanvasWidthThreeQuarters;
	private int mCanvasHeightThreeQuarters;
	private float mfXRatio;
	private float mfYRatio;

	private long mSleepTime = 50;
	private boolean mIsUpdated = false;
	private boolean mLoop = false;

	public DisplayView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		mSurfaceHolder = this.getHolder();
		mSurfaceHolder.addCallback(this);
		mSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);
	}

	private long lstData = System.currentTimeMillis() - 1000;

	private int FPS = 0;

	public void setTrafficEnvionment(DasTrafficEnvironment trafficEnv) {
		// synchronized(this)
		{
			mTrafficEnvionment = trafficEnv;
			mIsUpdated = true;

			DasLaneMarkings laneMarkings = mTrafficEnvionment.getLaneMarkings();
			DasPedestrians pedestrians = mTrafficEnvionment.getPedestrians();
			DasVehicles vehicles = mTrafficEnvionment.getVehicles();

			int nums = laneMarkings.getNums();
			int nums2 = pedestrians.getNums();
			int nums3 = vehicles.getNums();

			if (nums != 0 || nums2 != 0 || nums3 != 0) {
				FPS++;
			}
			long currentTimeMillis = System.currentTimeMillis();

			if ((currentTimeMillis - lstData) > 1000) {
				lstData = currentTimeMillis;
				int curFps = FPS;
				FPS = 0;
				Log.v("vispect", "curFps " + curFps);
			}
		}
	}

	public void setPreviewSize(int height, int width) {
		mPreviewHeight = height;
		mPreviewWidth = width;
	}

	private void doLayerClear(Canvas canvas) {
		mLayerClearPaint.setXfermode(mPorterDuffXfermodeClear);
		canvas.drawPaint(mLayerClearPaint);
		mLayerClearPaint.setXfermode(mPorterDuffXfermodeSrc);
	}

	private void doDrawVanishingLine(Canvas canvas) {

		mCanvasWidthHalf = mCanvasWidth >> 1;
		mCanvasHeightHalf = mCanvasHeight >> 1;

		mCanvasWidthQuarter = mCanvasWidth >> 2;
		mCanvasHeightQuarter = mCanvasHeight >> 2;

		mCanvasWidthThreeQuarters = mCanvasWidthHalf + mCanvasWidthQuarter;
		mCanvasHeightThreeQuarters = mCanvasHeightHalf + mCanvasHeightQuarter;

		mVanishingLinePaint.setStrokeWidth((float) 3); // 2
		mVanishingLinePaint.setAlpha(20);
		mVanishingLinePaint.setColor(Color.RED);
		mVanishingLinePaint.setStyle(Paint.Style.STROKE);

		canvas.drawLine(mCanvasWidthQuarter, mCanvasHeightHalf,
				mCanvasWidthThreeQuarters, mCanvasHeightHalf,
				mVanishingLinePaint);
		canvas.drawLine(mCanvasWidthHalf, mCanvasHeightQuarter,
				mCanvasWidthHalf, mCanvasHeightThreeQuarters,
				mVanishingLinePaint);
	}

	private void doDrawLaneMarking(Canvas canvas, DasLaneMarking obj,
			float fXRatio, float fYRatio, int color) {
		int i;

		for (i = 0; i < mGradualColors.length; i++) {
			mGradualColors[i] = (mGradualColors[i] & 0xff000000)
					| (color & 0x00ffffff);
		}

		Shader mShader = new LinearGradient(0, 0, 0, (mPreviewHeight >> 1)
				* fYRatio, mGradualColors, null, Shader.TileMode.MIRROR);
		mPaveRoadPaint.setShader(mShader);
		mPaveRoadPaint.setAlpha(200);
		mPaveRoadPaint.setStrokeWidth(10.f);

		mTextPaint.setColor(Color.WHITE);
		mTextPaint.setTypeface(Typeface.DEFAULT);
		mTextPaint.setTextSize(20);
		mTextPaint.setTextAlign(Paint.Align.CENTER);

		canvas.drawText(Integer.toString(obj.getXDistance()),
				obj.getEndpointAX() * fXRatio, obj.getEndpointAY() * fYRatio,
				mTextPaint);
		canvas.drawLine(obj.getEndpointAX() * fXRatio, obj.getEndpointAY()
				* fYRatio, obj.getEndpointBX() * fXRatio, obj.getEndpointBY()
				* fYRatio, mPaveRoadPaint);
	}

	private void doDrawVehicle(Canvas canvas, DasVehicle obj, float fXRatio,
			float fYRatio, int color) {
		mRectangePaint.setStyle(Paint.Style.FILL);
		mRectangePaint.setColor(color);
		mRectangePaint.setAlpha(120);
		mRectangePaint.setStrokeWidth(3.0f);

		mRect.left = Math.round(obj.getLeft() * fXRatio);
		mRect.top = Math.round(obj.getTop() * fYRatio);
		mRect.right = Math.round(obj.getRight() * fXRatio);
		mRect.bottom = Math.round(obj.getBottom() * fYRatio);

		mTextPaint.setColor(Color.WHITE);
		mTextPaint.setTypeface(Typeface.DEFAULT);
		mTextPaint.setTextSize(20);
		mTextPaint.setTextAlign(Paint.Align.CENTER);

		if (obj.getTTC() > 0.0001f && obj.getTTC() < 3.0f) {
			canvas.drawText("TTC:" + Float.toString(obj.getTTC()),
					obj.getLeft() * fXRatio, obj.getTop() * fYRatio, mTextPaint);
		}

		if (obj.getHeadway() > 0.0001f && obj.getHeadway() < 3.0f) {
			canvas.drawText("HW:" + Float.toString(obj.getHeadway()),
					obj.getLeft() * fXRatio, obj.getBottom() * fYRatio,
					mTextPaint);
		}

		canvas.drawRect(mRect, mRectangePaint);

		Log.v("DisplayView", String.format("info:%f-%d-[%d,%d,%d,%d]",
				obj.getTTC(), obj.getYDistance(), mRect.left, mRect.top,
				mRect.right, mRect.bottom));
	}

	private void doDrawPedestrain(Canvas canvas, DasPedestrian obj,
			float fXRatio, float fYRatio, int color) {
		mRectangePaint.setStyle(Paint.Style.FILL);
		mRectangePaint.setColor(color);
		mRectangePaint.setAlpha(120);
		mRectangePaint.setStrokeWidth(3.0f);

		mRect.left = Math.round(obj.getLeft() * fXRatio);
		mRect.top = Math.round(obj.getTop() * fYRatio);
		mRect.right = Math.round(obj.getRight() * fXRatio);
		mRect.bottom = Math.round(obj.getBottom() * fYRatio);

		mTextPaint.setColor(Color.WHITE);
		mTextPaint.setTypeface(Typeface.DEFAULT);
		mTextPaint.setTextSize(20);
		mTextPaint.setTextAlign(Paint.Align.CENTER);

		// if (obj.getHeadway() > 0.0001f )
		{
			canvas.drawText("HW:" + Float.toString(obj.getHeadway()),
					obj.getLeft() * fXRatio, obj.getTop() * fYRatio, mTextPaint);
		}

		canvas.drawRect(mRect, mRectangePaint);

		Log.v("DisplayView", String.format("info:%f-%d-[%d,%d,%d,%d]",
				obj.getHeadway(), obj.getYDistance(), mRect.left, mRect.top,
				mRect.right, mRect.bottom));
	}

	private void doDrawZebraCrossing(Canvas canvas, DasZebraCrossing obj,
			float fXRatio, float fYRatio, int color, int alpha) {
		mRectangePaint.setStyle(Paint.Style.FILL);
		mRectangePaint.setColor(color);
		mRectangePaint.setAlpha(alpha);
		mRectangePaint.setStrokeWidth(3.0f);

		mRect.left = Math.round(obj.getFarLineEndpointAX() * fXRatio);
		mRect.top = Math.round(obj.getFarLineEndpointAY() * fYRatio);
		mRect.right = Math.round(obj.getNearLineEndpointBX() * fXRatio);
		mRect.bottom = Math.round(obj.getNearLineEndpointBY() * fYRatio);

		mTextPaint.setColor(Color.WHITE);
		mTextPaint.setTypeface(Typeface.DEFAULT);
		mTextPaint.setTextSize(50);
		mTextPaint.setTextAlign(Paint.Align.CENTER);

		// if (obj.getTTC() > 0.0001f )
		{
			// canvas.drawText( "TTC:"+Float.toString(obj.getTTC()),
			// obj.getLeft()*fXRatio, obj.getTop()*fYRatio, mTextPaint );
		}

		// if (obj.getHeadway() > 0.0001f )
		{
			// canvas.drawText( "Headway:"+Float.toString(obj.getHeadway()),
			// obj.getLeft()*fXRatio, obj.getBottom()*fYRatio, mTextPaint );
		}

		canvas.drawRect(mRect, mRectangePaint);
	}

	private void doDrawCanvas(Canvas canvas) {
		canvas.save();

		mCanvasWidth = canvas.getWidth();
		mCanvasHeight = canvas.getHeight();

		mfXRatio = (float) (mCanvasWidth) / (float) (mPreviewWidth);
		mfYRatio = (float) (mCanvasHeight) / (float) (mPreviewHeight);

		doLayerClear(canvas);
		doDrawVanishingLine(canvas);

		mLaneMarkings = mTrafficEnvionment.getLaneMarkings();

		// Log.v(TAG, " mLaneMarkings.getNums() " + mLaneMarkings.getNums());

		for (int i = 0; i < mLaneMarkings.getNums(); i++) {
			mLaneMarking = mLaneMarkings.getLaneMarkingByIndex(i);
			doDrawLaneMarking(canvas, mLaneMarking, mfXRatio, mfYRatio,
					Color.RED);
		}

		mVehicles = mTrafficEnvionment.getVehicles();

		// Log.v(TAG, "mTrafficEnvionment.getVehicles() " +
		// mVehicles.getNums());

		for (int i = 0; i < mVehicles.getNums(); i++) {
			mVehicle = mVehicles.getVehicleByIndex(i);
			doDrawVehicle(canvas, mVehicle, mfXRatio, mfYRatio, Color.RED);
		}

		mPedestrains = mTrafficEnvionment.getPedestrians();

		// Log.v(TAG, "mTrafficEnvionment.getPedestrians() " +
		// mPedestrains.getNums());

		for (int i = 0; i < mPedestrains.getNums(); i++) {
			mPedestrain = mPedestrains.getPedestrianByIndex(i);
			doDrawPedestrain(canvas, mPedestrain, mfXRatio, mfYRatio,
					Color.BLUE);
		}

		mZebraCrossings = mTrafficEnvionment.getZebraCrossings();

		// Log.v(TAG, "mTrafficEnvionment.getZebraCrossings() " +
		// mZebraCrossings.getNums());

		for (int i = 0; i < mZebraCrossings.getNums(); i++) {
			mZebraCrossing = mZebraCrossings.getZebraCrossingByIndex(i);
			doDrawZebraCrossing(canvas, mZebraCrossing, mfXRatio, mfYRatio,
					Color.MAGENTA, 50);
		}

		canvas.restore();

		return;
	}

	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub

	}

	@Override
	public void surfaceCreated(SurfaceHolder arg0) {
		// TODO Auto-generated method stub
		mLoop = true;
		new Thread(this).start();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
		// TODO Auto-generated method stub
		mLoop = false;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		while (mLoop) {
			if (mIsUpdated == true) {
				Canvas c = null;
				try {
					synchronized (mSurfaceHolder) {
						c = mSurfaceHolder.lockCanvas();
						if (c != null) {
							doDrawCanvas(c);
							Thread.sleep(mSleepTime);
							mIsUpdated = false;
						}
					}
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				} finally {
					if (c != null) {
						mSurfaceHolder.unlockCanvasAndPost(c);
					}
				}
			}
		}
	}
}
