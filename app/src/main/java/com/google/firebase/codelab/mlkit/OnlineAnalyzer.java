package com.google.firebase.codelab.mlkit;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.EditText;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class OnlineAnalyzer implements Camera.PreviewCallback {
    private GraphicOverlay graphicOverlay;
    private BitmapFactory.Options options;
    private Canary processingPreview;
    private EditText textInputField;
    private int width;
    private int height;
    private int scale;

    OnlineAnalyzer(GraphicOverlay graphicOverlay, Canary processingPreview,
                   EditText textInputField) {
        this.graphicOverlay = graphicOverlay;
        options = new BitmapFactory.Options();
        options.inPurgeable = true;
        options.inInputShareable = true;
        this.processingPreview = processingPreview;
        this.textInputField = textInputField;
        this.scale = 2;
    }

    public void onPreviewFrame(byte[] data, Camera camera) {
        Camera.Parameters parameters=camera.getParameters();
        this.width = parameters.getPreviewSize().width;
        this.height = parameters.getPreviewSize().height;
        YuvImage yuv = new YuvImage(data, parameters.getPreviewFormat(), width, height, null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuv.compressToJpeg(new Rect(0, 0, width, height), 50, out);
        byte[] bytes = out.toByteArray();
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() / this.scale, bitmap.getHeight() / this.scale, true);
        runTextRecognition(bitmap);
    }

    private void runTextRecognition(Bitmap bitmapImage) {
        if (textInputField.getText() != null) {
            Log.e("111111111111111111", textInputField.getText().toString());
        }

        FirebaseVisionFaceDetectorOptions faceOpts =
                new FirebaseVisionFaceDetectorOptions.Builder().build();

        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmapImage);
        FirebaseVisionFaceDetector detector = FirebaseVision.getInstance()
                .getVisionFaceDetector(faceOpts);
        detector.detectInImage(image)
                .addOnSuccessListener(
                        new OnSuccessListener<List<FirebaseVisionFace>>() {
                            @Override
                            public void onSuccess(List<FirebaseVisionFace> faces) {
                                processFaceRecognitionResult(faces);
                                processingPreview.setIsActive(false);
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Task failed with an exception
                                e.printStackTrace();
                                processingPreview.setIsActive(false);
                            }
                        });
    }

    private void processFaceRecognitionResult(List<FirebaseVisionFace> faces) {
        graphicOverlay.clear();
        for (FirebaseVisionFace face : faces) {
            Rect bounds = face.getBoundingBox();
            Rect newBounds = new Rect();
            newBounds.left = this.height - this.scale * bounds.top;
            newBounds.top = this.scale * bounds.right;
            newBounds.right = this.height - this.scale * bounds.bottom;
            newBounds.bottom = this.scale * bounds.left;
            GraphicOverlay.Graphic faceGraphic = new FaceGraphic(graphicOverlay, newBounds);
            graphicOverlay.add(faceGraphic);
        }

        this.processingPreview.setIsActive(false);
    }
}
