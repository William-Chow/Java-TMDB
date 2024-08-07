package com.movie.tmdb.app.ui;

import static com.movie.tmdb.app.network.APIClient.API_KEY;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.material.snackbar.Snackbar;
import com.movie.tmdb.app.R;
import com.movie.tmdb.app.adapter.MovieAdapter;
import com.movie.tmdb.app.model.Movie;
import com.movie.tmdb.app.model.Result;
import com.movie.tmdb.app.network.APIClient;
import com.movie.tmdb.app.network.APIInterface;
import com.movie.tmdb.app.util.Utils;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    RelativeLayout rlContent;
    private Long backPressedTime = 0L;
    private APIInterface apiInterface;
    private RecyclerView rvMovie;
    private MovieAdapter movieAdapter;
    private final List<Movie> popularList = new ArrayList<>();
    private final List<Movie> originalList = new ArrayList<>();
    private final List<Movie> searchList = new ArrayList<>();

    private AdView mAdViewTop;
    private AdView mAdViewBottom;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rlContent = findViewById(R.id.rlContent);
        rvMovie = findViewById(R.id.rvMovie);
        apiInterface = APIClient.getClient().create(APIInterface.class);

        initAdmob();
        admobBanner();

        // Check Internet Connection
        if (Utils.getConnectionType(this)) {
            getPopularMovieList();
        } else {
            Snackbar.make(rlContent, "No Internet Connection. Please Try Again", Snackbar.LENGTH_LONG).show();
        }
    }

    private void initAdmob(){
        MobileAds.initialize(this);
    }

    private void admobBanner(){
        mAdViewTop = findViewById(R.id.adViewTop);
        mAdViewBottom = findViewById(R.id.adViewBottom);
        mAdViewTop.loadAd(new AdRequest.Builder().build());
        mAdViewBottom.loadAd(new AdRequest.Builder().build());
    }

    private void getPopularMovieList() {
        Call<Result> call = apiInterface.getPopularMovies(API_KEY);
        call.enqueue(new Callback<Result>() {
            @Override
            public void onResponse(@NonNull Call<Result> call, @NonNull Response<Result> response) {
                if (response.body() != null) {
                    popularList.addAll(response.body().getResults());
                    originalList.addAll(response.body().getResults());
                    setupAdapter(popularList);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Result> call, @NonNull Throwable t) {
                call.cancel();
                Snackbar.make(rlContent, "Error. Please Try Again later.", Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void setupAdapter(List<Movie> popularList) {
        if (!popularList.isEmpty()) {
            movieAdapter = new MovieAdapter(this, popularList);
            rvMovie.setHasFixedSize(true);
            rvMovie.setLayoutManager(new GridLayoutManager(this, 2));
            movieAdapter.setOnClickListener((_position, movieList) -> {
                Intent intent = new Intent(this, ViewActivity.class);
                intent.putExtra("movie_id", movieList.get(_position).getId());
                this.startActivity(intent);
            });
            rvMovie.setAdapter(movieAdapter);
        } else {
            Snackbar.make(rlContent, "No result found. Please Try Again later.", Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_menu, menu);
        MenuItem item = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) item.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                Log.d("onQueryTextChange", "query: " + query);
                if (query.length() >= 2) {
                    getSearchResult(query);
                } else {
                    movieAdapter.updateList(originalList);
                }
                return true;
            }
        });
        item.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(@NonNull MenuItem item) {
                showToast("Action Expand");
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(@NonNull MenuItem item) {
                showToast("Action Collapse");
                return true;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    private void getSearchResult(String query) {
        Call<Result> call = apiInterface.getSearch(API_KEY, query, 1);
        call.enqueue(new Callback<Result>() {
            @Override
            public void onResponse(@NonNull Call<Result> call, @NonNull Response<Result> response) {
                if (response.body() != null) {
                    searchList.clear();
                    searchList.addAll(response.body().getResults());
                    movieAdapter.updateList(searchList);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Result> call, @NonNull Throwable t) {
                call.cancel();
                Snackbar.make(getParent().findViewById(R.id.llContent), "Error. Please Try Again later.", Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (backPressedTime + 3000 > System.currentTimeMillis()) {
            finish();
        } else {
            Toast.makeText(this, "Press back again to leave the app.", Toast.LENGTH_LONG).show();
        }
        backPressedTime = System.currentTimeMillis();
    }
}
