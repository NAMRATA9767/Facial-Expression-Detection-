package com.example.opencv;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class FacialExpressionRecognition {
    // define interpreter
    // Before this implement tensorflow to build.gradle file
    private Interpreter interpreter;
    // define input size
    private int INPUT_SIZE;
    // define height and width of original frame
    private int height = 0;
    private int width = 0;
    Bitmap bitmap = null;
    int numberfaces;

    // now define Gpudelegate
    // it is use to implement gpu in interpreter
    private GpuDelegate gpuDelegate = null;
    float emotion_v;
    String emotion_s;

    // now define cascadeClassifier for face detection
    private CascadeClassifier cascadeClassifier;

    // now call this in CameraActivity
    FacialExpressionRecognition(AssetManager assetManager, Context context, String modelPath, int inputSize) throws IOException {
        INPUT_SIZE = inputSize;
        // set GPU for the interpreter
        Interpreter.Options options = new Interpreter.Options();
        gpuDelegate = new GpuDelegate();
        // add gpuDelegate to option
        options.addDelegate(gpuDelegate);
        // now set number of threads to options
        options.setNumThreads(4); // set this according to your phone
        // this will load model weight to interpreter
        interpreter = new Interpreter(loadModelFile(assetManager, modelPath), options);
        // if model is load print
        Log.d("facial_Expression", "Model is loaded");

        // now we will load haarcascade classifier
        try {
            // define input stream to read classifier
            InputStream is = context.getResources().openRawResource(R.raw.haarcascade_frontalface_alt2);
            // create a folder
            File cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE);
            // now create a new file in that folder
            File mCascadeFile = new File(cascadeDir, "haarcascade_frontalface_alt2");
            // now define output stream to transfer data to file we created
            FileOutputStream os = new FileOutputStream(mCascadeFile);
            // now create buffer to store byte
            byte[] buffer = new byte[4096];
            int byteRead;
            // read byte in while loop
            // when it read -1 that means no data to read
            while (( byteRead = is.read(buffer) ) != -1) {
                // writing on mCascade file
                os.write(buffer, 0, byteRead);

            }
            // close input and output stream
            is.close();
            os.close();
            cascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
            // if cascade file is loaded print
            Log.d("facial_Expression", "Classifier is loaded");
            // check your code one more time
            // select device and run
            //I/MainActivity: OpenCv Is loaded
            //D/facial_Expression: Model is loaded
            //D/facial_Expression: Classifier is loaded
            // cropped frame is then pass through interpreter which will return facial expression/emotion

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // Create a new function
    // input and output are in Mat format
    // call this in onCameraframe of CameraActivity
    public Mat recognizeImage(Mat mat_image) {
//        Mat a = mat_image.t();
//        a.release();
        // before predicting
        // our image is not properly align
        // we have to rotate it by 90 degree for proper prediction
        Core.flip(mat_image.t(), mat_image, 0);// rotate mat_image by 90 degree
        // start with our process
        // convert mat_image to gray scale image
        Mat grayscaleImage = new Mat();
        Imgproc.cvtColor(mat_image, grayscaleImage, Imgproc.COLOR_RGBA2GRAY);
        // set height and width
        height = grayscaleImage.height();
        System.out.println("height" + height);
        width = grayscaleImage.width();
        System.out.println("width" + width);
        // define minimum height of face in original image
        // below this size no face in original image will show
        int absoluteFaceSize = (int) ( height * 0.1 );
        System.out.println("absoluteFaceSize" + absoluteFaceSize);
        // now create MatofRect to store face
        //Haar cascade detector detects "face" from the mgray(image) and stores it in faces(MatofRect),
        // MatofRect hold 4 points namely(x,y,widht,height).
        // These four points can be used to draw the rectangle around
        // the detected "face".There can be more than one possible matches so it stored in array of Rect.

        MatOfRect faces = new MatOfRect();
        System.out.println("faces matofreact" + faces);
        // check if cascadeClassifier is loaded or not
//The CascadeClassifier class of is used to load the classifier file and detects the desired objects in the image.
//
//The detectMultiScale() of this class detects multiple objects of various sizes. This method accepts −
//
//An object of the class Mat holding the input image.
//
//An object of the class MatOfRect to store the detected faces.
//
//To get the number of faces in the image −
//
//Load the lbpcascade_frontalface.xml file using the CascadeClassifier class.
//
//Invoke the detectMultiScale() method.
//
//Convert the MatOfRect object to an array.
//
//The length of the array is the number of faces in the image.
        if (cascadeClassifier != null) {
            // detect face in frame
            //                                  input         output
            cascadeClassifier.detectMultiScale(grayscaleImage, faces, 1.1, 2, 2,
                    new Size(absoluteFaceSize, absoluteFaceSize), new Size());
            // minimum size
            System.out.println(String.format("Detected %s faces",
                    faces.toArray().length));
            numberfaces = faces.toArray().length;
            System.out.println("number of faces" + numberfaces);

        }

        // now convert it to array
        Rect[] faceArray = faces.toArray();
        // loop through each face
        System.out.println("faceArray length:" + faceArray.length);
        for (int i = 0; i < faceArray.length; i++) {
            System.out.println("face array" + faceArray[i]);
        }
        for (int i = 0; i < faceArray.length; i++) {
            // if you want to draw rectangle around face
            //                input/output starting point ending point        color   R  G  B  alpha    thickness
            Imgproc.rectangle(mat_image, faceArray[i].tl(), faceArray[i].br(), new Scalar(0, 255, 0, 255), 2);
            // now crop face from original frame and grayscaleImage
            // starting x coordinate       starting y coordinate
            Rect roi = new Rect((int) faceArray[i].tl().x, (int) faceArray[i].tl().y,
                    ( (int) faceArray[i].br().x ) - (int) ( faceArray[i].tl().x ),
                    ( (int) faceArray[i].br().y ) - (int) ( faceArray[i].tl().y ));
            System.out.println("roi" + roi);
            // it's very important check one more time
            Mat cropped_rgba = new Mat(mat_image, roi);//
            // now convert cropped_rgba to bitmap
            bitmap = Bitmap.createBitmap(cropped_rgba.cols(), cropped_rgba.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(cropped_rgba, bitmap);
            // resize bitmap to (48,48)
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 48, 48, false);
            // now convert scaledBitmap to byteBuffer
            ByteBuffer byteBuffer = convertBitmapToByteBuffer(scaledBitmap);
            // now create an object to hold output
            float[][] emotion = new float[1][1];
            //now predict with bytebuffer as an input and emotion as an output
            interpreter.run(byteBuffer, emotion);
            // if emotion is recognize print value of it

            // define float value of emotion
            emotion_v = (float) Array.get(Array.get(emotion, 0), 0);
            Log.d("facial_expression", "Output:  " + emotion_v);
            // create a function that return text emotion
            emotion_s = get_emotion_text(emotion_v);
            System.out.println("STrING vALUE :" + emotion_s);
            String str = "Surprise";
            System.out.println("String Original" + str);

            // now put text on original frame(mat_image)
            //             input/output    text: Angry (2.934234)
            if (str.equals(emotion_s)) {
                System.out.println("String equal");

                //  Scalar colour = get_emotion_color(emotion_v);
                Imgproc.putText(mat_image, emotion_s + " (" + emotion_v + ")",


                        new Point((int) faceArray[i].tl().x + 10, (int) faceArray[i].tl().y + 20),
                        1, 3, new Scalar(255, 25, 255, 150), 2);
                //      use to scale text      color     R G  B  alpha    thickness

                //      use to scale text      color     R G  B  alpha    thickness
            }
            if (emotion_s == "Neutral") {
                Imgproc.putText(mat_image, emotion_s + " (" + emotion_v + ")",


                        new Point((int) faceArray[i].tl().x + 10, (int) faceArray[i].tl().y + 20),
                        1, 3, new Scalar(0, 255, 255, 150), 2);
                //      use to scale text      color     R G  B  alpha    thickness

                // select device and run

            }
            if (emotion_s == "Angry") {
                Imgproc.putText(mat_image, emotion_s + " (" + emotion_v + ")",


                        new Point((int) faceArray[i].tl().x + 10, (int) faceArray[i].tl().y + 20),
                        1, 3, new Scalar(150, 0, 255, 255), 2);
                //      use to scale text      color     R G  B  alpha    thickness
////
                // select device and run

            }
            if (emotion_s == "Sad") {
                Imgproc.putText(mat_image, emotion_s + " (" + emotion_v + ")",


                        new Point((int) faceArray[i].tl().x + 10, (int) faceArray[i].tl().y + 20),
                        1, 3, new Scalar(255, 0, 255, 150), 2);
                //      use to scale text      color     R G  B  alpha    thickness

                // select device and run

            }
            if (emotion_s == "Happy") {
                Imgproc.putText(mat_image, emotion_s + " (" + emotion_v + ")",


                        new Point((int) faceArray[i].tl().x + 10, (int) faceArray[i].tl().y + 20),
                        1, 3, new Scalar(0, 255, 255, 150), 2);
                //      use to scale text      color     R G  B  alpha    thickness

            }
            if (emotion_s == "Neutral") {
                Imgproc.putText(mat_image, emotion_s + " (" + emotion_v + ")",


                        new Point((int) faceArray[i].tl().x + 10, (int) faceArray[i].tl().y + 20),
                        1, 3, new Scalar(255, 255, 255, 150), 2);
                //      use to scale text      color     R G  B  alpha    thickness
            }
            // select device and run


            // now put text on original frame(mat_image)
            //             input/output    text: Angry (2.934234)
            // select device and run

        }


        // after prediction
        // rotate mat_image -90 degree
       // Mat b=mat_image.t();
        Core.flip(mat_image.t(), mat_image, -2);
      //  b.release();
        return mat_image;
    }
    public Mat recognizePhoto(Mat mat_image) {
//        Mat a = mat_image.t();
//        a.release();
        // before predicting
        // our image is not properly align
        // we have to rotate it by 90 degree for proper prediction
        // start with our process
        // convert mat_image to gray scale image
        Core.flip(mat_image.t(), mat_image, 0);// rotate mat_image by 90 degree

        Mat grayscaleImage = new Mat();
        Imgproc.cvtColor(mat_image, grayscaleImage, Imgproc.COLOR_RGBA2GRAY);
        // set height and width
        height = grayscaleImage.height();
        System.out.println("height" + height);
        width = grayscaleImage.width();
        System.out.println("width" + width);
        // define minimum height of face in original image
        // below this size no face in original image will show
        int absoluteFaceSize = (int) ( height * 0.1 );
        System.out.println("absoluteFaceSize" + absoluteFaceSize);
        // now create MatofRect to store face
        //Haar cascade detector detects "face" from the mgray(image) and stores it in faces(MatofRect),
        // MatofRect hold 4 points namely(x,y,widht,height).
        // These four points can be used to draw the rectangle around
        // the detected "face".There can be more than one possible matches so it stored in array of Rect.

        MatOfRect faces = new MatOfRect();
        System.out.println("faces matofreact" + faces);
        // check if cascadeClassifier is loaded or not
//The CascadeClassifier class of is used to load the classifier file and detects the desired objects in the image.
//
//The detectMultiScale() of this class detects multiple objects of various sizes. This method accepts −
//
//An object of the class Mat holding the input image.
//
//An object of the class MatOfRect to store the detected faces.
//
//To get the number of faces in the image −
//
//Load the lbpcascade_frontalface.xml file using the CascadeClassifier class.
//
//Invoke the detectMultiScale() method.
//
//Convert the MatOfRect object to an array.
//
//The length of the array is the number of faces in the image.
        if (cascadeClassifier != null) {
            // detect face in frame
            //                                  input         output
            cascadeClassifier.detectMultiScale(grayscaleImage, faces, 1.1, 2, 2,
                    new Size(absoluteFaceSize, absoluteFaceSize), new Size());
            // minimum size
            System.out.println(String.format("Detected %s faces",
                    faces.toArray().length));
            numberfaces = faces.toArray().length;
            System.out.println("number of faces" + numberfaces);

        }

        // now convert it to array
        Rect[] faceArray = faces.toArray();
        // loop through each face
        System.out.println("faceArray length:" + faceArray.length);
        for (int i = 0; i < faceArray.length; i++) {
            System.out.println("face array" + faceArray[i]);
        }
        for (int i = 0; i < faceArray.length; i++) {
            // if you want to draw rectangle around face
            //                input/output starting point ending point        color   R  G  B  alpha    thickness
            Imgproc.rectangle(mat_image, faceArray[i].tl(), faceArray[i].br(), new Scalar(0, 255, 0, 255), 2);
            // now crop face from original frame and grayscaleImage
            // starting x coordinate       starting y coordinate
            Rect roi = new Rect((int) faceArray[i].tl().x, (int) faceArray[i].tl().y,
                    ( (int) faceArray[i].br().x ) - (int) ( faceArray[i].tl().x ),
                    ( (int) faceArray[i].br().y ) - (int) ( faceArray[i].tl().y ));
            System.out.println("roi" + roi);
            // it's very important check one more time
            Mat cropped_rgba = new Mat(mat_image, roi);//
            // now convert cropped_rgba to bitmap
            bitmap = Bitmap.createBitmap(cropped_rgba.cols(), cropped_rgba.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(cropped_rgba, bitmap);
            // resize bitmap to (48,48)
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 48, 48, false);
            // now convert scaledBitmap to byteBuffer
            ByteBuffer byteBuffer = convertBitmapToByteBuffer(scaledBitmap);
            // now create an object to hold output
            float[][] emotion = new float[1][1];
            //now predict with bytebuffer as an input and emotion as an output
            interpreter.run(byteBuffer, emotion);
            // if emotion is recognize print value of it

            // define float value of emotion
            emotion_v = (float) Array.get(Array.get(emotion, 0), 0);
            Log.d("facial_expression", "Output:  " + emotion_v);
            // create a function that return text emotion
            emotion_s = get_emotion_text(emotion_v);
            System.out.println("STrING vALUE :" + emotion_s);
            String str = "Surprise";
            System.out.println("String Original" + str);

            // now put text on original frame(mat_image)
            //             input/output    text: Angry (2.934234)
            if (str.equals(emotion_s)) {
                System.out.println("String equal");

                //  Scalar colour = get_emotion_color(emotion_v);
                Imgproc.putText(mat_image, emotion_s + " (" + emotion_v + ")",


                        new Point((int) faceArray[i].tl().x + 10, (int) faceArray[i].tl().y + 20),
                        1, 3, new Scalar(255, 25, 255, 150), 2);
                //      use to scale text      color     R G  B  alpha    thickness

                //      use to scale text      color     R G  B  alpha    thickness
            }
            if (emotion_s == "Neutral") {
                Imgproc.putText(mat_image, emotion_s + " (" + emotion_v + ")",


                        new Point((int) faceArray[i].tl().x + 10, (int) faceArray[i].tl().y + 20),
                        1, 3, new Scalar(0, 255, 255, 150), 2);
                //      use to scale text      color     R G  B  alpha    thickness

                // select device and run

            }
            if (emotion_s == "Angry") {
                Imgproc.putText(mat_image, emotion_s + " (" + emotion_v + ")",


                        new Point((int) faceArray[i].tl().x + 10, (int) faceArray[i].tl().y + 20),
                        1, 3, new Scalar(150, 0, 255, 255), 2);
                //      use to scale text      color     R G  B  alpha    thickness
////
                // select device and run

            }
            if (emotion_s == "Sad") {
                Imgproc.putText(mat_image, emotion_s + " (" + emotion_v + ")",


                        new Point((int) faceArray[i].tl().x + 10, (int) faceArray[i].tl().y + 20),
                        1, 3, new Scalar(255, 0, 255, 150), 2);
                //      use to scale text      color     R G  B  alpha    thickness

                // select device and run

            }
            if (emotion_s == "Happy") {
                Imgproc.putText(mat_image, emotion_s + " (" + emotion_v + ")",


                        new Point((int) faceArray[i].tl().x + 10, (int) faceArray[i].tl().y + 20),
                        1, 3, new Scalar(0, 255, 255, 150), 2);
                //      use to scale text      color     R G  B  alpha    thickness

            }
            if (emotion_s == "Neutral") {
                Imgproc.putText(mat_image, emotion_s + " (" + emotion_v + ")",


                        new Point((int) faceArray[i].tl().x + 10, (int) faceArray[i].tl().y + 20),
                        1, 3, new Scalar(255, 255, 255, 150), 2);
                //      use to scale text      color     R G  B  alpha    thickness
            }
            // select device and run


            // now put text on original frame(mat_image)
            //             input/output    text: Angry (2.934234)
            // select device and run

        }


        // after prediction
        // rotate mat_image -90 degree
        // Mat b=mat_image.t();
        Core.flip(mat_image.t(), mat_image, -2);
        //  b.release();
        return mat_image;
    }


    private String get_emotion_text(float emotion_v) {
        // create an empty string
        String val = "";
        // use if statement to determine val
        // You can change starting value and ending value to get better result
        // Like

        if (emotion_v >= 0 & emotion_v < 0.5) {

            val = "Surprise";
        } else if (emotion_v >= 0.5 & emotion_v < 1.5) {
            val = "Fear";
        } else if (emotion_v >= 1.5 & emotion_v < 2.6) {
            val = "Angry";
        } else if (emotion_v >= 2.6 & emotion_v < 3.1) {
            val = "Neutral";
        } else if (emotion_v >= 3.1 & emotion_v < 4.4) {
            val = "Sad";
        } else if (emotion_v >= 4.4 & emotion_v < 5.9) {
            val = "Disgust";
        } else {
            val = "Happy";
        }
        return val;

    }
//                 }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap scaledBitmap) {
        ByteBuffer byteBuffer;
        int size_image = INPUT_SIZE;//48
        // creating object of ByteBuffer
        // and allocating size capacity

        byteBuffer = ByteBuffer.allocateDirect(4 * 1 * size_image * size_image * 3);
        // 4 is multiplied for float input
        // 3 is multiplied for rgb
        // Reads the Int at this buffer's current position
        // using order() method

        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[size_image * size_image];
        scaledBitmap.getPixels(intValues, 0, scaledBitmap.getWidth(), 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight());
        int pixel = 0;
        for (int i = 0; i < size_image; ++i) {
            for (int j = 0; j < size_image; ++j) {
                final int val = intValues[pixel++];
                // now put float value to bytebuffer
                // scale image to convert image from 0-255 to 0-1
                byteBuffer.putFloat(( ( ( val >> 16 ) & 0xFF ) ) / 255.0f);
                byteBuffer.putFloat(( ( ( val >> 8 ) & 0xFF ) ) / 255.0f);
                byteBuffer.putFloat(( ( val & 0xFF ) ) / 255.0f);

            }
        }
        return byteBuffer;
        // check one more time it is important else you will get error
    }


    private MappedByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException {
        // this will give description of file
        AssetFileDescriptor assetFileDescriptor = assetManager.openFd(modelPath);
        // create a inputsteam to read file
        FileInputStream inputStream = new FileInputStream(assetFileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();

        long startOffset = assetFileDescriptor.getStartOffset();
        long declaredLength = assetFileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);

    }
}
