package com.movie.app.ui;

import static com.movie.app.network.APIClient.API_KEY;

import android.content.Context;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.material.snackbar.Snackbar;
import com.movie.app.R;
import com.movie.app.network.APIClient;
import com.movie.app.network.APIInterface;
import com.movie.app.network.Movie;
import com.movie.app.util.Utils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ViewActivity extends AppCompatActivity {

    private ConstraintLayout clContent;
    private TextView tvTitle;
    private TextView tvOverview;
    private ImageView ivImage;

    private APIInterface apiInterface;

    private AdView mAdViewBottom;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view);
        clContent = findViewById(R.id.clContent);
        tvTitle = findViewById(R.id.tvTitle);
        tvOverview = findViewById(R.id.tvOverview);
        ivImage = findViewById(R.id.ivImage);

        initAdmob();
        admobBanner();

        apiInterface = APIClient.getClient().create(APIInterface.class);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            int value = extras.getInt("movie_id", 0);
            getMovie(this, value);
        } else {
            Snackbar.make(clContent, "Something not right. Please Try Again later.", Snackbar.LENGTH_LONG).show();
        }
    }

    private void initAdmob(){
        MobileAds.initialize(this);
    }

    private void admobBanner(){
        mAdViewBottom = findViewById(R.id.adViewBottom);
        mAdViewBottom.loadAd(new AdRequest.Builder().build());
    }

    private void getMovie(Context context, int value) {
        if (value != 0 && Utils.getConnectionType(this)) {
            Call<Movie> call = apiInterface.getMovie(value, API_KEY);
            call.enqueue(new Callback<Movie>() {
                @Override
                public void onResponse(@NonNull Call<Movie> call, @NonNull Response<Movie> response) {
                    Movie movie = response.body();
                    if (null != movie) {
                        tvTitle.setText(movie.getTitle());
                        tvOverview.setText(movie.getOverview());
                        Glide.with(context).load(Utils.image_url + movie.getPoster_path()).placeholder(R.drawable.ic_no_exist).dontAnimate().into(ivImage);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Movie> call, @NonNull Throwable t) {
                    call.cancel();
                    Snackbar.make(clContent, "Error. Please Try Again later.", Snackbar.LENGTH_LONG).show();
                }
            });
        } else {
            Snackbar.make(clContent, "No Internet Connection. Please Try Again", Snackbar.LENGTH_LONG).show();
        }
    }
}
