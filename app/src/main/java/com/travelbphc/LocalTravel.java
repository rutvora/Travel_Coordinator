package com.travelbphc;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
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
import com.google.firebase.firestore.SetOptions;

import java.util.Calendar;
import java.util.HashMap;
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

            rootView.findViewById(R.id.submit).setOnClickListener(this);
        } catch (NullPointerException e) {
            Toast.makeText(getContext(), "Fragment Manager not found", Toast.LENGTH_SHORT).show();
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
        } else if (v.getId() == R.id.submit) {
            Log.w("Here", "1");
            double fromLat, fromLong, toLat, toLong;
            LatLng from = this.from.getLatLng();
            LatLng to = this.to.getLatLng();
            fromLat = Math.round((from.latitude) * 100.0) / 100.0;
            fromLong = Math.round((from.longitude) * 100.0) / 100.0;
            toLat = Math.round((to.latitude) * 100.0) / 100.0;
            toLong = Math.round((to.longitude) * 100.0) / 100.0;

            String collection = "(" + fromLat + "," + fromLong + ")-(" + toLat + "," + toLong + ")";
            String time = this.time.getText().toString();
            String[] splitTime = time.split(":");

            Map<String, String> userInfo = new HashMap<>(3);
            userInfo.put("Name", "Name1");
            userInfo.put("Email", "Email1");
            userInfo.put("Phone", "Phone1");

            Map<String, Map<String, String>> toAdd = new HashMap<>(1);
            toAdd.put("userID3", userInfo);
            Log.w("Here", "2");
            MainActivity.weakDBReference.get().collection("BITS Hyderabad").document("Local")
                    .collection(collection).document(splitTime[0]).set(toAdd, SetOptions.merge()).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.w("Success", "True");
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.w("Failed", "Error");
                    e.printStackTrace();
                }
            });
            Log.w("Here", "3");
        } else {
            Log.w("Unknown id", v.getId() + "");
        }
    }
}
