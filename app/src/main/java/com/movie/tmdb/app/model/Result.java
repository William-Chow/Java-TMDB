package com.movie.tmdb.app.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.annotations.SerializedName;
import com.movie.tmdb.app.model.Movie;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Result {
    @SerializedName("results")
    List<Movie> results;

    public List<Movie> getResults() {
        return results;
    }

    public void setResults(List<Movie> results) {
        this.results = results;
    }
}
