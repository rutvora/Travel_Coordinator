package com.travelbphc;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.SetOptions;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by rutvora (www.github.com/rutvora)
 */

public class LocalTravel extends Fragment implements View.OnClickListener {
    Place from, to;
    TextView date, time;

    public LocalTravel() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.local_travel, container, false);
        try {
            //FROM
            assert getActivity() != null;
            PlaceAutocompleteFragment fromField = (PlaceAutocompleteFragment) getActivity().getFragmentManager().findFragmentById(R.id.fromField);
            fromField.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(Place place) {
                    from = place;
                }

                @Override
                public void onError(Status status) {
                    Toast.makeText(getContext(), status.toString(), Toast.LENGTH_SHORT).show();
                }
            });

            //TO
            PlaceAutocompleteFragment toField = (PlaceAutocompleteFragment) getActivity().getFragmentManager().findFragmentById(R.id.toField);
            toField.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(Place place) {
                    to = place;
                }

                @Override
                public void onError(Status status) {
                    Toast.makeText(getContext(), status.toString(), Toast.LENGTH_SHORT).show();
                }
            });

            date = rootView.findViewById(R.id.datePicker);
            date.setOnClickListener(this);

            time = rootView.findViewById(R.id.timePicker);
            time.setOnClickListener(this);

            rootView.findViewById(R.id.addToList).setOnClickListener(this);
            rootView.findViewById(R.id.getList).setOnClickListener(this);
            rootView.findViewById(R.id.subscribe).setOnClickListener(this);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return rootView;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.datePicker) {
            int mYear, mMonth, mDay;
            final Calendar c = Calendar.getInstance();
            //Get current date
            mYear = c.get(Calendar.YEAR);
            mMonth = c.get(Calendar.MONTH);
            mDay = c.get(Calendar.DAY_OF_MONTH);

            assert getContext() != null;
            DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                    new DatePickerDialog.OnDateSetListener() {

                        @Override
                        public void onDateSet(DatePicker view, int year,
                                              int monthOfYear, int dayOfMonth) {
                            String printDate = dayOfMonth + "-" + (monthOfYear + 1) + "-" + year;
                            date.setText(printDate);

                        }
                    }, mYear, mMonth, mDay);
            datePickerDialog.show();
        } else if (v.getId() == R.id.timePicker) {
            int mHour, mMinute;
            // Get Current Time
            final Calendar c = Calendar.getInstance();
            mHour = c.get(Calendar.HOUR_OF_DAY);
            mMinute = c.get(Calendar.MINUTE);

            // Launch Time Picker Dialog
            assert getContext() != null;
            TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(),
                    new TimePickerDialog.OnTimeSetListener() {

                        @Override
                        public void onTimeSet(TimePicker view, int hourOfDay,
                                              int minute) {
                            String printTime = hourOfDay + ":" + minute;
                            time.setText(printTime);
                        }
                    }, mHour, mMinute, false);
            timePickerDialog.show();
        } else if (v.getId() == R.id.addToList) {
            addToList();
        } else if (v.getId() == R.id.getList) {
            getList();
        } else if (v.getId() == R.id.subscribe) {
            subscribeToList();
        }
    }

    private void getList() {
        assert getActivity() != null;
        double fromLat, fromLong, toLat, toLong;
        LatLng from = this.from.getLatLng();
        LatLng to = this.to.getLatLng();
        fromLat = Math.round((from.latitude) * 100.0) / 100.0;
        fromLong = Math.round((from.longitude) * 100.0) / 100.0;
        toLat = Math.round((to.latitude) * 100.0) / 100.0;
        toLong = Math.round((to.longitude) * 100.0) / 100.0;
        String document = "(" + fromLat + "," + fromLong + ")-(" + toLat + "," + toLong + ")";

        String date = this.date.getText().toString();

        EditText condition = getActivity().findViewById(R.id.condition);

        String exactDateTime = date + "'T'" + time.getText().toString() + "'Z'";

        List list = new List();
        Bundle bundle = new Bundle();
        bundle.putString(getString(R.string.localOrOutstation), "Local");
        bundle.putString(getString(R.string.fromTo), document);
        bundle.putString(getString(R.string.dateOfJourney), date);
        bundle.putString(getString(R.string.queryCondition), condition.getText().toString());        //TODO
        bundle.putString(getString(R.string.exactDateTime), exactDateTime);
        list.setArguments(bundle);
        getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.fragment, list).commit();
    }

    private void addToList() {
        assert getActivity() != null;
        getActivity().findViewById(R.id.loadingIcon).setVisibility(View.VISIBLE);
        //Add to or create the document with the same from and to locations.
        double fromLat, fromLong, toLat, toLong;
        LatLng from = this.from.getLatLng();
        LatLng to = this.to.getLatLng();
        fromLat = Math.round((from.latitude) * 100.0) / 100.0;
        fromLong = Math.round((from.longitude) * 100.0) / 100.0;
        toLat = Math.round((to.latitude) * 100.0) / 100.0;
        toLong = Math.round((to.longitude) * 100.0) / 100.0;
        String fromTo = "(" + fromLat + "," + fromLong + ")-(" + toLat + "," + toLong + ")";

        //Add to or create a collection with the date of travel
        String date = this.date.getText().toString();

        //Add to or Create a document with the time of travel
        String time = this.time.getText().toString();

        //Get Exact Date time to get while querying.
        String exactDateTime = date + "'T'" + time + "'Z'";

        //Collect info and store in the database for easier access
        Map<String, Object> info = new HashMap<>();
        info.put(getString(R.string.name), MainActivity.user.getDisplayName());
        info.put(getString(R.string.email), MainActivity.user.getEmail());
        EditText phoneNumber = getActivity().findViewById(R.id.phoneNumber);
        info.put(getString(R.string.phone), phoneNumber.getText().toString());      //TODO: find alternative
        info.put(getString(R.string.photoUri), MainActivity.user.getPhotoUrl());

        info.put(getString(R.string.isGroup), false);

        info.put("fromAddress", this.from.getAddress());
        info.put("toAddress", this.to.getAddress());
        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy'T'HH:mm'Z'", Locale.US);
        try {
            Date dateToAdd = format.parse(exactDateTime);
            Timestamp timestamp = new Timestamp(dateToAdd);
            info.put(getString(R.string.time), timestamp);
        } catch (ParseException e) {
            e.printStackTrace();
        }


        MainActivity.db
                .get()
                .collection("Local")
                .document(fromTo)
                .collection(date)
                .document(MainActivity.user.getUid())
                .set(info, SetOptions.merge())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        assert getActivity() != null;
                        getActivity().findViewById(R.id.loadingIcon).setVisibility(View.GONE);
                        Toast.makeText(getActivity(), R.string.addSuccess, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        getActivity().findViewById(R.id.loadingIcon).setVisibility(View.GONE);
                        Toast.makeText(getActivity(), R.string.error, Toast.LENGTH_SHORT).show();
                        e.printStackTrace();

                    }
                });

        //Add to user's travel history
        Map<String, Object> travelDetails = new HashMap<>();
        travelDetails.put(getString(R.string.fromTo), fromTo);
        travelDetails.put(getString(R.string.dateOfJourney), date);
        travelDetails.put(getString(R.string.groupId), null);

        Map<String, Object> historyData = new HashMap<>(1);
        historyData.put(exactDateTime, travelDetails);

        MainActivity.db
                .get()
                .collection("Users")
                .document(MainActivity.user.getUid())
                .set(historyData, SetOptions.merge())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getActivity(), R.string.error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void subscribeToList() {
        //TODO? (Alternative implemented in MainActivity)
        Toast.makeText(getActivity(), R.string.toBeImplemented, Toast.LENGTH_SHORT).show();
    }
}
