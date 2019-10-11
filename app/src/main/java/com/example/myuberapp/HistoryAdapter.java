package com.example.myuberapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import androidx.recyclerview.widget.RecyclerView;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.Holder> {
    private Context context;
    private ArrayList<HistoryObject> listHistory;

    public HistoryAdapter(Context context, ArrayList<HistoryObject> listHistory){
        this.context = context;
        this.listHistory = listHistory;
    }
    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.history_item, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
        final HistoryObject historyObject = listHistory.get(position);
        holder.tvCustomerName.setText(historyObject.getCustomerName() != null ? historyObject.getCustomerName():"");
        holder.tvDriverName.setText(historyObject.getDriverName() != null ? historyObject.getDriverName():"");
//        holder.tvComment.setText(historyObject.getComment() != null ? historyObject.getComment() : "");
        holder.tvRating.setText(historyObject.getRating());
        holder.tvDate.setText(historyObject.getDate());

        holder.getView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onItemHistoryClick != null){
                    onItemHistoryClick.onItemClick(historyObject);
                }
            }
        });
    }

    interface OnItemHistoryClick{
        void onItemClick(HistoryObject historyObject);
    }
    private OnItemHistoryClick onItemHistoryClick;
    public void setOnItemHistoryClick(OnItemHistoryClick onItemHistoryClick){
        this.onItemHistoryClick = onItemHistoryClick;
    }

    @Override
    public int getItemCount() {
        return listHistory.size();
    }

    public class Holder extends RecyclerView.ViewHolder {
        private TextView tvDate, tvCustomerName, tvDriverName, tvComment, tvRating;
        private View view;
        public Holder(View itemView) {
            super(itemView);
            view = itemView;
            tvDate = itemView.findViewById(R.id.tv_history_date);
            tvRating = itemView.findViewById(R.id.tv_history_rating);
            tvCustomerName = itemView.findViewById(R.id.tv_history_customer_name);
            tvDriverName = itemView.findViewById(R.id.tv_history_driver_name);
            tvComment = itemView.findViewById(R.id.tv_history_comment);
        }
        public View getView(){
            return view;
        }
    }
}
