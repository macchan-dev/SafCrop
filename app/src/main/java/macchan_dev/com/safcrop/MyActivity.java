package macchan_dev.com.safcrop;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class MyActivity extends Activity implements View.OnClickListener {
    private static final int REQUEST_CODE_OPEN_GALLERY = 100;
    private static final int REQUEST_CODE_PIC_CROP = 200;

    private ImageView mImageView;
    private String mCroppedFilename;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        mImageView = (ImageView) findViewById(R.id.imageView);
        Button openGallery = (Button) findViewById(R.id.open_gallery);
        openGallery.setOnClickListener(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.open_gallery:
                // open gallery
                Intent intent = new Intent();
                intent.setType("image/*");
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                } else {
                    intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                }
                startActivityForResult(intent, REQUEST_CODE_OPEN_GALLERY);
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_OPEN_GALLERY) {
            if (resultCode != RESULT_OK) {
                return;
            }
            // filename
            Date date = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.JAPANESE);
            mCroppedFilename = sdf.format(date) + ".jpg";
            File file = new File(getExternalFilesDir(null), mCroppedFilename);
            // save absolute path
            mCroppedFilename = file.getAbsolutePath();

            // crop
            final Intent cropIntent = new Intent("com.android.camera.action.CROP");

            Uri imageData = data.getData();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                // convert "file://" from "content://"
                ParcelFileDescriptor parcelFileDescriptor = null;
                FileInputStream from = null;
                FileOutputStream to = null;
                try {
                    parcelFileDescriptor = getContentResolver().openFileDescriptor(imageData, "r");
                    FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                    from = new FileInputStream(fileDescriptor);
                    File tempOutFile = new File(getExternalFilesDir(null), "crop_temp");
                    to = new FileOutputStream(tempOutFile);

                    byte[] buffer = new byte[4096]; // To hold file contents
                    int bytes_read; // How many bytes in buffer
                    while ((bytes_read = from.read(buffer)) != -1) {
                        to.write(buffer, 0, bytes_read);
                    }

                    imageData = Uri.fromFile(tempOutFile);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                } finally {
                    if (to != null) {
                        try {
                            to.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (from != null) {
                        try {
                            from.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (parcelFileDescriptor != null) {
                        try {
                            parcelFileDescriptor.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            cropIntent.setDataAndType(imageData, "image/*");
            cropIntent.putExtra("crop", "true");
            cropIntent.putExtra("aspectX", 1);
            cropIntent.putExtra("aspectY", 1);
            cropIntent.putExtra("outputX", 256);
            cropIntent.putExtra("outputY", 256);
            cropIntent.putExtra("scale", true);
            // Cropped image file is saved in storage.
            cropIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
            cropIntent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.name());

            // crop dialog.
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
            alertDialog.setTitle(getString(R.string.crop));
            alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // go to crop.
                    startActivityForResult(cropIntent, REQUEST_CODE_PIC_CROP);
                }
            });
            alertDialog.create();
            alertDialog.show();
        } else if (requestCode == REQUEST_CODE_PIC_CROP) {
            if (resultCode != RESULT_OK) {
                return;
            }
            // shome ImageView with Bitmap.
            Bitmap bitmap = BitmapFactory.decodeFile(mCroppedFilename);
            mImageView.setImageBitmap(bitmap);
        }
    }
}
