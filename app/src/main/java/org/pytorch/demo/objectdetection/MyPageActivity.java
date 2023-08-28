package org.pytorch.demo.objectdetection;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.VectorDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.util.Pair;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
public class MyPageActivity extends AppCompatActivity implements Runnable {
    private boolean isSelectBtnChecked = false;
    private ResultView mResultView;
    private ArrayList<Result> results = new ArrayList<>();
    private ImageView imageView;
    private ImageButton mButtonDetect;
    private Bitmap mBitmap = null;
    private Module mModule = null;
    private float mImgScaleX, mImgScaleY, mIvScaleX, mIvScaleY, mStartX, mStartY;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_page);

        mBitmap = BitmapFactory.decodeFile(mypage_upload.currentImagePath);


        imageView = findViewById(R.id.imageView);
        imageView.setImageBitmap(mBitmap);
        mResultView = findViewById(R.id.resultView);
        mResultView.setVisibility(View.INVISIBLE);

        final Button buttonSelect = findViewById(R.id.selectbutton); //앨범 재선택
        buttonSelect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mResultView.setVisibility(View.INVISIBLE);

                final CharSequence[] options = {"사진 선택", "사진 촬영", "취소"};
                AlertDialog.Builder builder = new AlertDialog.Builder(MyPageActivity.this);
                builder.setTitle("이미지");

                builder.setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        if (options[item].equals("사진 선택")) {
                            Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            startActivityForResult(takePicture, 0);
                        } else if (options[item].equals("사진 촬영")) {
                            Intent pickPhoto = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                            startActivityForResult(pickPhoto, 1);
                        } else if (options[item].equals("취소")) {
                            dialog.dismiss();
                        }
                    }
                });
                builder.show();
            }
        });

        mButtonDetect = findViewById(R.id.checkButton); //체크버튼(detect)
        mButtonDetect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mButtonDetect.setEnabled(false);

                mImgScaleX = (float) mBitmap.getWidth() / PrePostProcessor.mInputWidth;
                mImgScaleY = (float) mBitmap.getHeight() / PrePostProcessor.mInputHeight;

                mIvScaleX = (mBitmap.getWidth() > mBitmap.getHeight() ? (float) imageView.getWidth() / mBitmap.getWidth() : (float) imageView.getHeight() / mBitmap.getHeight());
                mIvScaleY = (mBitmap.getHeight() > mBitmap.getWidth() ? (float) imageView.getHeight() / mBitmap.getHeight() : (float) imageView.getWidth() / mBitmap.getWidth());

                mStartX = (imageView.getWidth() - mIvScaleX * mBitmap.getWidth()) / 2;
                mStartY = (imageView.getHeight() - mIvScaleY * mBitmap.getHeight()) / 2;

                Thread thread = new Thread(MyPageActivity.this);
                thread.start();
            }
        });

        try {
            mModule = LiteModuleLoader.load(MainActivity.assetFilePath(getApplicationContext(), "best0418.torchscript.ptl"));
            BufferedReader br = new BufferedReader(new InputStreamReader(getAssets().open("whole.txt")));
            String line;
            List<String> classes = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                classes.add(line);
            }
            PrePostProcessor.mClasses = new String[classes.size()];
            classes.toArray(PrePostProcessor.mClasses);
        } catch (IOException e) {
            Log.e("Object Detection", "Error reading assets", e);
            finish();
        }
        final Button adaptButton = findViewById(R.id.adaptButton);
        adaptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // SharedPreferences 초기화
                SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();

                // 전체 높이를 계산
                double BOX_HEIGHT = 0;
                for (Result r : results) {
                    double h = (double) r.rect.height();
                    BOX_HEIGHT += h;
                }

                // 각 결과에 대한 비율을 계산하고 저장
                for (Result result : results) {
                    double ratio = (double) result.rect.height() / BOX_HEIGHT;
                    String ratioText = String.format("%.1f", ratio * 100) + "%";

                    switch (result.classIndex) {
                        case 0:  // 머리
                            editor.putString("head_ratio", ratioText);
                            break;
                        case 1:  // 몸통
                            editor.putString("up_ratio", ratioText);
                            break;
                        case 2:  // 다리
                            editor.putString("low_ratio", ratioText);
                            break;
                    }
                }

                // SharedPreferences에 변경 사항을 저장합니다.
                editor.apply();
            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_CANCELED) {
            switch (requestCode) {
                case 0:
                    if (resultCode == RESULT_OK && data != null) {
                        mBitmap = (Bitmap) data.getExtras().get("data");
                        Matrix matrix = new Matrix();
                        matrix.postRotate(90.0f);
                        mBitmap = Bitmap.createBitmap(mBitmap, 0, 0, mBitmap.getWidth(), mBitmap.getHeight(), matrix, true);
                        imageView.setImageBitmap(mBitmap);
                    }
                    break;
                case 1:
                    if (resultCode == RESULT_OK && data != null) {
                        Uri selectedImage = data.getData();
                        String[] filePathColumn = {MediaStore.Images.Media.DATA};
                        if (selectedImage != null) {
                            Cursor cursor = getContentResolver().query(selectedImage,
                                    filePathColumn, null, null, null);
                            if (cursor != null) {
                                cursor.moveToFirst();
                                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                                String picturePath = cursor.getString(columnIndex);
                                mBitmap = BitmapFactory.decodeFile(picturePath);
                                Matrix matrix = new Matrix();
                                matrix.postRotate(90.0f);
                                mBitmap = Bitmap.createBitmap(mBitmap, 0, 0, mBitmap.getWidth(), mBitmap.getHeight(), matrix, true);
                                imageView.setImageBitmap(mBitmap);
                                cursor.close();
                            }
                        }
                    }
                    break;
            }
        }
    }

    @Override
    public void run() {
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(mBitmap, PrePostProcessor.mInputWidth, PrePostProcessor.mInputHeight, true);
        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(resizedBitmap, PrePostProcessor.NO_MEAN_RGB, PrePostProcessor.NO_STD_RGB);
        IValue[] outputTuple = mModule.forward(IValue.from(inputTensor)).toTuple();
        final Tensor outputTensor = outputTuple[0].toTensor();
        final float[] outputs = outputTensor.getDataAsFloatArray();
        final ArrayList<Result> newResults = PrePostProcessor.outputsToNMSPredictions(outputs, mImgScaleX, mImgScaleY, mIvScaleX, mIvScaleY, mStartX, mStartY);

        runOnUiThread(() -> {
            mButtonDetect.setEnabled(true);
            mResultView.setResults(newResults);
            mResultView.invalidate();
            mResultView.setVisibility(View.VISIBLE);

            // 리스트 업데이트
            results.clear();
            results.addAll(newResults);

            // SharedPreferences 초기화
            SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();

            // 전체 높이를 계산
            double BOX_HEIGHT = 0;
            for (Result r : newResults) {
                double h = (double) r.rect.height();
                BOX_HEIGHT += h;
            }

            // 각 결과에 대한 비율을 계산하고 저장
            for (Result result : newResults) {
                double ratio = (double) result.rect.height() / BOX_HEIGHT;
                String ratioText = String.format("%.1f", ratio * 100) + "%";

                switch (result.classIndex) {
                    case 0:  // 머리
                        editor.putString("head_ratio", ratioText);
                        break;
                    case 1:  // 몸통
                        editor.putString("up_ratio", ratioText);
                        break;
                    case 2:  // 다리
                        editor.putString("low_ratio", ratioText);
                        break;
                }
            }

            // SharedPreferences에 변경 사항을 저장합니다.
            editor.apply();
        });
    }

}