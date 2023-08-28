package org.pytorch.demo.objectdetection;

import android.graphics.Bitmap;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import kotlin.Triple;

public class CardAdapter extends RecyclerView.Adapter<CardAdapter.CardViewHolder> {

    private List<ImageTextData> dataList;


    public CardAdapter(List<ImageTextData> dataList) {
        this.dataList = dataList;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_card, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        ImageTextData data = dataList.get(position);
        holder.cardImageView.setImageBitmap(data.getImage());

        holder.headText.setText(data.getHeadText());
        holder.upText.setText(data.getUpText());
        holder.lowText.setText(data.getLowText());
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    class CardViewHolder extends RecyclerView.ViewHolder {
        ImageView cardImageView;
        TextView headText;
        TextView upText;
        TextView lowText;

        public CardViewHolder(@NonNull View itemView) {
            super(itemView);
            cardImageView = itemView.findViewById(R.id.cardImageView);
            headText = itemView.findViewById(R.id.headText);
            upText = itemView.findViewById(R.id.upText);
            lowText = itemView.findViewById(R.id.lowText);
        }
    }

}


