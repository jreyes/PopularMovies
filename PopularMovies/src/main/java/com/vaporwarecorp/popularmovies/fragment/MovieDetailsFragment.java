package com.vaporwarecorp.popularmovies.fragment;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import com.google.android.youtube.player.YouTubeIntents;
import com.google.android.youtube.player.YouTubeStandalonePlayer;
import com.vaporwarecorp.popularmovies.BuildConfig;
import com.vaporwarecorp.popularmovies.PopularMoviesApp;
import com.vaporwarecorp.popularmovies.R;
import com.vaporwarecorp.popularmovies.adapter.ReviewAdapter;
import com.vaporwarecorp.popularmovies.adapter.VideoAdapter;
import com.vaporwarecorp.popularmovies.databinding.FragmentMovieDetailsBinding;
import com.vaporwarecorp.popularmovies.events.FavoriteRemovedEvent;
import com.vaporwarecorp.popularmovies.model.Movie;
import com.vaporwarecorp.popularmovies.model.MovieDetail;
import com.vaporwarecorp.popularmovies.model.Review;
import com.vaporwarecorp.popularmovies.model.Video;
import com.vaporwarecorp.popularmovies.service.MovieApi;
import com.vaporwarecorp.popularmovies.service.MovieDB;
import de.greenrobot.event.EventBus;

import java.util.ArrayList;

import static com.vaporwarecorp.popularmovies.PopularMoviesApp.getMovieApi;
import static com.vaporwarecorp.popularmovies.util.ParcelUtil.*;

public class MovieDetailsFragment extends BaseFragment {
// ------------------------------ FIELDS ------------------------------

    public static final String YOUTUBE_PATH = "http://www.youtube.com/watch?v=";
    public static final int YOUTUBE_PLAY_INTENT = 1;

    Callback<Movie> mAddFavoriteCallback = new Callback<Movie>() {
        @Override
        public void failure() {
        }

        @Override
        public void success(Movie movie) {
            mBinding.setFavorite(true);
        }
    };
    Callback<MovieDetail> mMovieDetailCallback = new Callback<MovieDetail>() {
        @Override
        public void failure() {
        }

        @Override
        public void success(MovieDetail movieDetail) {
            updateMovieDetail(movieDetail.reviews, movieDetail.videos);
        }
    };
    Callback<Movie> mRemoveFavoriteCallback = new Callback<Movie>() {
        @Override
        public void failure() {
        }

        @Override
        public void success(Movie movie) {
            mBinding.setFavorite(false);
            EventBus.getDefault().post(new FavoriteRemovedEvent(movie));
        }
    };
    AdapterView.OnItemClickListener mOnItemClickListener = (parent, view, position, id) -> playVideo((Video) parent.getItemAtPosition(position));

    private FragmentMovieDetailsBinding mBinding;
    private MovieDB mMovieDB;

// -------------------------- STATIC METHODS --------------------------

    public static void start(AppCompatActivity activity, Movie movie) {
        Bundle arguments = new Bundle();
        setMovie(arguments, movie);

        MovieDetailsFragment fragment = new MovieDetailsFragment();
        fragment.setArguments(arguments);

        activity
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.movie_details_container, fragment)
                .commit();
    }

    public static void stop(AppCompatActivity activity) {
        Fragment fragment = activity
                .getSupportFragmentManager()
                .findFragmentById(R.id.movie_details_container);
        if (fragment != null) {
            activity.getSupportFragmentManager().beginTransaction().remove(fragment).commit();
        }
    }

// -------------------------- OTHER METHODS --------------------------

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_movie_details, container, false);
        initBindings(view, savedInstanceState);
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        setMovie(outState, mBinding.getMovie());
        setVideos(outState, mBinding.getVideos());
        setReviews(outState, mBinding.getReviews());
    }

    private void addFavorite() {
        subscribe(
                mMovieDB.addMovie(mBinding.getMovie(), mBinding.getReviews(), mBinding.getVideos()),
                mAddFavoriteCallback
        );
    }

    private void initBindings(View view, Bundle savedInstanceState) {
        // fix for Glide loading when the activity is being destroyed
        if (getActivity().isDestroyed()) {
            return;
        }

        mMovieDB = PopularMoviesApp.getMovieDb(getActivity());

        mBinding = DataBindingUtil.bind(view);
        mBinding.addFavorite.setOnClickListener(v -> addFavorite());
        mBinding.removeFavorite.setOnClickListener(v -> removeFavorite());
        if (savedInstanceState == null) {
            mBinding.setMovie(getMovie(getArguments()));
            MovieApi movieApi = getMovieApi(getActivity());
            subscribe(movieApi.getMovieDetail(mBinding.getMovie().id), mMovieDetailCallback);
        } else {
            mBinding.setMovie(getMovie(savedInstanceState));
            updateMovieDetail(getReviews(savedInstanceState), getVideos(savedInstanceState));
        }
    }

    private void playVideo(Video video) {
        Intent intent;
        if (YouTubeIntents.canResolvePlayVideoIntent(getActivity())) {
            intent = YouTubeStandalonePlayer.createVideoIntent(
                    getActivity(),
                    BuildConfig.YOUTUBE_API_KEY,
                    video.key,
                    0,
                    true,
                    false
            );
        } else {
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse(YOUTUBE_PATH + video.key));
        }
        startActivityForResult(intent, YOUTUBE_PLAY_INTENT);
    }

    private void removeFavorite() {
        subscribe(mMovieDB.removeMovie(mBinding.getMovie()), mRemoveFavoriteCallback);
    }

    private void updateMovieDetail(ArrayList<Review> reviews, ArrayList<Video> videos) {
        mBinding.setFavorite(mMovieDB.movieExists(mBinding.getMovie().id));
        mBinding.setReviews(reviews);
        mBinding.setVideos(videos);
        if (!videos.isEmpty()) {
            mBinding.videosView.setOnItemClickListener(mOnItemClickListener);
        }
        mBinding.videosView.setAdapter(new VideoAdapter(videos));
        mBinding.reviewsView.setAdapter(new ReviewAdapter(reviews));
    }
}
