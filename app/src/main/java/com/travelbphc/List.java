package com.travelbphc;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by rutvora (www.github.com/rutvora)
 */

public class List extends Fragment implements View.OnClickListener {
    private DocumentReference doc;
    private ListView list;
    private Button getUsers;
    private Button getGroups;

    public List() {
        Bundle args = getArguments();
        doc = MainActivity.weakDBReference.get().collection("BITS Hyderabad").document(args.getString("location")).collection(args.getString("collection")).document(args.getString("document"));
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.get_list, container, false);
        getUsers = rootView.findViewById(R.id.getUsers);
        getUsers.setOnClickListener(this);
        getGroups = rootView.findViewById(R.id.getGroups);
        getGroups.setOnClickListener(this);
        list = rootView.findViewById(R.id.list);
        list.setVisibility(View.GONE);

        return rootView;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.getUsers) {
            assert getActivity() != null;
            getActivity().findViewById(R.id.loadingIcon).setVisibility(View.VISIBLE);
            doc.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    Map<String, Object> map = documentSnapshot.getData();
                    assert map != null;
                    assert getContext() != null;
                    String[] array = map.keySet().toArray(new String[map.size()]);
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, array);
                    list.setAdapter(adapter);
                    list.setVisibility(View.VISIBLE);
                    getActivity().findViewById(R.id.loadingIcon).setVisibility(View.GONE);
                    getGroups.setVisibility(View.GONE);
                    getUsers.setVisibility(View.GONE);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(getActivity(), "Couldn't get list (or no list exists)", Toast.LENGTH_SHORT).show();
                }
            });
        } else if (v.getId() == R.id.getGroups) {
            assert getActivity() != null;
            getActivity().findViewById(R.id.loadingIcon).setVisibility(View.VISIBLE);
            doc.collection("Groups").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        java.util.List<String> array = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            array.add(document.getString("Users"));
                        }
                        assert getContext() != null;
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, array);
                        list.setAdapter(adapter);
                        list.setVisibility(View.VISIBLE);
                        getActivity().findViewById(R.id.loadingIcon).setVisibility(View.GONE);
                        getGroups.setVisibility(View.GONE);
                        getUsers.setVisibility(View.GONE);
                    } else {
                        Toast.makeText(getActivity(), "Error", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
}
