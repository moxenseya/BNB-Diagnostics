package com.example.bnb_diagnostics;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.features2d.SimpleBlobDetector;
import org.opencv.features2d.SimpleBlobDetector_Params;
import org.opencv.imgproc.Imgproc;

import static org.opencv.imgcodecs.Imgcodecs.imread;
import static org.opencv.imgcodecs.Imgcodecs.imwrite;
import static org.opencv.imgproc.Imgproc.FONT_HERSHEY_SIMPLEX;

public class MainActivity extends AppCompatActivity {

    Bitmap myBitmap;
    ImageView imageView;
    Mat rawImage;
    String currentPhotoPath;
    String currentPhotoPathMarked;

    int cellCountValue;
    int thresholdValue;

    float minCircularityValue;
    float minConvexityValue;
    float minInertiaRatioValue;
    int maxThresholdValue;
    int minAreaValue;
    int minThresholdValue;

    CheckBox filterByAreaBtn;
    CheckBox filterByCircularityBtn;
    CheckBox filterByConvexityBtn;
    CheckBox filterByInertiaBtn;
    CheckBox maxThresholdBtn;
    CheckBox minThresholdBtn;

    SeekBar filterByAreaVal;
    SeekBar filterByCircularityVal;
    SeekBar filterByConvexityVal;
    SeekBar filterByInertiaVal;
    SeekBar maxThresholdVal;
    SeekBar minThresholdVal;

    TextView cellCount;
    TextView filterByAreaValLabel;
    TextView filterByCircularityValLabel;
    TextView filterByConvexityValLabel;
    TextView filterByInertiaValLabel;
    TextView maxThresholdValLabel;
    TextView minThresholdValLabel;

    static {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
            Log.d("OpenCV", "OpenCV not loaded");
        }
    }

    private void clearBuffer() {

        if (currentPhotoPath == null) {
            return;
        }

        File fdelete = new File(currentPhotoPath);
        if (!currentPhotoPath.isEmpty()) {
            if (fdelete.exists()) {
                if (fdelete.delete()) {
                    Toast.makeText(MainActivity.this, "Buffer cleaned", Toast.LENGTH_SHORT).show();
                    resetImageView();
                    currentPhotoPath = "";
                    if (!rawImage.empty())
                        rawImage.release();

                } else {
                    Toast.makeText(MainActivity.this, "Failed to clean buffer", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            Toast.makeText(this, "Image buffer is empty, nothing to delete", Toast.LENGTH_SHORT).show();
        }

        if (currentPhotoPathMarked == null) {
            return;
        }

        File fdeleteMarked = new File(currentPhotoPathMarked);
        if (!currentPhotoPathMarked.isEmpty()) {
            if (fdeleteMarked.exists()) {
                if (fdeleteMarked.delete()) {
                    Toast.makeText(MainActivity.this, "Marked buffer cleaned", Toast.LENGTH_SHORT).show();
                    resetImageView();
                    currentPhotoPathMarked = "";
                    if (!rawImage.empty())
                        rawImage.release();

                } else {
                    Toast.makeText(MainActivity.this, "Failed to clean marked buffer", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            Toast.makeText(this, "Marked buffer is empty, nothing to delete", Toast.LENGTH_SHORT).show();
        }
    }

    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, "Permission denied to read your External storage. Please grant permissions and Try again.", Toast.LENGTH_SHORT).show();
                Handler handler = new Handler();
                // Actions to do after 2 seconds
                handler.postDelayed(this::finish, 2000);
            }
        }
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA},
                    1);
        }

    }

    // Checkbox Handlers

    public void onCheckboxClicked(View view) {
        // Is the view now checked?
        boolean checked = ((CheckBox) view).isChecked();

        // Check which checkbox was clicked
        switch (view.getId()) {
            case R.id.minThresholdBtn:
                if (checked) {
                    minThresholdVal.setVisibility(View.VISIBLE);
                } else {
                    minThresholdVal.setVisibility(View.INVISIBLE);
                    minThresholdVal.setProgress(0);
                    minThresholdValue = 0;
                }
                break;
            case R.id.maxThresholdBtn:
                if (checked) {
                    maxThresholdVal.setVisibility(View.VISIBLE);
                } else {
                    maxThresholdVal.setVisibility(View.INVISIBLE);
                    maxThresholdVal.setProgress(0);
                    maxThresholdValue = 0;
                }
                break;
            case R.id.filterByAreaBtn:
                if (checked) {
                    filterByAreaVal.setVisibility(View.VISIBLE);
                } else {
                    filterByAreaVal.setVisibility(View.INVISIBLE);
                    filterByAreaVal.setProgress(0);
                    minAreaValue = 0;
                }
                break;
            case R.id.filterByCircularityBtn:
                if (checked) {
                    filterByCircularityVal.setVisibility(View.VISIBLE);
                } else {
                    filterByCircularityVal.setVisibility(View.INVISIBLE);
                    filterByCircularityVal.setProgress(0);
                    minCircularityValue = 0.f;
                }
                break;
            case R.id.filterByConvexityBtn:
                if (checked) {
                    filterByConvexityVal.setVisibility(View.VISIBLE);
                } else {
                    filterByConvexityVal.setVisibility(View.INVISIBLE);
                    filterByConvexityVal.setProgress(0);
                    minConvexityValue = 0.f;
                }
                break;
            case R.id.filterByInertiaBtn:
                if (checked) {
                    filterByInertiaVal.setVisibility(View.VISIBLE);
                } else {
                    filterByInertiaVal.setVisibility(View.INVISIBLE);
                    filterByInertiaVal.setProgress(0);
                    minInertiaRatioValue = 0.f;
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clearBuffer(); // Cleanup any existing files before shutdown
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        currentPhotoPath = "";
        thresholdValue = 1;
        cellCountValue = -1;

        rawImage = new Mat();

        //Setup Image View
        imageView = findViewById(R.id.image);
        resetImageView();

        //Check and Request Permissions if Needed
        checkPermissions();

        //Setup buttons and other UI elements
        Button captureButton = findViewById(R.id.captureButton);
        Button clearMemory = findViewById(R.id.clearMemory);
        Button countCells = findViewById(R.id.countCells);

        cellCount = (TextView) findViewById(R.id.cellCount);

        minThresholdBtn = findViewById(R.id.minThresholdBtn);
        maxThresholdBtn = findViewById(R.id.maxThresholdBtn);
        filterByAreaBtn = findViewById(R.id.filterByAreaBtn);
        filterByCircularityBtn = findViewById(R.id.filterByCircularityBtn);
        filterByConvexityBtn = findViewById(R.id.filterByConvexityBtn);
        filterByInertiaBtn = findViewById(R.id.filterByInertiaBtn);


        minThresholdVal = findViewById(R.id.minThresholdVal);
        maxThresholdVal = findViewById(R.id.maxThresholdVal);
        filterByAreaVal = findViewById(R.id.filterByAreaVal);
        filterByCircularityVal = findViewById(R.id.filterByCircularityVal);
        filterByConvexityVal = findViewById(R.id.filterByConvexityVal);
        filterByInertiaVal = findViewById(R.id.filterByInertiaVal);

        minThresholdValLabel = findViewById(R.id.minThresholdValLbl);
        maxThresholdValLabel = findViewById(R.id.maxThresholdValLbl);
        filterByAreaValLabel = findViewById(R.id.filterByAreaValLbl);
        filterByCircularityValLabel = findViewById(R.id.filterByCircularityValLbl);
        filterByConvexityValLabel = findViewById(R.id.filterByConvexityValLbl);
        filterByInertiaValLabel = findViewById(R.id.filterByInertiaValLbl);

        minThresholdValLabel.setText(String.valueOf(minThresholdValue));
        maxThresholdValLabel.setText(String.valueOf(maxThresholdValue));
        filterByAreaValLabel.setText(String.valueOf(minAreaValue));
        filterByCircularityValLabel.setText(String.valueOf(minCircularityValue));
        filterByConvexityValLabel.setText(String.valueOf(minConvexityValue));
        filterByInertiaValLabel.setText(String.valueOf(minInertiaRatioValue));

        // SeekBar Handlers

        minThresholdVal.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                minThresholdValue = i;
                minThresholdValLabel.setText(String.valueOf(minThresholdValue));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Not implemented
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Not implemented
            }
        });

        maxThresholdVal.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                maxThresholdValue = i;
                maxThresholdValLabel.setText(String.valueOf(maxThresholdValue));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Not implemented
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Not implemented
            }
        });

        filterByAreaVal.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                minAreaValue = i;
                filterByAreaValLabel.setText(String.valueOf(minAreaValue));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Not implemented
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Not implemented
            }
        });

        filterByCircularityVal.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                minCircularityValue = (float) i / 100;
                filterByCircularityValLabel.setText(String.valueOf(minCircularityValue));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Not implemented
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Not implemented
            }
        });

        filterByConvexityVal.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                minConvexityValue = (float) i / 100;
                filterByConvexityValLabel.setText(String.valueOf(minConvexityValue));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Not implemented
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Not implemented
            }
        });

        filterByInertiaVal.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                minInertiaRatioValue = (float) i / 100;
                filterByInertiaValLabel.setText(String.valueOf(minInertiaRatioValue));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Not implemented
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Not implemented
            }
        });

        clearMemory.setOnClickListener(v -> {
            clearBuffer();
            cellCount.setText("Cell Count: -1");
        });

        captureButton.setOnClickListener(v -> dispatchTakePictureIntent());

        countCells.setOnClickListener(v -> getCellsCount());
    }

    private void getCellsCount() {

        if (rawImage.empty()) {
            Toast.makeText(MainActivity.this, "Image not available. Take an image first and try again.", Toast.LENGTH_LONG).show();
            return;
        }

        // Get param values
        SimpleBlobDetector_Params params = new SimpleBlobDetector_Params();

        if (minThresholdBtn.isChecked()) {
            params.set_minThreshold(minThresholdValue);
        }

        if (maxThresholdBtn.isChecked()) {
            params.set_maxThreshold(maxThresholdValue);
        }

        if (filterByAreaBtn.isChecked()) {
            params.set_filterByArea(true);
            params.set_minArea(minAreaValue);
        }

        if (filterByCircularityBtn.isChecked()) {
            params.set_filterByCircularity(true);
            params.set_minCircularity(minCircularityValue);
        }

        if (filterByConvexityBtn.isChecked()) {
            params.set_filterByConvexity(true);
            params.set_minConvexity(minConvexityValue);
        }

        if (filterByInertiaBtn.isChecked()) {
            params.set_filterByInertia(true);
            params.set_minInertiaRatio(minInertiaRatioValue);
        }

        // Detect
        SimpleBlobDetector detector = SimpleBlobDetector.create(params);

        MatOfKeyPoint keypoint = new MatOfKeyPoint();

        detector.detect(rawImage, keypoint);

        KeyPoint[] vals = keypoint.toArray();

        // Set outputs

        Mat markedImage = rawImage.clone();

        // Custom image marking (drawing detected keypoints on a new temporary image)
        int counter = 0;
        for (KeyPoint point : keypoint.toArray()) {
            Imgproc.circle(markedImage, point.pt, (int) point.size * 3, new Scalar(255, 255, 0), 15);
            Imgproc.putText(markedImage,                          // Matrix obj of the image
                    String.valueOf(++counter),          // Text to be added
                    new Point(point.pt.x, point.pt.y - 75),               // point
                    FONT_HERSHEY_SIMPLEX,      // font face
                    3,                               // font scale
                    new Scalar(255, 255, 0),             // Scalar object for color
                    5                                // Thickness
            );

        }

        currentPhotoPathMarked = currentPhotoPath + "marked.jpg";

        imwrite(currentPhotoPathMarked, markedImage);

        setImageView(currentPhotoPathMarked);

        cellCount.setText("Cell Count: " + vals.length);

    }


    private void resetImageView() {
        if (imageView != null)
            imageView.setImageDrawable(getResources().getDrawable(R.drawable.placeholder));
    }

    private void setImageView(String path) {
        if (imageView != null) {
            if (!path.isEmpty()) {
                myBitmap = BitmapFactory.decodeFile(path);
                imageView.setImageBitmap(myBitmap);
            } else {
                resetImageView();
            }
        }

    }

    private final BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS) {
                Log.i("OpenCV", "OpenCV loaded successfully");
            } else {
                super.onManagerConnected(status);
            }
        }
    };


    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }


    static final int REQUEST_TAKE_PHOTO = 1;

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.bnb_diagnostics.fileProvider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    static final int REQUEST_IMAGE_CAPTURE = 1;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            try {
                if (!currentPhotoPath.isEmpty())
                    rawImage = imread(currentPhotoPath);
                imwrite(currentPhotoPath, rawImage); // required to set image to portrait mode?
                setImageView(currentPhotoPath);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an matImage file name
        //String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        //String imageFileName = "JPEG_" + timeStamp + "_";
        String imageFileName = "Raw_Image";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",        /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }


}
