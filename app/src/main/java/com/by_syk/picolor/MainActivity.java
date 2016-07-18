package com.by_syk.picolor;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v7.graphics.Palette;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.by_syk.picolor.util.C;
import com.by_syk.picolor.util.ExtraUtil;

import java.io.FileNotFoundException;

import uk.co.senab.photoview.PhotoViewAttacher;

public class MainActivity extends Activity {
    private ImageView ivPhoto;

    private PhotoViewAttacher photoViewAttacher = null;

    private BottomSheetBehavior bottomSheetBehavior;

    // 图片
    private Bitmap bitmap = null;
    // 图片特征色信息
    private Palette palette = null;

    private AlertDialog alertDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
    }

    @Override
    protected void onDestroy() {
        if (photoViewAttacher != null) {
            photoViewAttacher.cleanup();
        }

        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
        }

        super.onDestroy();
    }

    private void init() {
        ivPhoto = (ImageView) findViewById(R.id.iv_photo);

        // The MAGIC happens here!
        photoViewAttacher = new PhotoViewAttacher(ivPhoto);
        photoViewAttacher.setScaleType(ImageView.ScaleType.CENTER_CROP);
        photoViewAttacher.setZoomable(true);

        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.nsv_main));
        bottomSheetBehavior.setPeekHeight(getResources().getDimensionPixelSize(R.dimen.card_height));
        //bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    private void choosePic(boolean crop) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        if (crop) {
            intent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.putExtra("crop", "true");
            // 长宽比：1:1
            intent.putExtra("aspectX", 1);
            intent.putExtra("aspectY", 1);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, ExtraUtil.getTempUri(this, false));

            startActivityForResult(intent, 1);
        } else {
            // true to return a Bitmap, false to directly save the cropped image
            // intent.putExtra("return-data", true);

            startActivityForResult(intent, 0);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case 0:
                if (resultCode == RESULT_OK && data != null) {
                    (new LoadDataTask()).execute(data.getData());
                }
                break;
            case 1:
                if (resultCode == RESULT_OK) {
                    (new LoadDataTask()).execute(ExtraUtil.getTempUri(this, true));
                }
        }
    }

    private class LoadDataTask extends AsyncTask<Uri, Integer, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            findViewById(R.id.pb_loading).setVisibility(View.VISIBLE);
        }

        @Override
        protected Boolean doInBackground(Uri... params) {
            if (params.length == 0) {
                return false;
            }
            Uri uri = params[0];
            if (uri == null) {
                return false;
            }

            /*try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            } catch (IOException e) {
                e.printStackTrace();
            }*/
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                // 设置其为true时并不会真正解码图片，但可获得一些图片的参数
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(getContentResolver()
                        .openInputStream(uri), null, options);
                // 计算图片缩小级别（2的对数最佳）
                final int IN_SAMPLE_SIZE = ExtraUtil.calculateInSampleSize(new int[]{4096, 4096},
                        options.outWidth, options.outHeight);
                options.inSampleSize = IN_SAMPLE_SIZE;
                options.inJustDecodeBounds = false;

                bitmap = BitmapFactory.decodeStream(getContentResolver()
                        .openInputStream(uri), null, options);
            } catch (FileNotFoundException | OutOfMemoryError e) {
                e.printStackTrace();
            }
            if (bitmap == null) {
                return false;
            }

            // 提取图片特征色
            // Synchronously
            palette = Palette.from(bitmap).generate();

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);

            if (result) {
                showPic();
                showPalette();
            }

            findViewById(R.id.pb_loading).setVisibility(View.GONE);
        }
    }

    private void showPic() {
        if (bitmap == null) {
            return;
        }

        ivPhoto.setImageDrawable(null);
        ivPhoto.setImageBitmap(bitmap);

        photoViewAttacher.update();

        ivPhoto.startAnimation(AnimationUtils.loadAnimation(this, R.anim.show_pic));
    }

    private void showPalette() {
        if (palette == null) {
            return;
        }

        Palette.Swatch swatch = palette.getVibrantSwatch();
        if (swatch != null) {
            findViewById(R.id.rl_vibrant).setBackgroundColor(swatch.getRgb());
            ((TextView) findViewById(R.id.tv_title_vibrant))
                    .setTextColor(swatch.getTitleTextColor());
            ((TextView) findViewById(R.id.tv_desc_vibrant))
                    .setTextColor(swatch.getBodyTextColor());
        } else {
            findViewById(R.id.rl_vibrant).setBackgroundColor(Color.TRANSPARENT);
        }

        swatch = palette.getLightVibrantSwatch();
        if (swatch != null) {
            findViewById(R.id.rl_vibrant_light).setBackgroundColor(swatch.getRgb());
            ((TextView) findViewById(R.id.tv_title_vibrant_light))
                    .setTextColor(swatch.getTitleTextColor());
            ((TextView) findViewById(R.id.tv_desc_vibrant_light))
                    .setTextColor(swatch.getBodyTextColor());
        } else {
            findViewById(R.id.rl_vibrant_light).setBackgroundColor(Color.TRANSPARENT);
        }

        swatch = palette.getDarkVibrantSwatch();
        if (swatch != null) {
            findViewById(R.id.rl_vibrant_dark).setBackgroundColor(swatch.getRgb());
            ((TextView) findViewById(R.id.tv_title_vibrant_dark))
                    .setTextColor(swatch.getTitleTextColor());
            ((TextView) findViewById(R.id.tv_desc_vibrant_dark))
                    .setTextColor(swatch.getBodyTextColor());
        } else {
            findViewById(R.id.rl_vibrant_dark).setBackgroundColor(Color.TRANSPARENT);
        }

        swatch = palette.getMutedSwatch();
        if (swatch != null) {
            findViewById(R.id.rl_muted).setBackgroundColor(swatch.getRgb());
            ((TextView) findViewById(R.id.tv_title_muted))
                    .setTextColor(swatch.getTitleTextColor());
            ((TextView) findViewById(R.id.tv_desc_muted))
                    .setTextColor(swatch.getBodyTextColor());
        } else {
            findViewById(R.id.rl_muted).setBackgroundColor(Color.TRANSPARENT);
        }

        swatch = palette.getLightMutedSwatch();
        if (swatch != null) {
            findViewById(R.id.rl_muted_light).setBackgroundColor(swatch.getRgb());
            ((TextView) findViewById(R.id.tv_title_muted_light))
                    .setTextColor(swatch.getTitleTextColor());
            ((TextView) findViewById(R.id.tv_desc_muted_light))
                    .setTextColor(swatch.getBodyTextColor());
        } else {
            findViewById(R.id.rl_muted_light).setBackgroundColor(Color.TRANSPARENT);
        }

        swatch = palette.getDarkMutedSwatch();
        if (swatch != null) {
            findViewById(R.id.rl_muted_dark).setBackgroundColor(swatch.getRgb());
            ((TextView) findViewById(R.id.tv_title_muted_dark))
                    .setTextColor(swatch.getTitleTextColor());
            ((TextView) findViewById(R.id.tv_desc_muted_dark))
                    .setTextColor(swatch.getBodyTextColor());
        } else {
            findViewById(R.id.rl_muted_dark).setBackgroundColor(Color.TRANSPARENT);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            choosePic(false);
        }
    }

    private void aboutDialog() {
        alertDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.dia_title_about)
                .setMessage(R.string.about_desc)
                .setPositiveButton(R.string.dia_bt_dismiss, null)
                .create();
        alertDialog.show();
    }

    @Override
    public void onBackPressed() {
        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        return true;
    }

    @TargetApi(23)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_pic:
                if (C.SDK >= 23 && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
                } else {
                    choosePic(false);
                }
                return true;
            case R.id.menu_about:
                aboutDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
