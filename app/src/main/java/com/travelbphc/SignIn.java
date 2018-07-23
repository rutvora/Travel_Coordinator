package com.travelbphc;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.GoogleAuthProvider;

/**
 * Created by rutvora (www.github.com/rutvora)
 */

public class SignIn extends Fragment implements View.OnClickListener {

    public SignIn() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.sign_in, container, false);
        rootView.findViewById(R.id.sign_in_button).setOnClickListener(this);

        return rootView;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                signIn();
                break;
            // ...
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == 0) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            MainActivity.account = completedTask.getResult(ApiException.class);

            // Signed in successfully, show authenticated UI.
            firebaseAuthWithGoogle(MainActivity.account);

        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Toast.makeText(getActivity(), R.string.error, Toast.LENGTH_SHORT).show();
        }
    }

    private void signIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .setHostedDomain(getString(R.string.domain))
                .requestIdToken(getString(R.string.Web_Client_Api_Key))
                .requestEmail()
                .requestProfile()
                .build();
        assert getContext() != null;
        GoogleSignInClient signInClient = GoogleSignIn.getClient(getContext(), gso);
        Intent signInIntent = signInClient.getSignInIntent();
        startActivityForResult(signInIntent, 0);
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {

        assert getActivity() != null;
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        MainActivity.firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            MainActivity.user = MainActivity.firebaseAuth.getCurrentUser();

                            //Update UI
                            ActionBar actionBar = MainActivity.mainActivity.get().getSupportActionBar();
                            assert actionBar != null;
                            actionBar.show();
                            getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.fragment, new LocalTravel()).commit();

                        } else {
                            // If sign in fails, display a message to the user.
                            Toast.makeText(getActivity(), R.string.error, Toast.LENGTH_SHORT).show();

                        }
                    }
                });
    }
}
