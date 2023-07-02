package com.movie.tmdb.app.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.movie.tmdb.app.R;
import com.movie.tmdb.app.model.Movie;
import com.movie.tmdb.app.util.Utils;

import java.util.List;

public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.ViewHolder> {

    private List<Movie> movieList;
    private final Activity activity;

    private AdapterOnClickListener onClickListener;

    public MovieAdapter(Activity _activity, List<Movie> _movieList) {
        activity = _activity;
        movieList = _movieList;
    }

    public void setOnClickListener(AdapterOnClickListener _onClickListener) {
        this.onClickListener = _onClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_movie, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Movie movie = movieList.get(position);
        Glide.with(activity).load(Utils.image_url + movie.getPoster_path()).into(holder.ivMovie);
        holder.tvMovieName.setText(movie.getOriginal_title());
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return movieList.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateList(List<Movie> _movieList) {
        movieList = _movieList;
        notifyDataSetChanged();
    }

    public interface AdapterOnClickListener {
        void onClickListener(int _position, List<Movie> movieList);
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView ivMovie;
        TextView tvMovieName;

        ViewHolder(View itemView) {
            super(itemView);
            ivMovie = itemView.findViewById(R.id.ivMovie);
            tvMovieName = itemView.findViewById(R.id.tvMovieName);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            onClickListener.onClickListener(getAdapterPosition(), movieList);
        }
    }
}
