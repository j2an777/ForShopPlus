package org.pytorch.demo.objectdetection;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;

import java.util.ArrayList;

public class DrawingImageView extends androidx.appcompat.widget.AppCompatImageView {
    private final Paint paint = new Paint();

    private float startX, startY, endX, endY;  // 사각형을 그리기 위한 변수
    private boolean isRectangleMode = false;   // 사각형 모드인지 아닌지
    private boolean isEraserActive = false;
    private Path path = new Path();
    private Rect rectToDraw = null;
    private float brushSize = 10f; // 초기 brush 사이즈 (중간 사이즈)
    private ArrayList<PathWithBrushSize> paths = new ArrayList<>();
    private ArrayList<RectF> drawnAreas = new ArrayList<>(); // 좌표 저장

    public DrawingImageView(Context context) {
        super(context);
        init();
    }

    public void setRectangleMode(boolean enabled) {
        isRectangleMode = enabled;
    }

    public DrawingImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void toggleEraser() {
        isEraserActive = !isEraserActive;
    }

    public DrawingImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setAntiAlias(true);
        paint.setStrokeWidth(brushSize);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (PathWithBrushSize pathWithBrushSize : paths) {
            paint.setStrokeWidth(pathWithBrushSize.brushSize);
            canvas.drawPath(pathWithBrushSize.path, paint);
        }
        paint.setStrokeWidth(brushSize);
        canvas.drawPath(path, paint);

        if (rectToDraw != null) {
            Paint rectPaint = new Paint();
            rectPaint.setColor(Color.RED);  // 색상 설정
            rectPaint.setStyle(Paint.Style.STROKE);  // 외곽선만
            rectPaint.setStrokeWidth(5);  // 선 굵기
            canvas.drawRect(rectToDraw, rectPaint);
        }
    }

    public void setRectToDraw(Rect rect) {
        this.rectToDraw = rect;
        invalidate();  // 뷰 다시 그리기
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isRectangleMode) {
            // 사각형 모드일 때의 터치 이벤트 처리
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startX = event.getX();
                    startY = event.getY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    endX = event.getX();
                    endY = event.getY();
                    rectToDraw = new Rect((int) startX, (int) startY, (int) endX, (int) endY);
                    invalidate();
                    break;
                case MotionEvent.ACTION_UP:
                    // 사각형을 확정하거나 다른 작업을 수행
                    break;
            }
            return true;
        } else if (isEraserActive) {
            float x = event.getX();
            float y = event.getY();

            float imageViewWidth = getWidth();
            float imageViewHeight = getHeight();
            float drawableWidth = getDrawable().getIntrinsicWidth();
            float drawableHeight = getDrawable().getIntrinsicHeight();
            float left = (imageViewWidth - drawableWidth * getScaleX()) / 2;
            float right = left + drawableWidth * getScaleX();
            float top = (imageViewHeight - drawableHeight * getScaleY()) / 2;
            float bottom = top + drawableHeight * getScaleY();

            if (x > left && x < right && y > top && y < bottom) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        path.moveTo(x, y);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        path.lineTo(x, y);
                        break;
                    case MotionEvent.ACTION_UP:
                        paths.add(new PathWithBrushSize(new Path(path), brushSize));
                        path.reset();
                        break;
                }
                invalidate();
                return true;
            }
            return false;
        }
        return false;
    }

    public Bitmap getMaskBitmap(int width, int height) {
        Bitmap maskBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(maskBitmap);
        canvas.drawColor(Color.BLACK);

        Paint maskPaint = new Paint();
        maskPaint.setColor(Color.WHITE);
        maskPaint.setStyle(Paint.Style.STROKE);
        maskPaint.setStrokeWidth(brushSize);
        maskPaint.setStrokeCap(Paint.Cap.ROUND);
        maskPaint.setStrokeJoin(Paint.Join.ROUND);
        maskPaint.setAntiAlias(true);

        for (PathWithBrushSize pathWithBrushSize : paths) {
            maskPaint.setStrokeWidth(pathWithBrushSize.brushSize);
            canvas.drawPath(pathWithBrushSize.path, maskPaint);
        }
        maskPaint.setStrokeWidth(brushSize);
        canvas.drawPath(path, maskPaint);

        return maskBitmap;
    }

    public void setBrushSize(float size) {
        brushSize = size;
    }

    public ArrayList<RectF> getDrawnAreas() {
        return drawnAreas;
    }

    public class PathWithBrushSize {
        public Path path;
        public float brushSize;

        public PathWithBrushSize(Path path, float brushSize) {
            this.path = path;
            this.brushSize = brushSize;
        }
    }

    public void resetDrawing() {
        paths.clear();
        path.reset();
        drawnAreas.clear();
        invalidate();
    }
}
