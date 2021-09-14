package com.example.opencv;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.os.Handler;
import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.icu.text.SymbolTable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;


import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;

import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
//import java.util.TimerTask;
//import java.util.Timer.*;
//import java.util.Timer;
//import java.util.TimerTask;
//import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class CameraActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "MainActivity";


    private Mat mRgba;
    private Mat mGray;
    Bitmap bitmap;
    byte[] b;
    ImageView imageView;
    private CameraBridgeViewBase mOpenCvCameraView;
    //   //now we define integer that represents camera
    //0= back camera
    //1= front camer//a
    //Initially it start with back camera
    private int mCameraId = 0;
    //now call take picture icon
    private int take_image = 0;
    String fileName;
    Mat save_mat;
    float value;

    String strval;
    String currentDateAndTime;

    MyDatabaseHandler myDatabaseHandler;
    private CascadeClassifier cascadeClassifier;

    private FacialExpressionRecognition facialExpressionRecognition;
    private final BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface
                        .SUCCESS: {
                    Log.i(TAG, "OpenCv Is loaded");
                    mOpenCvCameraView.enableView();
                }
                default: {
                    super.onManagerConnected(status);

                }
                break;
            }
        }
    };

    public CameraActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //  storageReference = FirebaseStorage.getInstance().getReference();
        myDatabaseHandler = new MyDatabaseHandler(this);
        int MY_PERMISSIONS_REQUEST_CAMERA = 0;
        // if camera permission is not given it will ask for it on device
        if (ContextCompat.checkSelfPermission(CameraActivity.this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(CameraActivity.this, new String[]{Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
        }
        if (ContextCompat.checkSelfPermission(CameraActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(CameraActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_CAMERA);
        }
        if (ContextCompat.checkSelfPermission(CameraActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(CameraActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_CAMERA);
        }

        setContentView(R.layout.activity_camera);

        mOpenCvCameraView = findViewById(R.id.frame_Surface);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.enableFpsMeter();
        //it will display fps in log
        imageView = findViewById(R.id.imageView);
        Handler handler = new Handler();

        //Load the model
        try {
            int inputSize = 48;
            facialExpressionRecognition = new FacialExpressionRecognition(getAssets(), CameraActivity.this,
                    "newmodel.tflite", inputSize);

            InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_alt2);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, "haarcascade_frontalface_alt2");
            FileOutputStream os = new FileOutputStream(mCascadeFile);
            byte[] buffer = new byte[4096];
            int byteRead;
            while (( byteRead = is.read(buffer) ) != -1) {
                os.write(buffer, 0, byteRead);
            }
            is.close();
            os.close();
            //loading file from cascade folder created above
            cascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());


//
        } catch (IOException e) {
            //Log.i(TAG, "Cascade file not found");

            e.printStackTrace();
        }

        swapCamera();


        if (take_image == 0) {
            take_image = 1;

        } else {
            take_image = 0;
        }
    }


    private void swapCamera() {
        //first will change mCameraOId
        //iF 0 CHANGE IT TO 1
        //IF 1 CHANGE IT TO 0
        mCameraId = mCameraId ^ 1;
        mOpenCvCameraView.disableView();
//set camera index
        mOpenCvCameraView.setCameraIndex(mCameraId);
        //enale view
        mOpenCvCameraView.enableView();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (OpenCVLoader.initDebug()) {
            //if load success
            Log.d(TAG, "Opencv initialization is done");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        } else {
            //if not loaded
            Log.d(TAG, "Opencv is not loaded. try again");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }

    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC1);
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

//           mRgba = CascadeRec(mRgba);
//OUTPUT                                                  //INPUT
        mRgba = facialExpressionRecognition.recognizeImage(mRgba);
        //take_image_function_rgb(take_image, mRgba);

        // mRgba = facialExpressionRecognition.recognizePhoto(mRgba);

        //but when we change camera from back to front there is a rotation problem
        //front camera rotataed by 180
        //when cameraId is 1 (front) rotatae camera frame with 180 degree
        if (mCameraId == 1) {
            Core.flip(mRgba, mRgba, 1);
            Core.flip(mGray, mGray, -1);
          //  take_image_function_rgb(take_image, mRgba);


        }

//        ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(1);
//        exec.scheduleAtFixedRate(new Runnable() {
//            public void run() {

        take_image_function_rgb(take_image, mRgba);

        // code to execute repeatedly
//            }
//        }, 10, 30, TimeUnit.SECONDS); // execute every 60 seconds
//
        return mRgba;

    }

    private int take_image_function_rgb(int take_image, Mat mRgba) {
        value = facialExpressionRecognition.emotion_v;
        System.out.println("value not null outside" + value);
        float previousvalue = 0;
        if (previousvalue != value) {
            previousvalue = value;
            System.out.println("previousvalue" + previousvalue);
        }
        String previousstrvalue = null;
        if (previousstrvalue != strval) {
            previousstrvalue = strval;
            System.out.println("previousstrvalue" + previousstrvalue);
        }
        int numfaces = facialExpressionRecognition.numberfaces;
        System.out.println("number of faces not null outside" + numfaces);
        // if (take_image == 1 ) {

        if (take_image == 1 && value != 0.0 && numfaces != 0) {
            if (previousvalue != value && previousstrvalue != strval)
                System.out.println("value not null" + value);
            System.out.println("number of faces not null " + numfaces);

//adding permission for writing in local storage
            //NOW CREATE NEW MAT    THAT U WANT TO SAVE
            save_mat = new Mat();
            //rotate image by 90 degrre
            Core.flip(mRgba.t(), save_mat, 0);
//now convert image from RGBA TO BGRA
            Imgproc.cvtColor(save_mat, save_mat, Imgproc.COLOR_RGBA2BGRA);
            //NOW CREATE A NW FOLDER iMAGEpRO
            //WE WILL SAVE IMAGES INTO IMAGEpRO FOLDER
            //File storagDir=getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File folder = new File(getFilesDir() + "VKSFolderFacial");

            // File folder = new File(Environment.getExternalStorageDirectory().getPath() + "/VKSFacialCompany ");
//now check if folder exists
            //If not create new one
            boolean success = true;
            if (!folder.exists()) {
                success = folder.mkdirs();
                System.out.println("Folder is created");
            }
//now we have to create unique filename for that image

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm--ss");
            currentDateAndTime = sdf.format(new Date());
            System.out.println("current date and time " + currentDateAndTime);
            fileName = folder + "/" + currentDateAndTime + " .jpeg";            //write save_mat

            //now created function which takes image
            //output is take image
//         // execute every 60 seconds
            Imgcodecs.imwrite(fileName, save_mat);
            take_image = 0;
            System.out.println("file is created" + fileName);


//

        }
        bitmap = Bitmap.createBitmap(save_mat.cols(), save_mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(save_mat, bitmap);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, stream);
        b = stream.toByteArray();
            value = facialExpressionRecognition.emotion_v;
        System.out.println("camera value" + value);
        strval = facialExpressionRecognition.emotion_s;
        System.out.println("camera string  value" + strval);
        boolean flag = myDatabaseHandler.insertdetails(fileName, b, strval, value, currentDateAndTime);
        System.out.println("flag" + flag);
        if (flag) {
            System.out.println("database added");

        } else {
            System.out.println("database  not added");

        }

        return take_image;

    }
}


//




