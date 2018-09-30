package cn.com.swain169.dasenginetest;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.neusoft.adas.DasAppParam;
import com.neusoft.adas.DasCameraFixedInfo;
import com.neusoft.adas.DasCameraParam;
import com.neusoft.adas.DasEgoCarInfo;
import com.neusoft.adas.DasEgoStatus;
import com.neusoft.adas.DasEngine;
import com.neusoft.adas.DasEvents;
import com.neusoft.adas.DasFCWParam;
import com.neusoft.adas.DasImageProcessingInfo;
import com.neusoft.adas.DasLDWParam;
import com.neusoft.adas.DasPCWParam;
import com.neusoft.adas.DasTrafficEnvironment;

import java.io.IOException;
import java.util.List;

public class MainActivity extends Activity implements SurfaceHolder.Callback {

    private String mVersion;
    private String mRequestKey;
    private Camera mCamera;
    private Parameters mParameters;
    private SurfaceView mPreviewSurfaceView;
    private DisplayView mDisplaySurfaceView;
    private SurfaceHolder mPreviewSurfaceHolder;
    private SurfaceHolder mDisplaySurfaceHolder;
    private int mCapImageHeight = 720;
    private int mCapImageWidth = 1280;
    private boolean mIsPreviewing = false;

    private FramePreviewCallback mPreviewCallback = new FramePreviewCallback();

    private String mTAG = "vispect";

    private DasAppParam mAppParam = new DasAppParam();
    private DasLDWParam mLDWParam = new DasLDWParam();
    private DasFCWParam mFCWParam = new DasFCWParam();
    private DasPCWParam mPCWParam = new DasPCWParam();
    private DasEgoStatus mEgoStatus = new DasEgoStatus();
    private DasEvents mEvents = new DasEvents();
    private DasTrafficEnvironment mTrafficEnv = new DasTrafficEnvironment();
    private DasEgoCarInfo mCarInfo;
    private DasCameraFixedInfo mCameraFixedInfo;
    private DasCameraParam mCameraParam;
    private DasImageProcessingInfo mImageInfo;
    private byte[] mKey;
    // private byte[] mFramebuffer = new byte[1280*720*2];
    private int ret = 0;

    static final private int NUM_CAMERA_PREVIEW_BUFFERS = 3;

    private TextView requestIDTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        requestIDTextView = (TextView) findViewById(R.id.requestid);

        mPreviewSurfaceView = (SurfaceView) findViewById(R.id.camera_preview);
        mPreviewSurfaceHolder = mPreviewSurfaceView.getHolder();
        mPreviewSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mPreviewSurfaceHolder.addCallback(this);

        mDisplaySurfaceView = (DisplayView) findViewById(R.id.display_layout);
        mDisplaySurfaceView.setPreviewSize(mCapImageHeight, mCapImageWidth);
        mDisplaySurfaceView.setZOrderOnTop(true);

        mVersion = DasEngine.getVersion();
        Log.v(mTAG, "[mVersion]" + mVersion);

        mRequestKey = DasEngine.generateRequestKey(getApplicationContext());
        Log.v(mTAG, "[mRequestKey]" + mRequestKey);
        requestIDTextView.setTextColor(Color.RED);
        requestIDTextView.setText(mRequestKey);

        String key;
        // key = "PmFEwIQd/nKms+4e5XB83g=="; // no author

        key = "YrSLBTJo9m15WqxjZLKuHw=="; // my vivo phone 867194027653693
//        key = "HiYRTk2juCyT4dGsKyAzFM=="; // vispect devices

        ret = DasEngine.saveAuthorizationKey(key.getBytes());

        // ret =
        // DasEngine.saveAuthorizationKey("XD9202dcSB3u0L9x+z+N1g==".getBytes());

        if (ret == 0) {
            Log.v(mTAG, "[saveAuthorizationKey] ok!");
        } else if (ret == -1) {
            Log.e(mTAG, "[saveAuthorizationKey] error!");
        }

        // AppParam
        mCarInfo = mAppParam.getEgoCarInfo();
        mCarInfo.setEgoHeight(1500);
        mCarInfo.setEgoWidth(1800);
        mCarInfo.setEgoLength(2500);

        mCameraFixedInfo = mAppParam.getCameraFixedInfo();
        mCameraFixedInfo.setFixedHeight(1200);
        mCameraFixedInfo.setFixedCenterOffset(0);
        mCameraFixedInfo.setFixedDistanceFromHead(1000);
        mCameraFixedInfo.setVanishingLineRowInPixel(360);
        mCameraFixedInfo.setDashboardRowInPixel(684);

        mCameraParam = mAppParam.getCameraParam();
        mCameraParam.setCx(640);
        mCameraParam.setCy(360);
        mCameraParam.setFx(1300);
        mCameraParam.setFy(1300);
        mCameraParam.setPitch(0.f);
        mCameraParam.setYaw(0.f);
        mCameraParam.setRoll(0.f);
        // mCameraParam.setK1(0.f);
        // mCameraParam.setK2(0.f);
        // mCameraParam.setK3(0.f);
        // mCameraParam.setK4(0.f);
        // mCameraParam.setK5(0.f);
        // mCameraParam.setP1(0.f);
        // mCameraParam.setP2(0.f);
        // mCameraParam.setAsp(1.f);

        mImageInfo = mAppParam.getImageProcessingInfo();
        mImageInfo.setImageWidth(1280);
        mImageInfo.setImageHeight(720);

        ret = DasEngine.init(getApplicationContext(), mAppParam);
        if (ret == 0) {
            Log.v(mTAG, "[init] ok!");
        } else if (ret == -7) {
            Log.e(mTAG, "[init] unauthorized device!");
        }

        // DasLDWParam
        mLDWParam
                .setSensitivityOfSolidMode(DasLDWParam.WARNING_SENSITIVITY_MIDDLE);
        mLDWParam
                .setSensitivityOfDashMode(DasLDWParam.WARNING_SENSITIVITY_MIDDLE);
        mLDWParam.setWarningStartSpeedOfSolidMode(40);
        mLDWParam.setWarningStartSpeedOfDashMode(40);
        mLDWParam.setWanderingAcrossLaneTimeThresh(5.f);
        DasEngine.setLDWParam(mLDWParam);

        // DasFCWParam
        mFCWParam.setSensitivityOfTTC(DasFCWParam.WARNING_SENSITIVITY_MIDDLE);
        mFCWParam
                .setSensitivityOfHeadway(DasFCWParam.WARNING_SENSITIVITY_MIDDLE);
        mFCWParam
                .setSensitivityOfVirtualBumper(DasFCWParam.WARNING_SENSITIVITY_MIDDLE);
        mFCWParam.setWarningStartSpeedOfTTC(20);
        mFCWParam.setWarningStartSpeedOfHeadway(20);
        mFCWParam.setFrontVehicleMovingDistanceThresh(5000);
        DasEngine.setFCWParam(mFCWParam);

        // DasPCWParam
        mPCWParam.setSensitivity(DasPCWParam.WARNING_SENSITIVITY_MIDDLE);
        mPCWParam.setWarningEndSpeed(60);
        DasEngine.setPCWParam(mPCWParam);

        // projectionManager = (MediaProjectionManager)
        // getSystemService(MEDIA_PROJECTION_SERVICE);
        // Intent intent = new Intent(this, RecordService.class);
        // bindService(intent, connection, BIND_AUTO_CREATE);

//		a(1);
    }


    private final class FramePreviewCallback implements PreviewCallback {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            // TODO Auto-generated method stub
            // Log.v(mTAG, " onPreviewFrame ");

            if (data != null) {

                // DasEgoStatus
                mEgoStatus.setTimestamp(System.currentTimeMillis());
                mEgoStatus.setModuleRunTable(DasEgoStatus.FCW_MODULE_RUN
                        | DasEgoStatus.LDW_MODULE_RUN
                        | DasEgoStatus.PCW_MODULE_RUN);
                mEgoStatus.setSpeedStatus(DasEgoStatus.SPEED_STATUS_GPS);
                mEgoStatus.setSpeed(55);
                mEgoStatus
                        .setSteeringStatus(DasEgoStatus.STEERING_STATUS_INVALID);
                mEgoStatus.setSteeringAngle(0);
                mEgoStatus
                        .setAccelerationStatus(DasEgoStatus.ACCELERATION_STATUS_INVALID);
                mEgoStatus
                        .setCarLampStatus(DasEgoStatus.CAR_LAMP_STATUS_INVALID);
                mEgoStatus
                        .setWindscreenWiperStatus(DasEgoStatus.WINDSCREEN_WIPER_STATUS_INVALID);
                mEgoStatus
                        .setWeatherCondition(DasEgoStatus.WEATHER_CONDITION_INVALID);
                mEgoStatus
                        .setLightCondition(DasEgoStatus.LIGHT_CONDITION_INVALID);

                long currentTimeMillis = System.currentTimeMillis();

                DasEngine.process(data, mEgoStatus);

                Log.e(mTAG, " limit : "
                        + (System.currentTimeMillis() - currentTimeMillis));

                DasEngine.getResults(mEvents, mTrafficEnv);

                parseEvents(mEvents);

                mDisplaySurfaceView.setTrafficEnvionment(mTrafficEnv);

                // Log.v(mTAG, "onPreviewFrame call......");

                camera.addCallbackBuffer(data);
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        // TODO Auto-generated method stub
        initCamera(holder);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        // initCamera(holder);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        closeCamera();
    }

    @Override
    protected void onDestroy() {

        // recordService.stopRecord();
        // unbindService(connection);
        Log.v(mTAG, " start onDestroy ");
        DasEngine.release();
        Log.v(mTAG, "  end onDestroy ");
        super.onDestroy();
    }

    private void initCamera(SurfaceHolder holder) {

        if (mCamera == null) {
            try {
                mCamera = Camera.open(0);
                if (null != mCamera) {
                    mParameters = mCamera.getParameters();
                } else {
                }
            } catch (RuntimeException e) {
                return;
            }

        } else {
            try {
                mCamera.reconnect();
                if (mParameters == null) {
                    mParameters = mCamera.getParameters();
                }
            } catch (IOException e) {
                return;
            }
        }

        List<Size> lstSize = mParameters.getSupportedPreviewSizes();
        boolean isHave = false;
        for (Size size : lstSize) {
            if (size.width == mCapImageWidth && size.height == mCapImageHeight) {
                isHave = true;
                break;
            }
        }

        if (!isHave) {
            int max_size = 0;
            int current_size = 0;

            mCapImageWidth = 0;
            mCapImageHeight = 0;

            for (int i = 0; i < lstSize.size(); i++) {
                current_size = lstSize.get(i).width * lstSize.get(i).height;
                if (current_size > max_size) {
                    mCapImageWidth = lstSize.get(i).width;
                    mCapImageHeight = lstSize.get(i).height;
                    max_size = current_size;
                }
            }
        }

        mParameters.setPreviewSize(mCapImageWidth, mCapImageHeight);
        mDisplaySurfaceHolder = mDisplaySurfaceView.getHolder();

        List<String> lstModes = mParameters.getSupportedFocusModes();

        if (lstModes.contains(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            mParameters
                    .setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        } else if (lstModes.contains(Parameters.FOCUS_MODE_AUTO)) {
            mParameters.setFocusMode(Parameters.FOCUS_MODE_AUTO);
        } else if (lstModes.contains(Parameters.FOCUS_MODE_INFINITY)) {
            mParameters.setFocusMode(Parameters.FOCUS_MODE_INFINITY);
        }

        mCamera.setParameters(mParameters);
        if (mCamera != null && !mIsPreviewing) {
            try {
                int preview_format = mParameters.getPreviewFormat();
                int bits_per_pixel = ImageFormat
                        .getBitsPerPixel(preview_format);
                Size preview_size = mParameters.getPreviewSize();
                int frame_size = ((preview_size.width * preview_size.height) * bits_per_pixel) / 8;

                // mFramebuffer = new byte[frame_size];
                // mCamera.addCallbackBuffer(mFramebuffer);
                for (int i = 0; i < NUM_CAMERA_PREVIEW_BUFFERS; i++) {
                    byte[] cameraBuffer = new byte[frame_size];
                    mCamera.addCallbackBuffer(cameraBuffer);
                }

                mCamera.setPreviewDisplay(holder);
                mCamera.setPreviewCallbackWithBuffer(mPreviewCallback);
                // mCamera.setPreviewCallback(mPreviewCallback);
                mCamera.startPreview();
                mIsPreviewing = true;
            } catch (IOException e) {
            }
        }
    }

    private void closeCamera() {
        if (mCamera != null) {
            mIsPreviewing = false;
            mCamera.setPreviewCallbackWithBuffer(null);
            // mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    private void parseEvents(DasEvents events) {

        boolean offRoadLeftEvent = events
                .parseEventCode(DasEvents.OFF_ROAD_LEFT_SOLID_EVENT);
        if (true == offRoadLeftEvent) {
            Log.v(mTAG, "------DasEvents.OFF_ROAD_LEFT_EVENT");
        }

        boolean offRoadRightEvent = events
                .parseEventCode(DasEvents.OFF_ROAD_RIGHT_SOLID_EVENT);
        if (true == offRoadRightEvent) {
            Log.v(mTAG, "------DasEvents.OFF_ROAD_RIGHT_EVENT");
        }

        boolean forwardTTCEvent = events
                .parseEventCode(DasEvents.FORWARD_TTC_COLLISION_EVENT);
        if (true == forwardTTCEvent) {
            Log.v(mTAG, "------DasEvents.FORWARD_TTC_COLLISION_EVENT");
        }

        boolean forwardHeadwayEvent = events
                .parseEventCode(DasEvents.FORWARD_HEADWAY_COLLISION_EVENT);
        if (true == forwardHeadwayEvent) {
            Log.v(mTAG, "------DasEvents.FORWARD_HEADWAY_COLLISION_EVENT");
        }

        boolean PedestrianWarningEvent = events
                .parseEventCode(DasEvents.PEDESTRIAN_WARNING_EVENT);
        if (true == PedestrianWarningEvent) {
            Log.v(mTAG, "------DasEvents.PEDESTRIAN_WARNING_EVENT");
        }

        boolean PedestrianCarefulEvent = events
                .parseEventCode(DasEvents.PEDESTRIAN_CAREFUL_EVENT);
        if (true == PedestrianCarefulEvent) {
            Log.v(mTAG, "------DasEvents.PEDESTRIAN_CAREFUL_EVENT");
        }

        boolean PedestrianSafeEvent = events
                .parseEventCode(DasEvents.PEDESTRIAN_SAFE_EVENT);
        if (true == PedestrianSafeEvent) {
            Log.v(mTAG, "------DasEvents.PEDESTRIAN_SAFE_EVENT");
        }

        boolean engineExceptionEvent = events
                .parseEventCode(DasEvents.ENGINE_EXCEPTION_EVENT);
        if (true == engineExceptionEvent) {
            Log.v(mTAG, "------DasEvents.ENGINE_EXCEPTION_EVENT");
        }

        boolean authorizationRequiredEvent = events
                .parseEventCode(DasEvents.AUTHORIZATION_REQUIRED_EVENT);
        if (true == authorizationRequiredEvent) {
            Log.v(mTAG, "------DasEvents.AUTHORIZATION_REQUIRED_EVENT");
        }

        boolean wanderingAcrossLaneEvent = events
                .parseEventCode(DasEvents.WANDERING_ACROSS_SOLID_LANE_EVENT);
        if (true == wanderingAcrossLaneEvent) {
            Log.v(mTAG, "------DasEvents.WANDERING_ACROSS_LANE_EVENT");
        }

        boolean frontVehicleMovingEvent = events
                .parseEventCode(DasEvents.FRONT_VEHICLE_MOVING_EVENT);
        if (true == frontVehicleMovingEvent) {
            Log.v(mTAG, "------DasEvents.FRONT_VEHICLE_MOVING_EVENT");
        }
    }

}
