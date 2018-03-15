package com.example.android.smarthashtaggenerator;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.nfc.Tag;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.FileProvider;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import com.example.android.smarthashtaggenerator.utils.MicrosoftComputerVisionUtils;
import com.example.android.smarthashtaggenerator.utils.NetworkUtils;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<String> {

    private TextView mTakePhotoTV;
    private TextView mChoosePhotoTV;
    private TextView mHistoryPhotoTV;
    private String mVisionURL;

    // For deciding what to do in onActivityResult
    private int ACTIVITYRESULT_ID;

    // For taking photos
    private static final int REQUEST_TAKE_PHOTO = 1;
    private String mCurrentPhotoPath;
    public static final String EXTRA_TAKE_PHOTO = "Take Photo";
    private static final String TAG = MainActivity.class.getSimpleName();
    public static final String VISION_URL_KEY = "ComputerVision URL";
    //public static final String VISION_FILE_KEY = "ComputerVision File";
    public static final String VISION_OBJECT_KEY = "VisionItem";
    private static final int VISION_LOADER_ID = 0;
    private File file;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTakePhotoTV = findViewById(R.id.take_photo_btn);
        mChoosePhotoTV = findViewById(R.id.choose_photo_btn);
        mHistoryPhotoTV = findViewById(R.id.view_history_btn);

        mTakePhotoTV.getBackground().setAlpha(63);
        mChoosePhotoTV.getBackground().setAlpha(63);
        mHistoryPhotoTV.getBackground().setAlpha(63);

        mVisionURL = MicrosoftComputerVisionUtils.buildVisionURL();

        mTakePhotoTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ACTIVITYRESULT_ID = 1;
                dispatchTakePictureIntent();
            }
        });

        mChoosePhotoTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ACTIVITYRESULT_ID = 2;
                Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(pickPhoto , 1);
            }
        });

        mHistoryPhotoTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent viewHistoryIntent = new Intent(MainActivity.this, ViewHistory.class);
                startActivity(viewHistoryIntent);
            }
        });

    }


    //upon taking or choosing image
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        //Uri selectedImage = data.getData();

        switch(ACTIVITYRESULT_ID) {

            case 1:
                if(requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK){
                    //send image to api
                    file = new File(mCurrentPhotoPath);
                    if (file.exists()) {
                        try {
                            Bundle args = new Bundle();
                            args.putString(VISION_URL_KEY, mVisionURL);
                            getSupportLoaderManager().initLoader(VISION_LOADER_ID, args, this);
                            //String visionJSON = NetworkUtils.doHTTPGet(url, file);
                            //ArrayList<MicrosoftComputerVisionUtils.ComputerVisionItem> tags = MicrosoftComputerVisionUtils.parseVisionJSON(visionJSON);
                            /*for (MicrosoftComputerVisionUtils.ComputerVisionItem tag : tags) {
                                Log.d(TAG, "Tag: " + tag);
                            }*/
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }


                }

                break;
            //case from choosing photo:
            case 2:
                if (resultCode == RESULT_OK){
                    //requesting permission
                    //send image to api

                    //send image over to choose photo activity to handle
                    //Intent choosePhotoIntent = new Intent(MainActivity.this, ChoosePhoto.class);
                    //startActivity(choosePhotoIntent);
                    Uri selectedImage = data.getData();
                    String filePath = getPath(this, selectedImage);
                    file = new File(filePath);

                    Bundle args = new Bundle();
                    args.putString(VISION_URL_KEY, mVisionURL);
                    getSupportLoaderManager().initLoader(VISION_LOADER_ID, args, this);

                    //send image to api with AsyncTaskLoader
                }

                break;
        }
    }

    @Override
    public Loader<String> onCreateLoader(int id, Bundle args) {
        String visionURL = null;
        if (args != null) {
            visionURL = args.getString(VISION_URL_KEY);
        }
        return new ComputerVisionLoader(this, visionURL, file);
    }

    @Override
    public void onLoadFinished(Loader<String> loader, String data) {
        Log.d(TAG, "Got tags from loader: " + data);
        if (data != null) {
            MicrosoftComputerVisionUtils.ComputerVisionItem visionItem = new MicrosoftComputerVisionUtils.ComputerVisionItem();
            visionItem.file = file;
            visionItem.tags = MicrosoftComputerVisionUtils.parseVisionJSON(data);
            for (String tag : visionItem.tags) {
                Log.d(TAG, "Tag: " + tag);
            }
            Intent showHashtagsIntent = new Intent(this, ShowTagsActivity.class);
            showHashtagsIntent.putExtra(VISION_OBJECT_KEY, visionItem);
            //showHashtagsIntent.putExtra(VISION_FILE_KEY, file);
            startActivity(showHashtagsIntent);
        }
    }

    @Override
    public void onLoaderReset(Loader<String> loader) {
        // Nothing to do...
    }

    /* INITIALIZE IMAGE FILE AND LET USER TAKE PHOTO */
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
                ex.printStackTrace();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                try {
                    Uri photoURI = FileProvider.getUriForFile(this,
                            "com.example.android.smarthashtaggenerator.fileprovider",
                            photoFile);

                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /* INITIALIZE IMAGE FILE */
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    String getBits(byte b) {
        String result = "";
        for (int i = 0; i < 8; i++) {
            result += (b & (1 << i)) == 0 ? "0" : "1";
        }
        return result;
    }

    public static String getPath(Context context, Uri uri) {
        String result = null;
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = context.getContentResolver().query(uri, proj, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int column_index = cursor.getColumnIndexOrThrow(proj[0]);
                result = cursor.getString(column_index);
            }
            cursor.close();
        }
        if (result == null) {
            Log.d(TAG, "File not found!");
            result = null;
        }
        return result;
    }
}
