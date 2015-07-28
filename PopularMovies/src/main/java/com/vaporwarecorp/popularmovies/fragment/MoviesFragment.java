package com.vaporwarecorp.popularmovies.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import com.vaporwarecorp.popularmovies.PopularMoviesApp;
import com.vaporwarecorp.popularmovies.R;
import com.vaporwarecorp.popularmovies.adapter.MovieAdapter;
import com.vaporwarecorp.popularmovies.events.MovieSelectedEvent;
import com.vaporwarecorp.popularmovies.events.MovieTypeSelectedEvent;
import com.vaporwarecorp.popularmovies.model.Movie;
import com.vaporwarecorp.popularmovies.model.MoviePager;
import com.vaporwarecorp.popularmovies.service.MovieApi;
import com.vaporwarecorp.popularmovies.widget.EndlessGridView;
import de.greenrobot.event.EventBus;

import static com.vaporwarecorp.popularmovies.util.ParcelUtil.*;

public class MoviesFragment extends BaseFragment
        implements EndlessGridView.OnMoreListener, AdapterView.OnItemClickListener {
// ------------------------------ FIELDS ------------------------------

    Callback<MoviePager> mCallback = new Callback<MoviePager>() {
        @Override
        public void failure() {
            mRecyclerView.stopLoadingMore();
        }

        @Override
        public void success(MoviePager moviePager) {
            mPage = moviePager.page;
            mTotalPages = moviePager.totalPages;
            mMovieAdapter.addItems(moviePager.results);
        }
    };

    private int mAction;
    private MovieAdapter mMovieAdapter;
    private MovieApi mMovieApi;
    private int mPage;
    private EndlessGridView mRecyclerView;
    private int mTotalPages;

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface OnItemClickListener ---------------------

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        EventBus.getDefault().post(new MovieSelectedEvent((Movie) parent.getItemAtPosition(position)));
    }

// --------------------- Interface OnMoreListener ---------------------

    @Override
    public boolean canLoadMore() {
        return mPage < mTotalPages;
    }

    @Override
    public void onMore() {
        switch (mAction) {
            case 0:
                subscribe(mMovieApi.getTopRated(mPage + 1), mCallback);
                break;
            case 1:
                subscribe(mMovieApi.getPopular(mPage + 1), mCallback);
                break;
            default:
                break;
        }
    }

// -------------------------- OTHER METHODS --------------------------

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_movies, container, false);
        initRecyclerView(rootView);
        initApi(savedInstanceState);
        return rootView;
    }

    @Override
    public void onDestroyView() {
        mRecyclerView.release();
        super.onDestroyView();
    }

    @SuppressWarnings("unused")
    public void onEvent(MovieTypeSelectedEvent event) {
        if (event.type == mAction) {
            return;
        }

        mAction = event.type;
        mPage = 0;
        mTotalPages = 1;
        mMovieAdapter.clearItems();
        onMore();
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        setAction(outState, mAction);
        setMovies(outState, mMovieAdapter.getItems());
        setPage(outState, mPage);
        setTotalPages(outState, mTotalPages);
    }

    private void initApi(Bundle savedInstanceState) {
        mMovieApi = PopularMoviesApp.getMovieApi(getActivity());
        if (savedInstanceState == null) {
            mAction = -1;
        } else {
            mAction = getAction(savedInstanceState);
            mPage = getPage(savedInstanceState);
            mTotalPages = getTotalPages(savedInstanceState);
            mMovieAdapter.addItems(getMovies(savedInstanceState));
        }
    }

    private void initRecyclerView(View view) {
        mMovieAdapter = new MovieAdapter();

        mRecyclerView = (EndlessGridView) view.findViewById(R.id.movie_view);
        mRecyclerView.setAdapter(mMovieAdapter);
        mRecyclerView.setOnItemClickListener(this);
        mRecyclerView.setOnMoreListener(this);
    }
}
