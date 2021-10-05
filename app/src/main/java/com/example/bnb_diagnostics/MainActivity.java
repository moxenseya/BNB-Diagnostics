package com.example.bnb_diagnostics;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

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
import android.app.Fragment;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
//import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.SimpleBlobDetector;
import org.opencv.features2d.SimpleBlobDetector_Params;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;

import static org.opencv.imgcodecs.Imgcodecs.imread;
import static org.opencv.imgcodecs.Imgcodecs.imwrite;
import static org.opencv.imgproc.Imgproc.FONT_HERSHEY_SIMPLEX;

public class MainActivity extends AppCompatActivity {

    ImageView imageView;
    String currentPhotoPath;
    String currentPhotoPathMarked;
    Bitmap myBitmap;
    Mat rawImage;

    int threshold_value;
    int cell_count_value;

    int minThresholdValue;
    int maxThresholdValue;
    int minAreaValue;
    float minCircularityValue;
    float minConvexityValue;
    float minInertiaRatioValue;

    SeekBar minThresholdVal;
    SeekBar maxThresholdVal;
    SeekBar filterByAreaVal;
    SeekBar filterByCircularityVal;
    SeekBar filterByConvexityVal;
    SeekBar filterByInertiaVal;

    CheckBox minThresholdBtn;
    CheckBox maxThresholdBtn;
    CheckBox filterByAreaBtn;
    CheckBox filterByCircularityBtn;
    CheckBox filterByConvexityBtn;
    CheckBox filterByInertiaBtn;

    TextView cells_count;

    static {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
            Log.d("OpenCV", "OpenCV not loaded");
        }
    }


    private void clearBuffer() {
        File fdelete = new File(currentPhotoPath);
        if (!currentPhotoPath.equals("Not Set")) {
            if (fdelete.exists()) {
                if (fdelete.delete()) {
                    Toast.makeText(MainActivity.this, "Buffer cleaned", Toast.LENGTH_SHORT).show();
                    resetImageView();
                    currentPhotoPath = "Not Set";
                    if (!rawImage.empty())
                        rawImage.release();

                } else {
                    Toast.makeText(MainActivity.this, "Failed to clean buffer", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            Toast.makeText(this, "Image buffer is empty, nothing to delete", Toast.LENGTH_SHORT).show();
        }

        File fdeleteMarked = new File(currentPhotoPathMarked);
        if (!currentPhotoPathMarked.equals("Not Set")) {
            if (fdeleteMarked.exists()) {
                if (fdeleteMarked.delete()) {
                    Toast.makeText(MainActivity.this, "Marked buffer cleaned", Toast.LENGTH_SHORT).show();
                    resetImageView();
                    currentPhotoPathMarked = "Not Set";
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
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(MainActivity.this, "Permission denied to read your External storage. Please grant permissions and Try again.", Toast.LENGTH_SHORT).show();
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            // Actions to do after 2 seconds
                            finish();

                        }
                    }, 2000);


                }
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void check_permissions() {
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA},
                1);

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


        currentPhotoPath = "Not Set";
        threshold_value = 1;
        cell_count_value = -1;

        rawImage = new Mat();

        //Setup Image View
        imageView = findViewById(R.id.image);
        resetImageView();

        //Check and Request Permissions if Needed
        check_permissions();

        //Setup buttons and other UI elements
        Button capture_button = findViewById(R.id.capture_button);
        //Button apply_threshold_button = findViewById(R.id.apply_threshold);
        Button clear_memory = findViewById(R.id.clear_memory);
        Button count_cells = findViewById(R.id.count_cells);

        cells_count = (TextView)findViewById(R.id.cell_count); // Text to display cells counted result

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

        // SeekBar Handlers

        minThresholdVal.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                minThresholdValue = i;
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

        clear_memory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearBuffer();
                cells_count.setText("Cell Count: -1");
            }
        });

        capture_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });

        count_cells.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                get_cells_count();
            }
        });
    }

    private void get_cells_count() {

        // Old template matching code

//        int match_method = 0;
//        Mat base_image = imread(currentPhotoPath);
//        Mat template = new Mat();
//        try {
//            template = Utils.loadResource(this, R.drawable.template_cell, 1);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        int result_cols = base_image.cols() - base_image.cols() + 1;
//        int result_rows = template.rows() - template.rows() + 1;
//        Mat result = new Mat(result_rows, result_cols, CvType.CV_32FC1);
//
//        Imgproc.matchTemplate(base_image, template, result, match_method);
//        Core.normalize(result, result, 0, 1, Core.NORM_MINMAX, -1, new Mat());
//
//        Core.MinMaxLocResult mmr = Core.minMaxLoc(result);
//
//        Point matchLoc;
//        if (match_method == Imgproc.TM_SQDIFF || match_method == Imgproc.TM_SQDIFF_NORMED) {
//            matchLoc = mmr.minLoc;
//        } else {
//            matchLoc = mmr.maxLoc;
//        }
//
//        // / Show me what you got
//        Imgproc.rectangle(base_image, matchLoc, new Point(matchLoc.x + template.cols(),
//                matchLoc.y + template.rows()), new Scalar(0, 255, 0));
//
//        imwrite(currentPhotoPath, base_image);
//        base_image.release();
//        template.release();
//
//        return result_cols;

        // TODO New SimpleBlobDetector mode


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

        cells_count.setText("Cell Count: " + vals.length);

    }


    private void resetImageView() {
        if (imageView != null)
            imageView.setImageDrawable(getResources().getDrawable(R.drawable.placeholder));
    }

    private void setImageView(String path) {
        if (imageView != null) {
            if (!path.equals("Not Set")) {
                myBitmap = BitmapFactory.decodeFile(path);
                imageView.setImageBitmap(myBitmap);
            } else {
                resetImageView();
            }
        }

    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i("OpenCV", "OpenCV loaded successfully");
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
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
                if (!currentPhotoPath.equals("Not Set"))
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
