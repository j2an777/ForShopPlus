package org.pytorch.demo.objectdetection;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewStub;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.SwitchCompat;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.app.ActivityCompat;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Map;


public class MainActivity extends AppCompatActivity implements Runnable {
    private static final int REQUEST_CODE_CAMERA_PERMISSION = 200;
    private static final String[] PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;
    private long mLastAnalysisResultTime;

    private SwitchCompat gridSwitch;
    private GridOverlayView gridOverlayView;

    protected HandlerThread mBackgroundThread;
    protected Handler mBackgroundHandler;
    protected Handler mUIHandler;

    private AppCompatButton reverseButton;

    private ImageView mImageView;
    private ResultView mResultView;
    private Button mButtonDetect;
    private ProgressBar mProgressBar;
    private Bitmap mBitmap = null;
    private Module mModule = null;
    private float mImgScaleX, mImgScaleY, mIvScaleX, mIvScaleY, mStartX, mStartY;

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private String currentPhotoPath;

    private FocusCircleView mFocusCircleView;

    // 기존 MainActivity의 멤버 변수 및 상수
    private GridOverlayView mGridOverlayView;
    private SwitchCompat mGridSwitch;
    // 기존 MainActivity의 멤버 변수 및 상수
    private TextureView mTextureView;

    private boolean isAnalysisEnabled = true;

    private TextView timerTextView;

    private boolean isAIEnabled = false;

    boolean gridCheckboxState = true; // 체크박스가 체크된 상태
    boolean aiCheckboxState = true; // AI 체크박스가 체크된 상태 (AI는 켜져 있음)

    private GestureDetector gestureDetector;

    static class AnalysisResult {
        private final ArrayList<Result> mResults;

        public AnalysisResult(ArrayList<Result> results) {
            mResults = results;
        }
    }

    private int timerDuration = 0;  // 선택된 타이머 시간 (기본값은 0)

    private void showTimerDialog(final ImageButton timerButton) {
        if (isFinishing()) {
            return; // 액티비티가 종료 중이면 다이얼로그를 보여주지 않음
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialogStyle);

        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.timer_options, null);

        final ImageButton threeSeconds = view.findViewById(R.id.three_seconds);
        final ImageButton fiveSeconds = view.findViewById(R.id.five_seconds);
        final ImageButton tenSeconds = view.findViewById(R.id.ten_seconds);

        // Button listeners
        threeSeconds.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timerDuration = 3000;
                updateTimerButtonImage(timerButton);
                setButtonImageColor(threeSeconds, Color.BLACK);
                resetButtonImageColors(fiveSeconds, tenSeconds);
            }
        });

        fiveSeconds.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timerDuration = 5000;
                updateTimerButtonImage(timerButton);
                setButtonImageColor(fiveSeconds, Color.BLACK);
                resetButtonImageColors(threeSeconds, tenSeconds);
            }
        });

        tenSeconds.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timerDuration = 10000;
                updateTimerButtonImage(timerButton);
                setButtonImageColor(tenSeconds, Color.BLACK);
                resetButtonImageColors(threeSeconds, fiveSeconds);
            }
        });

        builder.setView(view);
        AlertDialog dialog = builder.create();

        // AlertDialog의 뒤 배경 어두움을 제거합니다.
        Window window = dialog.getWindow();
        if (window != null) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }

        // AlertDialog의 위치와 크기를 조정합니다.
        WindowManager.LayoutParams wlp = new WindowManager.LayoutParams();
        wlp.copyFrom(dialog.getWindow().getAttributes());
        wlp.gravity = Gravity.TOP;  // 위치를 상단으로 설정합니다.
        wlp.y = 230;  // 원하는 위치만큼 y 값을 조정하세요.
        dialog.getWindow().setAttributes(wlp);

        dialog.show();
    }

    private void resetButtonImageColors(ImageButton... buttons) {
        for (ImageButton button : buttons) {
            Drawable drawable = button.getDrawable();
            if (drawable != null) {
                drawable.clearColorFilter();
            }
        }
    }

    private void setButtonImageColor(ImageButton button, int color) {
        Drawable drawable = button.getDrawable();
        if (drawable != null) {
            drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        }
    }

    private void resetButtonColors(Button... buttons) {
        for (Button btn : buttons) {
            btn.setBackgroundColor(Color.TRANSPARENT);
            btn.setTextColor(Color.parseColor("#4E4E4E"));
        }
    }

    private void startTimer() {
        // 타이머 시작시 visibility 설정
        timerTextView.setVisibility(View.VISIBLE);

        new CountDownTimer(timerDuration, 1000) {

            public void onTick(long millisUntilFinished) {
                // 남은 시간 업데이트
                timerTextView.setText(String.valueOf(millisUntilFinished+1000 / 1000));
            }

            public void onFinish() {
                // 타이머 종료시 visibility 설정
                timerTextView.setVisibility(View.INVISIBLE);
                takePicture();
            }
        }.start();
    }

    private void updateTimerText(long secondsLeft) {
        timerTextView.setText(String.valueOf(secondsLeft) + "초");
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        timerTextView = findViewById(R.id.timerTextView);
        timerTextView.setVisibility(View.INVISIBLE);

        mUIHandler = new Handler(getMainLooper());

        mGridOverlayView = findViewById(R.id.grid_overlay_view);
        mGridSwitch = findViewById(R.id.grid_switch);

        // 앱이 처음 실행될 때의 상태 설정
        mGridOverlayView.setVisibility(View.VISIBLE);
        toggleAnalysis(aiCheckboxState); // AI 켜짐
        mGridSwitch.setChecked(true); // grid_switch 켜짐

        mResultView = findViewById(R.id.resultView);

        startBackgroundThread();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS,
                    REQUEST_CODE_CAMERA_PERMISSION);
        } else {
            setupCameraX();
        }

        mFocusCircleView = findViewById(R.id.focus_circle_view);

        // 촬영버튼
        Button takePictureButton = findViewById(R.id.button_take_picture);

        // 손가락을 버튼에 누르고 있을 때와 손가락을 뗐을 때의 이벤트 처리
        takePictureButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                TransitionDrawable transition = (TransitionDrawable) v.getBackground();
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        transition.startTransition(1000); // 300ms 동안 애니메이션
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        transition.reverseTransition(1000); // 300ms 동안 애니메이션을 되돌림
                        break;
                }
                return false;  // onClick 이벤트도 처리하기 위해 false를 반환
            }
        });

        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (timerDuration > 0) {
                    // 타이머가 설정된 경우, 설정된 시간만큼 딜레이를 준 후 사진 촬영
                    timerTextView.setVisibility(View.VISIBLE);  // 타이머 표시
                    new CountDownTimer(timerDuration, 1000) {
                        public void onTick(long millisUntilFinished) {
                            timerTextView.setText(String.valueOf(millisUntilFinished / 1000 + 1));
                        }

                        public void onFinish() {
                            timerTextView.setVisibility(View.INVISIBLE);  // 타이머 숨기기
                            takePicture();
                        }
                    }.start();
                } else {
                    // 타이머가 설정되지 않은 경우, 바로 사진 촬영
                    takePicture();
                }
            }
        });

        AppCompatButton storageButton = findViewById(R.id.storage_btn);
        setLatestImageAsBackground(storageButton);
        storageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });

        // "mydata_btn"
        AppCompatButton myDataButton = findViewById(R.id.mydata_btn);
        myDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, mypage_upload.class);
                startActivity(intent);
            }
        });

        // "gallery_btn"
        ImageButton myGalleryButton = findViewById(R.id.gallery_btn);
        myGalleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, UploadActivity.class);
                startActivity(intent);
            }
        });

        ImageButton settingsButton = findViewById(R.id.settings);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSettingsDialog();
            }
        });

        final ImageButton timerButton = findViewById(R.id.your_timer_button_id);
        updateTimerButtonImage(timerButton);

        timerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (timerDuration > 0) {
                    // 타이머가 설정된 상태면 타이머를 끄고 이미지 업데이트
                    timerDuration = 0;
                    updateTimerButtonImage(timerButton);
                } else {
                    // 타이머가 설정되지 않은 상태면 타이머 설정 이미지
                    showTimerDialog(timerButton);
                }
            }
        });

        mGridSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mGridOverlayView.setVisibility(gridCheckboxState ? View.VISIBLE : View.INVISIBLE);
                    toggleAnalysis(aiCheckboxState);
                } else {
                    mGridOverlayView.setVisibility(View.INVISIBLE); // 격자선 꺼짐
                    toggleAnalysis(false); // AI 꺼짐
                }
            }
        });

        final SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);

        preferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                // 변경된 key에 따라 적절한 TextView를 업데이트
                if (key.equals("head_ratio")) {
                    updateTextView(R.id.headRatioTextView, prefs.getString("head_ratio", "0"));
                } else if (key.equals("up_ratio")) {
                    updateTextView(R.id.upRatioTextView, prefs.getString("up_ratio", "0"));
                } else if (key.equals("low_ratio")) {
                    updateTextView(R.id.lowRatioTextView, prefs.getString("low_ratio", "0"));
                }
            }
        };
        // 리스너 등록
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    private void updateTextView(int textViewId, String value) {
        TextView textView = findViewById(textViewId);
        textView.setText(value.replace("%", "") + "%");
    }
    private void showSettingsDialog() {
        if (isFinishing()) {
            return; // 액티비티가 종료 중이면 다이얼로그를 보여주지 않음
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialogStyle);
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.settings_item, null);

        // AI Button 처리
        ImageButton aiBtn = view.findViewById(R.id.aiBtn);
        TextView aiText = view.findViewById(R.id.ai_text);
        aiBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                aiCheckboxState = !aiCheckboxState;  // 상태 토글
                toggleAnalysis(aiCheckboxState && mGridSwitch.isChecked());

                if(aiCheckboxState) {
                    ((ImageButton) v).setImageResource(R.drawable.checkedai); // 이미지 변경
                    aiText.setTextColor(Color.parseColor("#000000"));
                } else {
                    ((ImageButton) v).setImageResource(R.drawable.ai); // 다른 이미지
                    aiText.setTextColor(Color.parseColor("#4e4e4e"));
                }
            }
        });

        // Grid Button 처리
        ImageButton gridBtn = view.findViewById(R.id.gridBtn);
        TextView gridText = view.findViewById(R.id.grid_text);
        gridBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gridCheckboxState = !gridCheckboxState;  // 상태 토글
                mGridOverlayView.setVisibility(gridCheckboxState && mGridSwitch.isChecked() ? View.VISIBLE : View.INVISIBLE);

                if(gridCheckboxState) {
                    ((ImageButton) v).setImageResource(R.drawable.checkedgrid); // 이미지 변경
                    gridText.setTextColor(Color.parseColor("#000000"));
                } else {
                    ((ImageButton) v).setImageResource(R.drawable.grid); // 다른 이미지
                    gridText.setTextColor(Color.parseColor("#4e4e4e"));
                }
            }
        });

        builder.setView(view);
        AlertDialog dialog = builder.create();

        // AlertDialog의 뒤 배경 어두움을 제거합니다.
        Window window = dialog.getWindow();
        if (window != null) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }

        // AlertDialog의 위치와 크기를 조정합니다.
        WindowManager.LayoutParams wlp = new WindowManager.LayoutParams();
        wlp.copyFrom(dialog.getWindow().getAttributes());
        wlp.gravity = Gravity.TOP;  // 위치를 상단으로 설정합니다.
        wlp.y = 230;  // 원하는 위치만큼 y 값을 조정하세요.
        dialog.getWindow().setAttributes(wlp);

        dialog.show();
    }


    private void toggleAnalysis(boolean enable) {
        isAnalysisEnabled = enable;
        if (isAnalysisEnabled) {
            Toast.makeText(MainActivity.this, "Analyze Image Enabled", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(MainActivity.this, "Analyze Image Disabled", Toast.LENGTH_SHORT).show();
            hideResults();
        }
    }

    private void updateTimerButtonImage(ImageButton button) {
        if (timerDuration == 0) {
            button.setImageResource(R.drawable.timeroff);
        } else {
            button.setImageResource(R.drawable.timeron);
        }
    }

    private void setLatestImageAsBackground(AppCompatButton button) {
        File directory = new File(Environment.getExternalStorageDirectory(), "/DCIM");
        File[] files = directory.listFiles();
        File latestFile = null;
        if (files != null && files.length > 0) {
            Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
            latestFile = files[0];
        }

        if (latestFile != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(latestFile.getAbsolutePath());
            Drawable drawable = new BitmapDrawable(getResources(), bitmap);
            button.setBackground(drawable);

            // 애니메이션 적용
            Animation slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down);
            button.startAnimation(slideDown);
        }
    }

    //초점 맞추기
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
            float x = event.getX();
            float y = event.getY();
            mFocusCircleView.setFocusPoint(new PointF(x, y));
        }
        return true;
    }

    private void takePicture() {
        Bitmap bitmap = mTextureView.getBitmap();

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = timeStamp + "_image.jpg";
        File storageDir = new File(Environment.getExternalStorageDirectory(), "/DCIM");
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        File imageFile = new File(storageDir, fileName);

        // 파일 저장을 위한 OutputStream을 생성
        OutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(imageFile);
            // Bitmap을 JPEG 형식으로 압축하여 파일에 저장
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();

            addImageToGallery(imageFile.getAbsolutePath());

            Toast.makeText(MainActivity.this, "사진이 저장되었습니다.", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "사진 저장에 실패했습니다.", Toast.LENGTH_SHORT).show();
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

//        saveRatiosToSharedPreferences();

        AppCompatButton storageButton = findViewById(R.id.storage_btn);
        setLatestImageAsBackground(storageButton);
    }

    private void saveRatiosToSharedPreferences() {
        Map<String, String> ratios = mResultView.getRatios();

        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        for (Map.Entry<String, String> entry : ratios.entrySet()) {
            editor.putString(entry.getKey(), entry.getValue());
        }
        editor.apply();
    }

    //갤러리에 찍은 사진 추가하기
    private void addImageToGallery(String imagePath) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File imageFile = new File(imagePath);
        Uri contentUri = Uri.fromFile(imageFile);
        mediaScanIntent.setData(contentUri);
        sendBroadcast(mediaScanIntent);
    }

    private static final int REQUEST_IMAGE_PICK = 2;

    //이미지 경로 받아오기
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };

            Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            processSelectedImage(picturePath);
        }

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {

            Intent intent = new Intent(MainActivity.this, MyPageActivity.class);
            startActivity(intent);
        }
    }

    private void processSelectedImage(String imagePath) {
        setContentView(R.layout.image_view_layout);
        ImageView imageView = findViewById(R.id.imageView);
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
        imageView.setImageBitmap(bitmap);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        startBackgroundThread();
    }

    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("ModuleActivity");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 리스너 해제 (메모리 누수 방지)
        getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                .unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            Log.e("Object Detection", "Error on stopping background thread", e);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CODE_CAMERA_PERMISSION) {
            if (grantResults.length >= 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                setupCameraX();
            } else {
                Toast.makeText(
                        this,
                        "You need to grant CAMERA and WRITE_EXTERNAL_STORAGE permissions to use the object detection feature.",
                        Toast.LENGTH_LONG
                ).show();
                finish();
            }
        }
    }

    private void setupCameraX() {
        mTextureView = getCameraPreviewTextureView();
        final PreviewConfig previewConfig = new PreviewConfig.Builder().build();
        final Preview preview = new Preview(previewConfig);
        preview.setOnPreviewOutputUpdateListener(output -> mTextureView.setSurfaceTexture(output.getSurfaceTexture()));

        final ImageAnalysisConfig imageAnalysisConfig =
                new ImageAnalysisConfig.Builder()
                        .setTargetResolution(new Size(480, 640))
                        .setCallbackHandler(mBackgroundHandler)
                        .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
                        .build();
        final ImageAnalysis imageAnalysis = new ImageAnalysis(imageAnalysisConfig);
        imageAnalysis.setAnalyzer((image, rotationDegrees) -> {
            if (SystemClock.elapsedRealtime() - mLastAnalysisResultTime < 0.01) { //0.01
                return;
            }

            final AnalysisResult result = analyzeImage(image, rotationDegrees);
            if (result != null) {
                mLastAnalysisResultTime = SystemClock.elapsedRealtime();
                runOnUiThread(() -> applyToUiAnalyzeImageResult(result));
            }
        });

        CameraX.bindToLifecycle(this, preview, imageAnalysis);
    }

    @WorkerThread
    @Nullable
    private AnalysisResult analyzeImage(ImageProxy image, int rotationDegrees) {
        if (!isAnalysisEnabled) {
            return null;
        }
        try {
            if (mModule == null) {
                mModule = LiteModuleLoader.load(MainActivity.assetFilePath(getApplicationContext(), "best0418.torchscript.ptl"));
                BufferedReader br = new BufferedReader(new InputStreamReader(getAssets().open("whole.txt")));
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        Bitmap bitmap = imgToBitmap(image.getImage());
        Matrix matrix = new Matrix();
        matrix.postRotate(rotationDegrees);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, PrePostProcessor.mInputWidth, PrePostProcessor.mInputHeight, true);

        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(resizedBitmap, PrePostProcessor.NO_MEAN_RGB, PrePostProcessor.NO_STD_RGB);
        IValue[] outputTuple = mModule.forward(IValue.from(inputTensor)).toTuple();
        final Tensor outputTensor = outputTuple[0].toTensor();
        final float[] outputs = outputTensor.getDataAsFloatArray();

        float imgScaleX = (float) bitmap.getWidth() / PrePostProcessor.mInputWidth;
        float imgScaleY = (float) bitmap.getHeight() / PrePostProcessor.mInputHeight;
        float ivScaleX = (float) mResultView.getWidth() / bitmap.getWidth();
        float ivScaleY = (float) mResultView.getHeight() / bitmap.getHeight();

        final ArrayList<Result> results = PrePostProcessor.outputsToNMSPredictions(outputs, imgScaleX, imgScaleY, ivScaleX, ivScaleY, 0, 0);
        return new AnalysisResult(results);
    }

    @UiThread
    protected void applyToUiAnalyzeImageResult(AnalysisResult result) {
        if (isAnalysisEnabled) {
            mResultView.setResults(result.mResults);
            mResultView.invalidate();
            mResultView.setVisibility(View.VISIBLE);
        } else {
            hideResults();
        }
    }

    private Bitmap imgToBitmap(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, out);

        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    @Override
    public void run() {
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(mBitmap, PrePostProcessor.mInputWidth, PrePostProcessor.mInputHeight, true);
        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(resizedBitmap, PrePostProcessor.NO_MEAN_RGB, PrePostProcessor.NO_STD_RGB);
        IValue[] outputTuple = mModule.forward(IValue.from(inputTensor)).toTuple();
        final Tensor outputTensor = outputTuple[0].toTensor();
        final float[] outputs = outputTensor.getDataAsFloatArray();
        final ArrayList<Result> results =  PrePostProcessor.outputsToNMSPredictions(outputs, mImgScaleX, mImgScaleY, mIvScaleX, mIvScaleY, mStartX, mStartY);

        runOnUiThread(() -> {
            mButtonDetect.setEnabled(true);
            mButtonDetect.setText(getString(R.string.detect));
            mProgressBar.setVisibility(ProgressBar.INVISIBLE);
            mResultView.setResults(results);
            mResultView.invalidate();
            mResultView.setVisibility(View.VISIBLE);
        });
    }

    @UiThread
    private void hideResults() {
        mResultView.setVisibility(View.INVISIBLE);
    }

    // 기존 MainActivity의 메서드
    public static String assetFilePath(Context context, String assetName) throws IOException {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }
    // 기존 MainActivity의 메서드

    protected int getContentViewLayoutId() {
        return R.layout.activity_main;
    }

    protected TextureView getCameraPreviewTextureView() {
        mResultView = findViewById(R.id.resultView);
        return ((ViewStub) findViewById(R.id.object_detection_texture_view_stub))
                .inflate()
                .findViewById(R.id.object_detection_texture_view);
    }
}