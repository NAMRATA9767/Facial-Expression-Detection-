package com.example.opencv;

import static com.example.opencv.StorageDatabaseHandler.*;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.IOException;

public class StorageAct extends AppCompatActivity {
    Mat selectedimage;
    Bitmap bmp;
    private FacialExpressionRecognition facialExpressionRecognition;
    ImageView imageView;
    float value;
    String strval;
    StorageDatabaseHandler storageDatabaseHandler;
    String filename;
    String currentDateAndTime;

    public StorageAct() throws IOException {
        // facialExpressionRecognition1 = new FacialExpressionRecognition();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = (ImageView) findViewById(R.id.image);
        storageDatabaseHandler = new StorageDatabaseHandler(this);

        Bundle extras = getIntent().getExtras();
        filename = extras.getString("filename");
        System.out.println("filename Storagact" + filename);
        currentDateAndTime = extras.getString("datetime");
        System.out.println("currentDateAndTime Storagact" + currentDateAndTime);

        byte[] byteArray = extras.getByteArray("picture");
        bmp = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);

        imageView.setImageBitmap(bmp);

        try {
            int inputsize = 48;
            facialExpressionRecognition = new FacialExpressionRecognition(getAssets(), StorageAct.this,
                    "model.tflite", inputsize);

        } catch (IOException e) {
            e.printStackTrace();
        }


        selectedimage = new Mat(bmp.getHeight(), bmp.getWidth(), CvType.CV_8UC4);
        System.out.println("selectedimage data" + selectedimage);
        selectedimage = facialExpressionRecognition.recognizePhoto(selectedimage);
        Bitmap bitmap1 = null;
        bitmap1 = Bitmap.createBitmap(selectedimage.cols(), selectedimage.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(selectedimage, bitmap1);
        //set imageview with this bitmap1
        imageView.setImageBitmap(bitmap1);
        value = facialExpressionRecognition.emotion_v;
        // value= facialExpressionRecognition.;
        System.out.println("Storage value" + value);
        strval = facialExpressionRecognition.emotion_s;
        System.out.println("storagevalue string  value" + strval);
        boolean flag = storageDatabaseHandler.insert(filename, byteArray, strval, value, currentDateAndTime);
        System.out.println("flag" + flag);
        if (flag) {
            System.out.println("database added");

        } else {
            System.out.println("database  not added");
//
//        }               return BitmapFactory.decodeByteArray(image, 0, image.length);
        }
    }
}
















