package com.uname.whatisthisthing;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.media.ImageReader;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;


import android.Manifest;
import android.content.Intent;
// import android.support.design.widget.FloatingActionButton;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


import java.util.Map;

import mehdi.sakout.fancybuttons.FancyButton;


/**
 * Full-screen activity that shows live feed of camrea as background.
 */
public class Main extends AppCompatActivity{

    // Context
    Main boop;

    // PerimssionRequest
    private static final int REQUEST_CAMERA_PERMISSION = 200;

    // Buttons
    private FancyButton mButton;
    private Button mThingsButton;
    private Button mLogosButton;
    private Button mLandmarksButton;

    // Animations
    private Animation fadeInAnim = null;
    private Animation fadeOutAnim = null;
    private Animation flashAnim = null;

    // TextViews
    private TextView notificationText;
    private TextView title;
    private TextView content;

    // Progressbar
    private ProgressBar progressbar;

    // Overlay
    private ImageView overlay;

    // Holders
    private Button currentlySelectedButton;
    private RetrofitBuilder retrofitBuilder;

    // Modes
    private String mode = Constants.LABEL;


    private static final int UI_ANIMATION_DELAY = 300;

    // Camera
    CameraManager mCam;
    CameraCaptureSession camSesh;
    String cameraId;
    protected CameraDevice cameraDevice;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    protected CameraCaptureSession cameraCaptureSessions;
    private ImageReader imageReader;

    // View for Camera
    TextureView textureView;

    // Aspect Ratio
    DisplayMetrics displayMetrics;
    int DSI_height;
    int DSI_width;

    private final Handler mHideHandler = new Handler();
    private SurfaceView mContentView;
    private boolean btnToggle = false;
    private boolean safeToPreview = false;

    // Background Handler
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
/*            mContentView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
            );*/
        }
    };

    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };


    public static String PACKAGE_NAME;
    public static PackageManager PACKAGE_MANAGER;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PACKAGE_NAME = getApplicationContext().getPackageName();
        PACKAGE_MANAGER = getApplicationContext().getPackageManager();

        // Aspect Ratio
        displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        DSI_height = displayMetrics.heightPixels;
        DSI_width = displayMetrics.widthPixels;


        // Sets animations
        this.fadeOutAnim = AnimationUtils.loadAnimation(this, R.anim.fade_out);
        this.fadeInAnim = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        this.flashAnim = AnimationUtils.loadAnimation(this, R.anim.flash);


        setContentView(R.layout.activity_main_camera);

        mButton = (FancyButton) findViewById(R.id.btnID);

        notificationText = (TextView) findViewById(R.id.notificationTextID);

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btnClick();
            }
        });

        mThingsButton = (Button) findViewById(R.id.thingsBtnID);
        mThingsButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                thingsBtnClicked();
            }
        });

        mLogosButton = (Button) findViewById(R.id.logosBtnID);
        mLogosButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                logosBtnClicked();
            }
        });

        mLandmarksButton= (Button) findViewById(R.id.landmarksBtnID);
        mLandmarksButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                landmarksBtnClicked();
            }
        });


        // mContentView = (SurfaceView) findViewById(R.id.fullscreen_content);
        // mContentView.getHolder().addCallback(this);

        textureView = (TextureView) findViewById(R.id.fullscreen_content);
        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);

        boop = this;
/*
        Button mFocusbtn = (Button) findViewById(R.id.focusBtnID);
        mFocusbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: Need to do some checks here.
                Log.i("autofocus", "outer");
                // mCam.autoFocus(boop);
                if (safeToPreview && mCam != null){
                    Log.i("autofocus", "inner");
                    //mCam.autoFocus(boop);
                }
            }
        });
        */

        // TODO: Add onClick Listener on mContentView and to a mCam.autofocus(Callback) when it is clicked.

        // Sets textviews
        title = (TextView) findViewById(R.id.infoTitleID);
        content = (TextView) findViewById(R.id.infoContentID);

        progressbar = (ProgressBar) findViewById(R.id.progressID);

        // Hides the action- and toolbars.
        hide();

        overlay = (ImageView) findViewById(R.id.blackBackground);
        currentlySelectedButton = mThingsButton;
        // Log.i("Holder", mContentView.getHolder().toString());

        this.retrofitBuilder = new RetrofitBuilder();


        TextView permissionsText = findViewById(R.id.permissionsText);
        permissionsText.setVisibility(View.INVISIBLE);
        // Verify Permissions
        int permissionCheckCam = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA);
        int permissionCheckInternet = ContextCompat.checkSelfPermission(this,
                Manifest.permission.INTERNET);
        if (permissionCheckCam == PackageManager.PERMISSION_DENIED || permissionCheckInternet == PackageManager.PERMISSION_DENIED) {
            Log.i("perm", "Need access to camera and internet");
            permissionsText.setVisibility(View.VISIBLE);
        }
        permissionsText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                int permissionCheckCam = ContextCompat.checkSelfPermission(getApplicationContext(),
                        Manifest.permission.CAMERA);
                int permissionCheckInternet = ContextCompat.checkSelfPermission(getApplicationContext(),
                        Manifest.permission.INTERNET);
                if (permissionCheckCam == PackageManager.PERMISSION_DENIED || permissionCheckInternet == PackageManager.PERMISSION_DENIED) {
                    Log.i("perm", "Need access to camera and internet");
                } else {
                    restart();
                }
            }
        });
    }

    public void restart(){
        Intent intent = new Intent(this, Main.class);
        this.startActivity(intent);
        this.finishAffinity();
    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //open your camera here
            surfaceCreated();
        }
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height
        }
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };


    @Override
    public void onBackPressed() {
        if (btnToggle){
            returnToCameraState();
        } else {
            super.onBackPressed();
        }
    }

    private void thingsBtnClicked() {
        mThingsButton.startAnimation(fadeOutAnim);
        mThingsButton.setVisibility(View.INVISIBLE);
        notificationText.setText(R.string.logos);
        notificationText.startAnimation(flashAnim);
        mLogosButton.startAnimation(fadeInAnim);
        mLogosButton.setVisibility(View.VISIBLE);
        currentlySelectedButton = mLogosButton;
        mode = Constants.LOGO;
    }

    private void logosBtnClicked() {
        mLogosButton.startAnimation(fadeOutAnim);
        mLogosButton.setVisibility(View.INVISIBLE);
        notificationText.setText(R.string.landmarks);
        notificationText.startAnimation(flashAnim);
        mLandmarksButton.startAnimation(fadeInAnim);
        mLandmarksButton.setVisibility(View.VISIBLE);
        currentlySelectedButton = mLandmarksButton;
        mode = Constants.LANDMARK;
    }

    private void landmarksBtnClicked() {
        mLandmarksButton.startAnimation(fadeOutAnim);
        mLandmarksButton.setVisibility(View.INVISIBLE);
        notificationText.setText(R.string.things);
        notificationText.startAnimation(flashAnim);
        mThingsButton.startAnimation(fadeInAnim);
        mThingsButton.setVisibility(View.VISIBLE);
        currentlySelectedButton = mThingsButton;
        mode = Constants.LABEL;
    }


    private void btnClick() {
        if (!btnToggle){
            mButton.setBackgroundColor(Color.parseColor("#FF000000"));
            mButton.setText("Again");
            safeToPreview = false;
            btnToggle = true;

            overlay.startAnimation(fadeInAnim);
            overlay.setVisibility(View.VISIBLE);

            // Hides the mode selection
            currentlySelectedButton.setVisibility(View.INVISIBLE);
            currentlySelectedButton.startAnimation(fadeOutAnim);

            progressbar.startAnimation(fadeInAnim);
            progressbar.setVisibility(View.VISIBLE);

            takePicture();
            // Callbackmethod will be called when takePicture is done. see onPictureTaken

        } else{
            returnToCameraState();
        }
    }

    protected void takePicture(){
        if(null == cameraDevice) {
            Log.e("camNull", "cameraDevice is null");
            return;
        }
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            }
            int width = 640;
            int height = 480;
            if (jpegSizes != null && 0 < jpegSizes.length) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            // Orientation
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, rotation);
            // final File file = new File(Environment.getExternalStorageDirectory()+"/pic.jpg");
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    try {
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        Toast.makeText(Main.this, "Looking up!", Toast.LENGTH_SHORT).show();
                        retrieveInformation(bytes);
                        // save(bytes);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }
                /*
                private void save(byte[] bytes) throws IOException {
                    OutputStream output = null;
                    try {
                        output = new FileOutputStream(file);
                        output.write(bytes);
                    } finally {
                        if (null != output) {
                            output.close();
                        }
                    }
                }
                */
            };
            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                }
            };
            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            }, mBackgroundHandler);
            safeToPreview = true;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void returnToCameraState(){
        if (safeToPreview) {
            mButton.setBackgroundColor(Color.parseColor("#633e3e3e"));
            mButton.setText("");
            createCameraPreview();

            overlay.startAnimation(fadeOutAnim);
            overlay.setVisibility(View.INVISIBLE);

            // Shows the mode selection
            currentlySelectedButton.setVisibility(View.VISIBLE);
            currentlySelectedButton.startAnimation(fadeInAnim);

            title.startAnimation(fadeOutAnim);
            content.startAnimation(fadeOutAnim);
            title.setVisibility(View.INVISIBLE);
            content.setVisibility(View.INVISIBLE);
            if (progressbar.getVisibility() == View.VISIBLE){
                progressbar.startAnimation(fadeOutAnim);
                progressbar.setVisibility(View.INVISIBLE);
            }

            btnToggle = false;
        } else {
            // Todo: Tell the user to calm down
        }
    }

    /*
    private android.hardware.Camera safeCameraOpen() {

        // try catch here
        releaseCameraAndPreview();
        // return Camera.open();
        return false;
    }
    */

    private void releaseCameraAndPreview() {
        if (mCam != null) {
            // mCam.release();
            mCam = null;
        }
    }


    private void preparePreview() {
        try {
            // mCam.setPreviewDisplay(mContentView.getHolder());
            // mCam.startPreview();
        }
        catch (Exception e){
            Log.d("setPreviewDisplay", e.toString());
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first
        hide();
        if (safeToPreview){
            createCameraPreview();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
        hide();
    }

    public void surfaceCreated() {
        Log.d("Surface", "Started");
        // mCam = safeCameraOpen();
        // mCam.setDisplayOrientation(90);
        try{
            // Init camera
            mCam = (CameraManager) boop.getSystemService(Context.CAMERA_SERVICE);
            cameraId = mCam.getCameraIdList()[0];
            CameraCharacteristics characteristics = mCam.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(Main.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            mCam.openCamera(cameraId, openCameraCallback, null);
        } catch (Exception e){

        }

    }

    private final CameraDevice.StateCallback openCameraCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            //This is called when the camera is open
            Log.e("openCam", "onOpened");
            cameraDevice = camera;
            createCameraPreview();
        }
        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraDevice.close();
        }
        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    protected void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            setAspectRatioTextureView(imageDimension.getHeight(),imageDimension.getWidth());
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback(){
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(Main.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private void setAspectRatioTextureView(int ResolutionWidth , int ResolutionHeight )
    {
        int heightWOStatusbar = DSI_height + getStatusBarHeight();
        float dimension = (float) ResolutionWidth / (float) ResolutionHeight;
        if(ResolutionWidth > ResolutionHeight){
            int newWidth = DSI_width;
            int newHeight = ((heightWOStatusbar * ResolutionWidth)/ResolutionHeight);
            updateTextureViewSize(newWidth,newHeight);

        }else {
            int newHeight = heightWOStatusbar;
            int newWidth = (int) (dimension * (float) newHeight);
            updateTextureViewSize(newWidth,newHeight);
        }

    }

    private int getStatusBarHeight() {
        int height;

        Resources myResources = getResources();
        int idStatusBarHeight = myResources.getIdentifier(
                "status_bar_height", "dimen", "android");
        if (idStatusBarHeight > 0) {
            height = getResources().getDimensionPixelSize(idStatusBarHeight);
        }else{
            height = 0;
        }

        return height;
    }

    private void updateTextureViewSize(int viewWidth, int viewHeight) {
        Log.d("Aspect Ratio", "TextureView Width : " + viewWidth + " TextureView Height : " + viewHeight);
        textureView.setLayoutParams(new FrameLayout.LayoutParams(viewWidth, viewHeight));
    }


    protected void updatePreview() {
        if(null == cameraDevice) {
            Log.e("update", "updatePreview error, return");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d("Surface", "Changed");
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d("Surface", "Destroyed");
    }


    public void retrieveInformation(byte[] bytes) {
        Log.i("PicCallback", "Picture taken");
        Log.i("cloud", "bytes[] size: " + String.valueOf(bytes.length));
        Bitmap mBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        Log.i("cloud", "bitmap size: " + String.valueOf(mBitmap.getByteCount()));
        // Scale the image down to not send too big photo.
        // This is the actual bitmap we are going to send to cloudVision.

        //TODO: Compare notscaled and dowscaled and see difference
        // Bitmap downScaled = scaleBitmapDown(mBitmap, 1200);

        Log.i("cloud", "be4 send");
        //TODO : use downscaled
        try {
            callCloudVision(mBitmap);
        } catch (Exception e){
            content.setVisibility(View.VISIBLE);
            content.setText("wooops" + e.toString());
        }
        safeToPreview = true;


    }

    protected void sendRestRequest(String query){
        /* TODO: App sometimes crashes after a photo is taken. I currently have no Idea where and
            Why the crash occurs, but I am guessing that the names google returns sometimes contain
            characters that doesn't work well with the Wikipedia API.
        */

        try {
            retrofitBuilder.performQuery(query, this);
        } catch (Exception e){
            content.setText("Wops" + e.toString());
            content.setVisibility(View.VISIBLE);

        }
    }

    private void callCloudVision(final Bitmap downScaled) {

        // Do the real work in an async task, because we need to use the network anyway
        AsyncTask async = new MyAsync(this, downScaled, mode).execute();

    }


    public Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {

        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;

        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }


    public void restResponseReceived(Result mResponse) {
        if (mResponse != null) {
            Log.i("Retrofit", "Response received in Main.");

            // Get the actual page object
            Map.Entry<String, Page> entry = mResponse.query.pages.entrySet().iterator().next();
            Page page = entry.getValue();


            Log.i("Retrofit", page.title);
            Log.i("Retrofit", page.content);

            // Update view

            title.setText(page.title);
            content.setText(page.content);
            progressbar.startAnimation(fadeOutAnim);
            progressbar.setVisibility(View.INVISIBLE);

            title.startAnimation(fadeInAnim);
            content.startAnimation(fadeInAnim);
            title.setVisibility(View.VISIBLE);
            content.setVisibility(View.VISIBLE);
        } else {
            title.setText("Sorry");
            content.setText("We could not find anything.");
            progressbar.startAnimation(fadeOutAnim);
            progressbar.setVisibility(View.INVISIBLE);

            title.startAnimation(fadeInAnim);
            content.startAnimation(fadeInAnim);
            title.setVisibility(View.VISIBLE);
            content.setVisibility(View.VISIBLE);
        }
    }

    /*
    @Override
    public void onAutoFocus(boolean b, Camera camera) {
        //
    }
    */

    // Background Handler Thread
    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }
    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
