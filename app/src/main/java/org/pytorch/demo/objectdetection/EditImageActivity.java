package org.pytorch.demo.objectdetection;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;

import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.graphics.RectF;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import okhttp3.OkHttpClient;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class EditImageActivity extends AppCompatActivity {

    private static final String baseURL = "https://e423-210-115-229-182.ngrok-free.app";
    private static final String SHARED_PREFS_NAME = "DrawnCoordinates";
    private static final String COORDINATES_KEY = "coordinates";

    private String imagePath;
    private DrawingImageView drawingImageView;

    private ImageView imageView1;
    private ImageView imageView2;

    public static OkHttpClient.Builder getUnsafeOkHttpClient() {
        try {
            final TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };

            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager)trustAllCerts[0]);
            builder.hostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
            // Timeout 설정 추가
            builder.connectTimeout(1000, TimeUnit.SECONDS); // 연결 시간 초과 (15초)
            builder.readTimeout(1000, TimeUnit.SECONDS);    // 읽기 시간 초과 (30초)
            builder.writeTimeout(1000, TimeUnit.SECONDS);    // 읽기 시간 초과 (30초)


            return builder;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        trustAllCertificates();
        setContentView(R.layout.editimage_activity);

        drawingImageView = findViewById(R.id.edit_image_view);
        imagePath = getIntent().getStringExtra("IMAGE_PATH");
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
        drawingImageView.setImageBitmap(bitmap);

        ImageButton backButton = findViewById(R.id.back);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        ImageButton eraseButton = findViewById(R.id.erase_btn);
        eraseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingImageView.toggleEraser();
                showEraseDialog();
            }
        });

        ImageButton resetButton = findViewById(R.id.resetButton);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isFinishing()) {
                    return; // 액티비티가 종료 중이면 다이얼로그를 보여주지 않음
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(EditImageActivity.this, R.style.CustomAlertDialogStyle);

                // 커스텀 레이아웃 로드
                View customLayout = LayoutInflater.from(EditImageActivity.this).inflate(R.layout.custom_alert_buttons, null);
                Button positiveButton = customLayout.findViewById(R.id.customPositiveButton);
                Button negativeButton = customLayout.findViewById(R.id.customNegativeButton);

                builder.setView(customLayout);

                AlertDialog dialog = builder.create();

                // OK 버튼 클릭 리스너 설정
                positiveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        drawingImageView.resetDrawing();
                        dialog.dismiss();
                    }
                });

                // Cancel 버튼 클릭 리스너 설정
                negativeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                    }
                });

                Window window = dialog.getWindow();
                if (window != null) {
                    WindowManager.LayoutParams layoutParams = window.getAttributes();
                    layoutParams.dimAmount = 0.0f; // 어둡게 하지 않을 것이므로 0.0f로 설정합니다.
                    window.setAttributes(layoutParams);
                }

                dialog.show();
            }
        });

        ImageButton uploadButton = findViewById(R.id.uploadButton);
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isFinishing()) {
                    return; // 액티비티가 종료 중이면 다이얼로그를 보여주지 않음
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(EditImageActivity.this, R.style.CustomAlertDialogStyle);

                // 커스텀 레이아웃 로드
                View customLayout = LayoutInflater.from(EditImageActivity.this).inflate(R.layout.custom_alert_buttons2, null);

                builder.setView(customLayout);

                // 여기서 다이얼로그 생성
                final AlertDialog dialog = builder.create();

                Button positiveButton = customLayout.findViewById(R.id.customPositiveButton);
                Button negativeButton = customLayout.findViewById(R.id.customNegativeButton);

                // OK 버튼 동작 정의
                positiveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // 여기에 서버와의 동작 수행
                        Intent intent = new Intent(EditImageActivity.this, MainActivity.class);
                        startActivity(intent);
                    }
                });

                // Cancel 버튼 동작 정의
                negativeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Cancel 버튼을 클릭했을 때의 동작
                        dialog.dismiss();
                    }
                });

                Window window = dialog.getWindow();
                if (window != null) {
                    WindowManager.LayoutParams layoutParams = window.getAttributes();
                    layoutParams.dimAmount = 0.0f; // 어둡게 하지 않을 것이므로 0.0f로 설정합니다.
                    window.setAttributes(layoutParams);
                }

                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }

                dialog.show();
            }
        });

        ImageButton resBtn = findViewById(R.id.resolution_btn);
        resBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveReshapedImage();

                // 1. 커스텀 다이얼로그 생성
                final Dialog dialog = new Dialog(v.getContext(), R.style.CustomAlertDialogStyle);
                dialog.setContentView(R.layout.resolution_start);

                // 2. 다이얼로그의 ImageView 찾기
                ImageView reloadImageView = dialog.findViewById(R.id.loading);

                // 4. ObjectAnimator를 사용해 ImageView 회전
                ObjectAnimator rotate = ObjectAnimator.ofFloat(reloadImageView, "rotation", 0f, 360f);
                rotate.setDuration(50000); // 20 seconds
                rotate.setInterpolator(new LinearInterpolator()); // For smooth rotation
                rotate.setRepeatCount(ObjectAnimator.INFINITE);
                rotate.start();

                dialog.show();
            }
        });
    }

    private void saveReshapedImage() {
        Bitmap originalBitmap = BitmapFactory.decodeFile(imagePath);

        // 지정한 크기로 리사이징
        Bitmap reshapedBitmap = Bitmap.createScaledBitmap(originalBitmap, 256, 256, true);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String timestamp = sdf.format(new Date());

        try {
            File baseDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), ".HighQuality");
            if (!baseDir.exists()) {
                baseDir.mkdirs();
            }

            File reshapedFile = new File(baseDir, "SR_" + timestamp + ".png");
            FileOutputStream fos = new FileOutputStream(reshapedFile);
            reshapedBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();

            Log.d("SavedImage", "Reshaped image saved in: " + reshapedFile.getAbsolutePath());

            // Toast 메시지 표시
            Toast.makeText(EditImageActivity.this, "고화질 이미지로 변환중입니다", Toast.LENGTH_SHORT).show();

            ///고화질모드

            //원본 이미지 서버 전송
            RequestBody requestBody0 = RequestBody.create(MediaType.parse("image/*"), reshapedFile);
            MultipartBody.Part imagePart0 = MultipartBody.Part.createFormData("image", reshapedFile.getName(), requestBody0);
            Retrofit retrofit = new Retrofit.Builder() /////ssl 우회 O
                    .baseUrl(baseURL) //유알
                    .client(getUnsafeOkHttpClient().build())
                    .build();

            ApiService apiService0 = retrofit.create(ApiService.class);
            // 원본 전송
//            Call<ResponseBody> call0 = apiService.uploadImage(imagePart0);
            Call<ResponseBody> call0 = apiService0.test1(imagePart0); //////teasf
            call0.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    Log.d("t00", "고화질 원본 이미지 전송 성공");
                    Call<ResponseBody> call1 = apiService0.test3();
                    call1.enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            // 엔드포인트 호출에 대한 응답 처리
                            if (response.isSuccessful()) {
                                ResponseBody responseBody1 = response.body();
                                if (responseBody1 != null) {
                                    try {
                                        // 이미지 데이터를 읽어와서 비트맵으로 변환
                                        InputStream inputStream = responseBody1.byteStream();
                                        Bitmap imageBitmap = BitmapFactory.decodeStream(inputStream);

                                        // 이미지를 표시할 ImageView에 비트맵 설정
                                        imageView1 =findViewById(R.id.dsb);
                                        imageView1.setImageBitmap(imageBitmap);
                                        imageView1.bringToFront();
                                        inputStream.close();
                                        Log.d("t001", "고화질 비트맵 표시 완료");

                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    } /////
                                }
                            } else {
                                Log.d("t001", "고화질 이미지 전송 실패");
                            }
                        }
                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Log.d("t001", "고화질 비트맵 표시 실패");
                    }
                    });
                }
                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Log.d("t00", "고화질 원본 이미지 전송 실패");
                    if (t instanceof IOException) {
                        Log.e("t00", "IOException: " + t.getMessage());
                    } else {
                        Log.e("t00", "Error: " + t.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void downloadImageWithRetry(String imageUrl) {
        Glide.with(EditImageActivity.this)
                .asBitmap()
                .load(imageUrl)
                .listener(new RequestListener<Bitmap>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                        if (e != null && e.getMessage() != null && e.getMessage().contains("[Errno 2] No such file or directory")) {
                            new android.os.Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    downloadImageWithRetry(imageUrl);
                                }
                            }, 5000);
                        }
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                        // 초기화 부분
                        drawingImageView.resetDrawing();
                        drawingImageView.setImageBitmap(resource);
                        return false;
                    }
                })
                .into(drawingImageView);
    }

    public static void trustAllCertificates() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }

                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
            };

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void showEraseDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialogStyle);
        LayoutInflater inflater = this.getLayoutInflater();
        View customView = inflater.inflate(R.layout.mask_items, null);

        // 버튼 클릭 리스너 설정
        ImageButton smallBrushButton = customView.findViewById(R.id.smallBrush);
        ImageButton mediumBrushButton = customView.findViewById(R.id.mediumBrush);
        ImageButton largeBrushButton = customView.findViewById(R.id.largeBrush);
        ImageButton checkButton = customView.findViewById(R.id.maskcheck);

        smallBrushButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingImageView.setBrushSize(15f);
            }
        });

        mediumBrushButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingImageView.setBrushSize(40f);
            }
        });

        largeBrushButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingImageView.setBrushSize(100f);
            }
        });

        checkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(EditImageActivity.this, R.style.CustomAlertDialogStyle);
                LayoutInflater inflater = getLayoutInflater();
                View dialogView = inflater.inflate(R.layout.resolution_start, null);

                builder.setView(dialogView)
                        .setCancelable(false); // 백 버튼으로 닫히지 않도록 설정, 필요하면 삭제

                AlertDialog dialog = builder.create();

                ImageView reloadImageView = dialogView.findViewById(R.id.loading);

                // 4. ObjectAnimator를 사용해 ImageView 회전
                ObjectAnimator rotate = ObjectAnimator.ofFloat(reloadImageView, "rotation", 0f, 360f);
                rotate.setDuration(5000); // 10 seconds
                rotate.setInterpolator(new LinearInterpolator()); // For smooth rotation
                rotate.setRepeatCount(ObjectAnimator.INFINITE);

                dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface d) {
                        rotate.start();  // Dialog가 보이게 되면 로테이션 애니메이션 시작
                    }
                });

                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

                // 이미지 처리 및 서버 전송 코드
                Bitmap originalBitmap = BitmapFactory.decodeFile(imagePath);
                Bitmap maskBitmap = drawingImageView.getMaskBitmap(originalBitmap.getWidth(), originalBitmap.getHeight());

                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
                String timestamp = sdf.format(new Date());

                try {
                    File baseDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), ".MaskImage");
                    if (!baseDir.exists()) {
                        baseDir.mkdirs();
                    }

                    File originalFile = new File(baseDir, "original_" + timestamp + ".png");
                    FileOutputStream fosOriginal = new FileOutputStream(originalFile);
                    originalBitmap.compress(Bitmap.CompressFormat.PNG, 100, fosOriginal);
                    fosOriginal.flush();
                    fosOriginal.close();

                    File maskFile = new File(baseDir, "mask_" + timestamp + ".png");
                    FileOutputStream fosMask = new FileOutputStream(maskFile);
                    maskBitmap.compress(Bitmap.CompressFormat.PNG, 100, fosMask);
                    fosMask.flush();
                    fosMask.close();

                    Log.d("SavedImages", "Original and mask images saved in: " + baseDir.getAbsolutePath());

                    // 이미지를 서버로 전송하는 코드
                    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl(baseURL)
                            .client(getUnsafeOkHttpClient().build())
                            .build();
                    ApiService apiService = retrofit.create(ApiService.class);

                    RequestBody requestFile1 = RequestBody.create(MediaType.parse("image/*"), originalFile);
                    MultipartBody.Part imagePart1 = MultipartBody.Part.createFormData("image", originalFile.getName(), requestFile1);

                    RequestBody requestFile2 = RequestBody.create(MediaType.parse("image/*"), maskFile);
                    MultipartBody.Part imagePart2 = MultipartBody.Part.createFormData("image", maskFile.getName(), requestFile2);

                    // 이미지 1 전송
                    Call<ResponseBody> call1 = apiService.test0(imagePart1);
                    call1.enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            Log.d("t000", "원본 이미지 전송 성공");
                        }
                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            Log.d("t0000", "원본 이미지 전송 실패");
                        }
                    });

                    // 이미지 2 전송
                    Call<ResponseBody> call2 = apiService.test0(imagePart2);
                    call2.enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            Log.d("t001", "마스크 이미지 전송 성공");

                            // 처리된 이미지 받아오기
                            Call<ResponseBody> callResult = apiService.test2();
                            callResult.enqueue(new Callback<ResponseBody>() {
                                @Override
                                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                    if (response.isSuccessful()) {
                                        ResponseBody responseBody2 = response.body();
                                        if (responseBody2 != null) {
                                            try {
                                                InputStream inputStream2 = responseBody2.byteStream();
                                                Bitmap imageBitmap2 = BitmapFactory.decodeStream(inputStream2);
                                                imageView2 = findViewById(R.id.dsb);
                                                imageView2.setImageBitmap(imageBitmap2);
                                                imageView2.bringToFront();
                                                inputStream2.close();
                                                Log.d("t002", "객체 제거 후 결과 표시 완료");

                                                // 이미지 업데이트 후 다이얼로그 닫기
                                                dialog.dismiss();

                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    } else {
                                        Log.d("t003", "원본, 마스크 이미지 전송 실패");
                                    }
                                }

                                @Override
                                public void onFailure(Call<ResponseBody> call, Throwable t) {
                                    Log.d("t004", "객체 제거 후 결과 표시 실패");
                                }
                            });

                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            Log.d("t005", "원본, 마스크 이미지 전송 실패");
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }

                dialog.show();
            }
        });

        AlertDialog alertDialog = builder.setView(customView).create();

        // Window 객체를 가져와서 위치를 설정합니다.
        Window window = alertDialog.getWindow();
        if (window != null) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(window.getAttributes());
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            layoutParams.gravity = Gravity.BOTTOM;

            // 110dp를 pixels로 변환
            float scale = getResources().getDisplayMetrics().density;
            int pixels = (int) (110 * scale + 0.5f);  // 110dp를 pixel 값으로 변환

            layoutParams.y = pixels;

            window.setAttributes(layoutParams);
        }

        alertDialog.show();
    }

}
