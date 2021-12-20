package com.example.labdarbas3;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import yuku.ambilwarna.AmbilWarnaDialog;
import static com.example.labdarbas3.display.current_brush;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    int defaultColor;
    ImageView showPhoto;
    ConstraintLayout displayView;
    ImageButton cameraButton;
    ImageButton colorPicker;
    ImageButton drawButton;
    ImageButton copyrightButton;
    TextView copyrightText;

    public static Path path = new Path();
    public static Paint paint_brush = new Paint();

    private static final int SPEECH_REQUEST_CODE = 10;
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int REQUEST_CODE = 101;

//    private SpeechRecognizer speechRecognizer;
//    private Intent intentRecognizer;

    Bitmap captureImage;
    OutputStream outputStream;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

        displayView.setVisibility(View.GONE);
        copyrightText.setVisibility(View.GONE);

        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{
                            Manifest.permission.CAMERA
                    }, CAMERA_REQUEST_CODE);
        }

        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(intent, CAMERA_REQUEST_CODE);
            }
        });

        colorPicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openColorPicker();
            }
        });

        copyrightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyrightText.setVisibility(View.VISIBLE);
            }
        });
    }

    private void init() {
        showPhoto = findViewById(R.id.show_photo);
        cameraButton = findViewById(R.id.image_button);
        colorPicker = findViewById(R.id.color_pick_button);
        defaultColor = ContextCompat.getColor(MainActivity.this, R.color.white);
        displayView = findViewById(R.id.display_view);
        drawButton = findViewById(R.id.draw_button);
        copyrightButton = findViewById(R.id.copyright_button);
        copyrightText = findViewById(R.id.copyright_text);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST_CODE) {
            assert data != null;
            captureImage = (Bitmap) data.getExtras().get("data");
            showPhoto.setImageBitmap(captureImage);
        }

        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            assert data != null;
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);

            if (spokenText.equals("copyright")) {
                copyrightText.setVisibility(View.VISIBLE);
            }

            if (spokenText.equals("pencil")) {
                displayView.setVisibility(View.VISIBLE);
                paint_brush.setColor(defaultColor);
            }

            if (spokenText.equals("my name")) {
                if (copyrightText.getVisibility() == (View.VISIBLE)) {
                    copyrightText.setVisibility(View.GONE);
                } else {
                    copyrightText.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    public void openColorPicker() {
        AmbilWarnaDialog colorPicker = new AmbilWarnaDialog(this, defaultColor, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onCancel(AmbilWarnaDialog dialog) {

            }

            @Override
            public void onOk(AmbilWarnaDialog dialog, int color) {
                defaultColor = color;
                current_brush = defaultColor;
                path = new Path();
            }
        });
        colorPicker.show();
    }

    public void draw(View view) {
        displayView.setVisibility(View.VISIBLE);
        paint_brush.setColor(defaultColor);
    }

    public void speakButton(View view) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    public void sendEmail(View view) {
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            saveImage();
        } else {
            askPermission();
        }

        Intent intent = new Intent(this, EmailSend.class);
        startActivity(intent);
    }

    private void askPermission() {
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{WRITE_EXTERNAL_STORAGE}, REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveImage();
            } else {
                Toast.makeText(MainActivity.this, "Please provide the required permissions",
                        Toast.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void saveImage() {

        File dir = new File(Environment.getDataDirectory(), "SaveImage");

        if (!dir.exists()) {
            dir.mkdir();
        }

        File file = new File(dir, System.currentTimeMillis()+".jpg");
        try {
            outputStream = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        captureImage.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        Toast.makeText(MainActivity.this, "Image saved!", Toast.LENGTH_SHORT).show();

        try{
            outputStream.flush();
        } catch (Exception e){
            e.printStackTrace();
        }
        try{
            outputStream.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}