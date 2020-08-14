package com.yuriysurzhikov.trackensuretest.view.fragments;

import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.ArraySet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.yuriysurzhikov.trackensuretest.R;
import com.yuriysurzhikov.trackensuretest.model.MainRepository;
import com.yuriysurzhikov.trackensuretest.model.MainRepositoryContract;
import com.yuriysurzhikov.trackensuretest.model.entities.Refueling;
import com.yuriysurzhikov.trackensuretest.model.roomRepository.RoomDataProvider;
import com.yuriysurzhikov.trackensuretest.presenter.StationsFragmentPresenter;
import com.yuriysurzhikov.trackensuretest.presenter.contracts.StationsFragmentContract;
import com.yuriysurzhikov.trackensuretest.view.adapters.RefuelingRecyclerAdapter;
import com.yuriysurzhikov.trackensuretest.view.dialogs.ChoiceDialog;


import java.time.chrono.HijrahChronology;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class StationsFragment
        extends ProjectFragment
        implements RefuelingRecyclerAdapter.OnStationLongClickListener, StationsFragmentContract.View {

    private static final String TAG = "StationsFragment";

    private static final String ARG_TAB = "ARG_TAB";
    private static final String HINT_TAB = "HINT_TAB";
    private Context context;
    private View view;
    private List<Refueling> gasStations;
    private MainRepositoryContract repository;

    private StationsFragmentContract.Presenter presenter;
    private RecyclerView recyclerView;
    private RefuelingRecyclerAdapter recyclerAdapter;

    private StationsFragment(Context context) {
        this.context = context;
        repository = RoomDataProvider.getInstance(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            gasStations = new ArrayList<>();
        }
        presenter = StationsFragmentPresenter.getInstance(getActivity(), this);
    }


    public static StationsFragment getInstance(Context context) {
        Bundle bundle = new Bundle();
        bundle.putCharSequence(ARG_TAB, "Gas stations fragment");
        StationsFragment fragment = new StationsFragment(context);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.stations_layout_fragment, container, false);
        repository.getAllRefuelings().observe(getViewLifecycleOwner(), refuelings -> {
            gasStations.clear();
            gasStations.addAll(refuelings);
            Log.d(TAG, "onCreateView: " + refuelings.size());
            recyclerAdapter = RefuelingRecyclerAdapter.getInstance(context, this);
            recyclerAdapter.setRefuelingList(new ArrayList<>(gasStations));
            recyclerView = view.findViewById(R.id.stations_recycler);
            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
            recyclerView.setLayoutManager(layoutManager);
            recyclerView.setAdapter(recyclerAdapter);
        });
        Log.d(TAG, "onCreateView: " + gasStations);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextView view1 = view.findViewById(R.id.text);
        if (savedInstanceState != null)
            view1.setText(savedInstanceState.getCharSequence(ARG_TAB));
        View floatingButton = view.findViewById(R.id.add_station_button);
        floatingButton.setOnClickListener(v -> presenter.openAddingActivity(context));
    }

    @Override
    public void onLongClickListener(int position) {
        ChoiceDialog dialog = new ChoiceDialog(getContext(), new ChoiceDialog.DialogListener() {
            @Override
            public void onDeleteClickListener(View view) {
                MainRepository.getInstance().deleteRefuelingNote(gasStations.get(position));
            }

            @Override
            public void onEditClickListener(View view) {

            }
        });
        dialog.show(getChildFragmentManager(), HINT_TAB);
    }
}
