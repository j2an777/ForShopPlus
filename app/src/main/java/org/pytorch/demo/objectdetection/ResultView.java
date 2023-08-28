// Copyright (c) 2020 Facebook, Inc. and its affiliates.
// All rights reserved.
//
// This source code is licensed under the BSD-style license found in the
// LICENSE file in the root directory of this source tree.

package org.pytorch.demo.objectdetection;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.content.Context;
import android.graphics.DashPathEffect;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.content.res.Resources;
import androidx.core.content.ContextCompat;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ResultView extends View {

    private Paint paintLine;
    private Paint paint;
    private ArrayList<Result> mResults;

    private String differenceHead = "";
    private String differenceUp = "";
    private String differenceLow = "";


    public ResultView(Context context) {
        super(context);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int screenWidth = getWidth();
        int screenHeight = getHeight();
        int fixedX = (int) (screenWidth * 0.8);
        int fixedY = screenHeight / 2;

        Bitmap bH = BitmapFactory.decodeResource(getResources(), R.drawable.head);
        Bitmap bU = BitmapFactory.decodeResource(getResources(), R.drawable.up);
        Bitmap bL = BitmapFactory.decodeResource(getResources(), R.drawable.low);

        bH = Bitmap.createScaledBitmap(bH, 50, 50, false);
        bU = Bitmap.createScaledBitmap(bU, 50, 50, false);
        bL = Bitmap.createScaledBitmap(bL, 50, 50, false);


// 사각형을 그릴 Paint 객체 생성
        Paint rectPaint = new Paint();
        rectPaint.setColor(Color.GRAY);  // 회색 설정
        rectPaint.setAlpha(130);  // 투명도 설정 (약 30%)

// 사각형의 너비와 높이
        int rectWidth = 250;
        int rectHeight = 280;

// 사각형의 시작점 설정 (예: 이미지의 시작점을 기준으로)
        int startX = fixedX - 20;  // 왼쪽 여백 20을 뺀 위치
        int startY = fixedY - 100 - 20;  // 첫 번째 이미지의 시작점과 위쪽 여백 20을 뺀 위치

// 모서리 둥근 사각형을 그림
        float rx = 20; // x축 방향으로 둥근 정도
        float ry = 20; // y축 방향으로 둥근 정도

        canvas.drawRoundRect(startX, startY, startX + rectWidth, startY + rectHeight, rx, ry, rectPaint);

        paint = new Paint();
        paintLine = new Paint();
        double BOX_HEIGHT = 0;
        int resultLeft = 0;
        if (mResults == null) {
            paint.setColor(Color.RED);
            paint.setTextSize(20);
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            paint.setStrokeWidth(3);
            canvas.drawText("인물이 인식되지 않습니다.", 270, 1000, paint);
            return;
        }


        for (Result r : mResults) {
            if(r.classIndex == 1) {
                resultLeft = r.rect.left;
            }
            double h = (double)r.rect.height();
            BOX_HEIGHT += h;
        }

        // 먼저 이미지를 그립니다. 이 부분이 루프 바깥에 있어야 합니다.
        canvas.drawBitmap(bH, fixedX, fixedY - 100 + 5, null);
        canvas.drawBitmap(bU, fixedX, fixedY + 5, null);
        canvas.drawBitmap(bL, fixedX, fixedY + 100 + 8, null);

        if (mResults.isEmpty()) {
            paint.setColor(Color.RED);
            paint.setTextSize(50);
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            paint.setStrokeWidth(3);
            canvas.drawText("인물이 인식되지 않습니다.", 270, 1000, paint);
            return;
        }


        // 다른 로직은 그대로 유지합니다.
        for (Result result : mResults) {
            double ratio = (double)result.rect.height() / BOX_HEIGHT;
            paint.setColor(Color.WHITE);
            paint.setTextSize(45);
            String ratioText = String.format("%.1f", ratio*100);

            // 이미지 위에 텍스트를 덧붙입니다.
            if (result.classIndex == 0) {
                canvas.drawText(ratioText, fixedX + 90, fixedY - 50, paint);
            }
            else if (result.classIndex == 1) {
                canvas.drawText(ratioText, fixedX + 90, fixedY + 50, paint);
            }
            else {
                canvas.drawText(ratioText, fixedX + 90, fixedY + 150, paint);
            }

            if (result.classIndex == 2) { // 클래스 인덱스가 2인 경우는 발 (로우, 하체)를 의미합니다.
                int lineY = 1100; // 발판의 y 좌표
                int lineMinY = (int) (lineY - (lineY * 0.05)); // y 좌표 범위의 최소값
                int lineMaxY = (int) (lineY + (lineY * 0.05)); // y 좌표 범위의 최대값

                // 발이 발판의 범위 내에 있는지 확인하여 색상 결정
                if (result.rect.bottom >= lineMinY && result.rect.bottom <= lineMaxY) {
                    paintLine.setColor(Color.GREEN); // 발이 발판 내에 있을 경우 초록색 발판
                } else {
                    paintLine.setColor(Color.RED); // 발이 발판 밖에 있을 경우 빨간색 발판
                }

                // 발판 그리기
                canvas.drawLine(400, lineY, 700 , lineY, paintLine); // 발판을 가로로 그리기
            }
        }
        // BOX_HEIGHT 계산 로직
        for (Result r : mResults) {
            double h = (double) r.rect.height();
            BOX_HEIGHT += h;
        }

        SharedPreferences sharedPreferences = getContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        Paint lightPaint = new Paint();

        for (Result result : mResults) {
            double ratio = (double)result.rect.height() / BOX_HEIGHT;
            double diff;

        }

    }



    public Map<String, String> getRatios() {
        Map<String, String> ratios = new HashMap<>();

        if (mResults == null) {  // 추가한 부분
            return ratios;
        }

        double BOX_HEIGHT = 0;
        for (Result r : mResults) {
            double h = (double) r.rect.height();
            BOX_HEIGHT += h;
        }

        for (Result result : mResults) {
            double ratio = (double) result.rect.height() / BOX_HEIGHT;
            String ratioText = String.format("%.1f", ratio * 100);

            switch (result.classIndex) {
                case 0:
                    ratios.put("head_ratio", ratioText);
                    break;
                case 1:
                    ratios.put("up_ratio", ratioText);
                    break;
                case 2:
                    ratios.put("low_ratio", ratioText);
                    break;
            }
        }

        return ratios;
    }

    private void saveRatiosToSharedPreferences() {
        Map<String, String> ratios = getRatios();

        SharedPreferences sharedPreferences = getContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        for (Map.Entry<String, String> entry : ratios.entrySet()) {
            editor.putString(entry.getKey(), entry.getValue());
        }
        editor.apply();
    }


    public ResultView(Context context, AttributeSet attrs){
        super(context, attrs);
    }

    public void setResults(ArrayList<Result> results) {
        mResults = results;
    }
}
