package com.travelbphc;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by rutvora (www.github.com/rutvora)
 */

public class List extends Fragment implements View.OnClickListener {
    final private Timestamp[] condition = new Timestamp[2];
    private CollectionReference collectionReference;
    private ListView list;
    private Button getUsers;
    private Button getGroups;
    @Nullable
    private String exactDateTime;

    public List() {

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.get_list, container, false);

        Bundle args = getArguments();
        try {
            assert args != null;
            String local = args.getString(getString(R.string.localOrOutstation));
            String fromTo = args.getString(getString(R.string.fromTo));
            String date = args.getString(getString(R.string.dateOfJourney));
            exactDateTime = args.getString(getString(R.string.exactDateTime));

            assert local != null && fromTo != null && date != null;
            Log.w("variables", local + fromTo + date);
            collectionReference = MainActivity.db.get()
                    .collection(local)
                    .document(fromTo)
                    .collection(date);
            String query = args.getString(getString(R.string.queryCondition));
            assert query != null;
            String[] array = query.split("-");
            String[] condition = new String[2];
            condition[0] = args.getString(getString(R.string.dateOfJourney)) + " " + array[0];
            condition[1] = args.getString(getString(R.string.dateOfJourney)) + " " + array[1];
            SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.US);

            this.condition[0] = new Timestamp(format.parse(condition[0]));
            this.condition[1] = new Timestamp(format.parse(condition[1]));
        } catch (Exception e) {
            e.printStackTrace();
        }

        getUsers = rootView.findViewById(R.id.getUsers);
        getUsers.setOnClickListener(this);
        getGroups = rootView.findViewById(R.id.getGroups);
        getGroups.setOnClickListener(this);
        list = rootView.findViewById(R.id.list);
        list.setVisibility(View.GONE);

        return rootView;
    }

    @Override
    public void onClick(@NonNull View v) {
        if (v.getId() == R.id.getUsers) {
            assert getActivity() != null;
            getActivity().findViewById(R.id.loadingIcon).setVisibility(View.VISIBLE);
            collectionReference
                    .whereGreaterThanOrEqualTo(getString(R.string.time), condition[0])
                    .whereLessThanOrEqualTo(getString(R.string.time), condition[1])
                    //.whereEqualTo(getString(R.string.isGroup), false)     //TODO: Find method to auto-create indexes (or find alternative storage method)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                ArrayList<String> coTravellers = new ArrayList<>();
                                for (DocumentSnapshot doc : task.getResult()) {
                                    coTravellers.add(doc.getString(getString(R.string.name)));
                                    assert getContext() != null;
                                    ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, coTravellers);
                                    list.setAdapter(adapter);
                                    list.setVisibility(View.VISIBLE);
                                    getActivity().findViewById(R.id.loadingIcon).setVisibility(View.GONE);
                                    getGroups.setVisibility(View.GONE);
                                    getUsers.setVisibility(View.GONE);
                                }
                            } else {
                                Toast.makeText(getActivity(), R.string.error, Toast.LENGTH_SHORT).show();
                                task.getException().printStackTrace();
                            }
                        }
                    });
        } else if (v.getId() == R.id.getGroups) {
            Toast.makeText(getActivity(), R.string.toBeImplemented, Toast.LENGTH_SHORT).show();
        }
    }

    private void makeGroup(@NonNull Map<String, Object>[] memberDetails) {       //TODO: Make a sample map, and use this function... Name, Email, Phone, PhotoUri, UID
        assert getActivity() != null;
        Map<String, Object> groupData = new HashMap<>();
        groupData.put(getString(R.string.isGroup), true);
        groupData.put("members", 1);
        for (Map<String, Object> member : memberDetails) {

            if (MainActivity.user != null) {

                if (member.get("UID").equals(MainActivity.user.getUid())) {
                    member.put("status", "approved");
                } else {
                    member.put("status", "pending");
                }
                groupData.put(member.get("UID").toString(), member);
            }
        }

        getActivity().findViewById(R.id.loadingIcon).setVisibility(View.VISIBLE);
        collectionReference
                .add(groupData)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(@NonNull DocumentReference documentReference) {

                        getActivity().findViewById(R.id.loadingIcon).setVisibility(View.GONE);
                        //Register the user in this group
                        Map<String, String> addGroupId = new HashMap<>(1);
                        addGroupId.put(getString(R.string.groupId), documentReference.getId());

                        //Add details to the current user
                        getActivity().findViewById(R.id.loadingIcon).setVisibility(View.VISIBLE);
                        collectionReference
                                .document(MainActivity.user.getUid())
                                .set(addGroupId, SetOptions.merge())
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        getActivity().findViewById(R.id.loadingIcon).setVisibility(View.GONE);
                                        Toast.makeText(getActivity(), R.string.groupCreationSuccess, Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        getActivity().findViewById(R.id.loadingIcon).setVisibility(View.GONE);
                                        Toast.makeText(getActivity(), R.string.error, Toast.LENGTH_SHORT).show();
                                    }
                                });

                        //TODO: add pending status to other users (alternative implemented in MainActivity)

                        //Add it in the user's travel history
                        getActivity().findViewById(R.id.loadingIcon).setVisibility(View.VISIBLE);
                        Map<String, Object> travelDetails = new HashMap<>();
                        travelDetails.put(getString(R.string.groupId), documentReference.getId());
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
                                        getActivity().findViewById(R.id.loadingIcon).setVisibility(View.GONE);
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        getActivity().findViewById(R.id.loadingIcon).setVisibility(View.GONE);
                                        Toast.makeText(getActivity(), R.string.error, Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        getActivity().findViewById(R.id.loadingIcon).setVisibility(View.GONE);
                        Toast.makeText(getActivity(), R.string.error, Toast.LENGTH_SHORT).show();
                    }
                });

    }
}
