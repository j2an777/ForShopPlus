package org.pytorch.demo.objectdetection;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import java.util.List;

public class ImageAdapter extends BaseAdapter {

    private Context context;
    private List<String> imagePaths;

    private int selectedIndex = -1; // -1은 아무것도 선택되지 않았음을 나타냅니다.

    public ImageAdapter(Context context, List<String> imagePaths) {
        this.context = context;
        this.imagePaths = imagePaths;
    }

    @Override
    public int getCount() {
        return imagePaths.size();
    }

    @Override
    public Object getItem(int position) {
        return imagePaths.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.grid_item, parent, false);
            holder = new ViewHolder();
            holder.imageView = convertView.findViewById(R.id.grid_image);

            // 여기서 높이를 설정합니다.
            holder.imageView.getLayoutParams().height = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 100, context.getResources().getDisplayMetrics());

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        String imagePath = imagePaths.get(position);
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
        holder.imageView.setImageBitmap(bitmap);

        // 선택된 이미지의 인덱스와 현재 이미지의 인덱스를 확인
        if (position == selectedIndex) {
            convertView.setAlpha(0.5f); // 선택된 이미지는 흐리게
        } else {
            convertView.setAlpha(1.0f); // 선택되지 않은 이미지는 밝게
        }

        return convertView;
    }

    public void setSelectedIndex(int index) {
        selectedIndex = index;
    }

    private Bitmap makeSquareBitmap(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int size = Math.min(width, height);

        int x = (width - size) / 2;
        int y = (height - size) / 2;

        Bitmap squareBitmap = Bitmap.createBitmap(bitmap, x, y, size, size);

        // 정사각형 비트맵의 세로 길이를 조금 더 줄임
        float scaleFactor = 0.5f;
        int newHeight = (int) (squareBitmap.getHeight() * scaleFactor);
        int newY = (squareBitmap.getHeight() - newHeight) / 2;
        squareBitmap = Bitmap.createBitmap(squareBitmap, 0, newY, squareBitmap.getWidth(), newHeight);

        return squareBitmap;
    }



    private static class ViewHolder {
        ImageView imageView;
    }
}
