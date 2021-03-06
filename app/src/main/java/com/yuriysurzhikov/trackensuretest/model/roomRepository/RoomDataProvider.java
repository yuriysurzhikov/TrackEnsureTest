package com.yuriysurzhikov.trackensuretest.model.roomRepository;

import android.database.sqlite.SQLiteConstraintException;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import com.yuriysurzhikov.trackensuretest.config.App;
import com.yuriysurzhikov.trackensuretest.model.MainRepositoryContract;
import com.yuriysurzhikov.trackensuretest.model.entities.Place;
import com.yuriysurzhikov.trackensuretest.model.entities.Refueling;
import com.yuriysurzhikov.trackensuretest.model.entities.StatisticsLive;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class RoomDataProvider implements MainRepositoryContract {

    private static final String TAG = "RoomDataProvider";

    private LocalDatabase database;
    private LiveData<List<Refueling>> gasStations;
    private LiveData<List<Place>> places;

    private RoomDataProvider() {
        database = App.getInstance().getDatabase();
        try {
            gasStations = new GetRefuelingLiveData(database).execute().get();
            places = new GetPlacesLiveData(database).execute().get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

    }

    public static RoomDataProvider getInstance() {
        return new RoomDataProvider();
    }

    @NotNull
    @Override
    public LiveData<List<Refueling>> getAllRefuelingRecords() {
        return gasStations;
    }

    @Override
    public void addRefuelingNote(@NotNull Place place, Refueling station) {
        new InsertPlaceAsyncTask(database).execute(place);
        new InsertRefuelingAsyncTask(database).execute(station);
    }

    @Override
    public void deleteRefuelingNote(@NotNull Refueling station) {
        new DeleteRefuelingAsyncTask(database).execute(station);
        try {
            if (!new ConfirmPresenceChildren(database).execute(station.getAddressCreator()).get()) {
                Log.d(TAG, "deleteRefuelingNote: place have children");
                new DeletePlaceTask(database).execute(station.getAddressCreator());
            }
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateRefuelingNote(@NotNull Refueling stationOld, Refueling stationNew, Place place) {
        deleteRefuelingNote(stationOld);
        Log.d(TAG, "updateRefuelingNote: " + stationOld.getAddressCreator());
        if(getPlaceByAddress(stationNew.getAddressCreator()) == null)
            new InsertPlaceAsyncTask(database).execute(place);
        new UpdateAsyncTask(database).execute(stationNew);
    }

    public LiveData<List<Place>> getAllPlaces() {
        return places;
    }

    public Place getPlaceByAddress(String address) {
        try {
            return new GetPlaceByAddressTask(database).execute(address).get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void addPlace(Place place) {
        new InsertPlaceAsyncTask(database).execute(place);
    }

    @NotNull
    @Override
    public LiveData<List<StatisticsLive>> highlightStatistics() {
        return Transformations.map(places, input -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return input.parallelStream()
                        .map(place -> new StatisticsLive(place.getProvider(), place.getAddress(), database.refuelingDao().getStatisticsElementByAddress(place.getAddress())))
                        .collect(Collectors.toList());
            }
            else {
                List<StatisticsLive> returned = new ArrayList<>();
                for (int i = 0; i < input.size(); i++) {
                    returned.add(
                            new StatisticsLive(
                                    input.get(i).getProvider(),
                                    input.get(i).getAddress(),
                                    database.refuelingDao().getStatisticsElementByAddress(input.get(i).getAddress()
                                    )
                            )
                    );
                }
                return returned;
            }
        });
    }

    private static class GetPlacesLiveData extends AsyncTask<Void, Void, LiveData<List<Place>>> {
        private LocalDatabase db;

        public GetPlacesLiveData(LocalDatabase db) {
            this.db = db;
        }

        @Override
        protected LiveData<List<Place>> doInBackground(Void... voids) {
            return db.placeDao().getAll();
        }
    }

    private static class GetRefuelingLiveData extends AsyncTask<Void, Void, LiveData<List<Refueling>>> {

        private LocalDatabase db;

        public GetRefuelingLiveData(LocalDatabase db) {
            this.db = db;
        }

        @Override
        protected LiveData<List<Refueling>> doInBackground(Void... voids) {
            return db.refuelingDao().getAll();
        }
    }

    private static class GetPlaceByAddressTask extends AsyncTask<String, Void, Place> {

        private LocalDatabase db;

        public GetPlaceByAddressTask(LocalDatabase db) {
            this.db = db;
        }

        @Override
        protected Place doInBackground(String... strings) {
            return db.placeDao().getPlaceByAddress(strings[0]);
        }
    }

    private static class DeleteRefuelingAsyncTask extends AsyncTask<Refueling, Void, Void> {
        private LocalDatabase db;
        DeleteRefuelingAsyncTask(LocalDatabase db) {
            this.db = db;
        }

        @Override
        protected Void doInBackground(final Refueling... gasStations) {
            db.refuelingDao().deleteRefuelingRecord(gasStations[0]);
            return null;
        }
    }

    private static class InsertRefuelingAsyncTask extends AsyncTask<Refueling, Void, Void> {
        private LocalDatabase db;
        public InsertRefuelingAsyncTask(LocalDatabase db) {
            this.db = db;
        }

        @Override
        protected Void doInBackground(Refueling... refuelings) {
            db.refuelingDao().insertRefuelingRecord(refuelings[0]);
            return null;
        }
    }

    private static class InsertPlaceAsyncTask extends AsyncTask<Place, Void, Boolean> {
        private LocalDatabase db;

        InsertPlaceAsyncTask(LocalDatabase db) {
            this.db = db;
        }

        @Override
        protected Boolean doInBackground(final Place... places) throws SQLiteConstraintException {
            db.placeDao().addPlace(places[0]);
            return true;
        }
    }

    private static class UpdateAsyncTask extends AsyncTask<Refueling, Void, Void> {
        private LocalDatabase db;
        UpdateAsyncTask(LocalDatabase db) {
            this.db = db;
        }

        @Override
        protected Void doInBackground(final Refueling... gasStations) {
            db.refuelingDao().insertRefuelingRecord(gasStations[0]);
            return null;
        }
    }

    private static class ConfirmPresenceChildren extends AsyncTask<String, Void, Boolean> {

        private LocalDatabase db;
        public ConfirmPresenceChildren(LocalDatabase db) {
            this.db = db;
        }
        @Override
        protected Boolean doInBackground(String... places) {
            List<Refueling> list = db.refuelingDao().getRefuelingByAddress(places[0]);
            Log.d(TAG, "doInBackground: " + list.size());
            return list.size() != 0;
        }
    }

    private static class DeletePlaceTask extends AsyncTask<String, Void, Void> {

        private LocalDatabase db;
        public DeletePlaceTask(LocalDatabase db) {
            this.db = db;
        }

        @Override
        protected Void doInBackground(String... places) {
            db.placeDao().deletePlace(places[0]);
            return null;
        }
    }
}
