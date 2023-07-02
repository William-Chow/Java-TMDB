package com.movie.app.ui;

import static com.movie.app.network.APIClient.API_KEY;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.movie.app.R;
import com.movie.app.adapter.MovieAdapter;
import com.movie.app.network.APIClient;
import com.movie.app.network.APIInterface;
import com.movie.app.network.Movie;
import com.movie.app.network.Result;
import com.movie.app.util.Utils;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    LinearLayoutCompat llContent;
    private Long backPressedTime = 0L;
    private APIInterface apiInterface;
    private RecyclerView rvMovie;
    private MovieAdapter movieAdapter;
    private final List<Movie> popularList = new ArrayList<>();
    private final List<Movie> originalList = new ArrayList<>();
    private final List<Movie> searchList = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        llContent = findViewById(R.id.llContent);
        rvMovie = findViewById(R.id.rvMovie);
        apiInterface = APIClient.getClient().create(APIInterface.class);

        // Check Internet Connection
        if (Utils.getConnectionType(this)) {
            getPopularMovieList();
        } else {
            Snackbar.make(llContent, "No Internet Connection. Please Try Again", Snackbar.LENGTH_LONG).show();
        }
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
                Snackbar.make(llContent, "Error. Please Try Again later.", Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void setupAdapter(List<Movie> popularList) {
        if (popularList.size() > 0) {
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
            Snackbar.make(llContent, "No result found. Please Try Again later.", Snackbar.LENGTH_LONG).show();
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
        if (backPressedTime + 3000 > System.currentTimeMillis()) {
            finish();
        } else {
            Toast.makeText(this, "Press back again to leave the app.", Toast.LENGTH_LONG).show();
        }
        backPressedTime = System.currentTimeMillis();
    }
}
