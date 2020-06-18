package com.example.bnb_diagnostics;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;


import java.io.File;
import java.io.IOException;

import static org.opencv.imgcodecs.Imgcodecs.imread;
import static org.opencv.imgcodecs.Imgcodecs.imwrite;

public class MainActivity extends AppCompatActivity {

    ImageView imageView;
    String currentPhotoPath;
    Bitmap myBitmap;
    Mat matImage;

    static {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
            Log.d("OpenCV", "OpenCV not loaded");
        }
    }


    private void deleteTempImage() {
        File fdelete = new File(currentPhotoPath);
        if(!currentPhotoPath.equals("Not Set")) {
            if (fdelete.exists()) {
                if (fdelete.delete()) {
                    Toast.makeText(MainActivity.this, "Deleted", Toast.LENGTH_SHORT).show();
                    resetImageView();
                    currentPhotoPath = "Not Set";
                } else {
                    Toast.makeText(MainActivity.this, "Not Deleted", Toast.LENGTH_SHORT).show();
                }
            }
        }
        else
        {
            Toast.makeText(this, "Buffer is empty, nothing to delete", Toast.LENGTH_SHORT).show();
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

    private void check_permissions()
    {
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE ,Manifest.permission.CAMERA},
                1);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        currentPhotoPath = "Not Set";
        imageView = findViewById(R.id.image);
        resetImageView();

        //Check and Request Permissions if Needed
       check_permissions();

        Button capture_button = findViewById(R.id.capture_button);
        Button count_cells = findViewById(R.id.count_cells);
        Button clear_memory = findViewById(R.id.clear_memory);


        clear_memory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteTempImage();
            }
        });

        capture_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });
        //imageView.setRotation(90);
        count_cells.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Call OpenCV method and pass it the matImage obtained from the camera
                //Toast.makeText(MainActivity.this, "Implement OpenCV Detection", Toast.LENGTH_SHORT).show();



                if (!currentPhotoPath.equals("Not Set")) {
                    matImage = imread(currentPhotoPath);

                    convert_to_grayscale();
                    imwrite(currentPhotoPath, matImage);
                    setImageView(currentPhotoPath);
                    Toast.makeText(MainActivity.this, "Image has been loaded : " + currentPhotoPath, Toast.LENGTH_SHORT).show();
                }
                else
                {
                    Toast.makeText(MainActivity.this, "Image buffer empty, take a picture!", Toast.LENGTH_SHORT).show();
                }


            }
        });
    }

    private void resetImageView()
    {
        if(imageView!=null)
            imageView.setImageDrawable(getResources().getDrawable(R.drawable.placeholder));
    }

    private void setImageView(String path)
    {
        if(imageView!=null) {
             if(!path.equals("Not Set")) {
                 myBitmap = BitmapFactory.decodeFile(path);
                 imageView.setImageBitmap(myBitmap);
             }
                else
            {
                resetImageView();
            }
        }

    }

    public void convert_to_grayscale() {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inDither = true;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

//            byte[] decodedString = Base64.decode(imageAsBase64, Base64.DEFAULT);
//            Bitmap image = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
//
//
////      Bitmap image = decodeSampledBitmapFromFile(imageurl, 2000, 2000);
//            int l = CvType.CV_8UC1; //8-bit grey scale image
//            Mat matImage = new Mat();
//            Utils.bitmapToMat(image, matImage);

            Mat matImageGrey = new Mat();
           // Imgproc.cvtColor(matImage, matImageGrey, Imgproc.COLOR_BGR2GRAY);

            Imgproc.cvtColor(matImage, matImage, Imgproc.COLOR_BGR2GRAY);
//            Params params = new Params();
//            params.set_filterByConvexity(true);
//            params.set_minConvexity(0.2f);
//            params.set_maxConvexity(1.0f);
//            params.set_minThreshold(1);
//            params.set_maxThreshold(255);
            //SimpleBlobDetector detector = SimpleBlobDetector.create();

            //MatOfKeyPoint keypoint = new MatOfKeyPoint();

            //detector.detect(matImageGrey, keypoint);



            //KeyPoint[] vals = keypoint.toArray();
            //successCallback.invoke( "Cell Count : " + vals.length);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i("OpenCV", "OpenCV loaded successfully");
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };


    public void onResume()
    {
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
            try{
                if(!currentPhotoPath.equals("Not Set"))
                    setImageView(currentPhotoPath);
            }
            catch(Exception e)
            {
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
