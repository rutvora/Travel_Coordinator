package com.travelbphc;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.uber.sdk.android.core.UberSdk;
import com.uber.sdk.android.rides.RideParameters;
import com.uber.sdk.android.rides.RideRequestButton;
import com.uber.sdk.rides.client.ServerTokenSession;
import com.uber.sdk.rides.client.SessionConfiguration;

import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    static WeakReference<FirebaseFirestore> db;
    static FirebaseAuth firebaseAuth;
    static GoogleSignInAccount account;
    @Nullable
    static FirebaseUser user;
    static WeakReference<AppCompatActivity> mainActivity;
    private final String CHANNEL_ID = "Channel ID";             //TODO

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Hide the loading icon
        findViewById(R.id.loadingIcon).setVisibility(View.GONE);

        //Set toolbar for burger menu and options
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        //Set Drawer content and settings
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        //NavigationView for (obviously) navigating to various parts of the app.
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //Initialize firebase and corresponding requirements
        FirebaseApp.initializeApp(this);
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        MainActivity.db = new WeakReference<>(db);
        firebaseAuth = FirebaseAuth.getInstance();

        //Check for firebase Sign-In
        user = firebaseAuth.getCurrentUser();
        //account = GoogleSignIn.getLastSignedInAccount(this);

        if (!(GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS)) {
            GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this);
        } else {

            if (user == null) {
                try {
                    assert getSupportActionBar() != null;
                    getSupportActionBar().hide();
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment, new SignIn()).commit();

                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            } else {
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment, new LocalTravel()).commit();

                //Get pending group requests
                db
                        .collection("Users")
                        .document(user.getUid())
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if (task.isSuccessful()) {
                                    DocumentSnapshot snapshot = task.getResult();           //TODO: Realise that the same can be used in travel history too
                                    assert snapshot.getData() != null;
                                    Set<String> keys = snapshot.getData().keySet();
                                    SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy'T'HH:mm'Z'", Locale.US);
                                    for (String key : keys) {
                                        try {
                                            if (new Date().after(format.parse(key))) {
                                                keys.remove(key);
                                            }
                                        } catch (ParseException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    checkPendingRequests(snapshot.getData(), keys);

                                } else {
                                    Toast.makeText(MainActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

            }

            //Create a weakReference to itself
            mainActivity = new WeakReference<AppCompatActivity>(this);
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_local) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment, new LocalTravel()).commit();
        } else if (id == R.id.nav_others) {

        } else if (id == R.id.nav_share) {

        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void checkPendingRequests(@NonNull Map<String, Object> travels, @NonNull Set<String> keys) {
        final Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Pending Group Requests")
                .setContentText("Click here to Check")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .build(); // Make it non-removable
        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        Map<String, Object> userDetails = new HashMap<>();
        userDetails.put(getString(R.string.name), user.getDisplayName());
        userDetails.put(getString(R.string.email), user.getEmail());
        userDetails.put(getString(R.string.phone), user.getPhoneNumber());           //TODO: find alternative
        userDetails.put(getString(R.string.photoUri), user.getPhotoUrl());
        userDetails.put("UID", user.getUid());
        userDetails.put(getString(R.string.status), "pending");
        for (String key : keys) {
            @SuppressWarnings("unchecked")      //TODO: Check if the typecasting works
                    Map<String, Object> travelDetails = (Map<String, Object>) travels.get(key);

            db.get()
                    .collection("Local")
                    .document(travelDetails.get(getString(R.string.fromTo)).toString())
                    .collection(travelDetails.get(getString(R.string.dateOfJourney)).toString())
                    .whereEqualTo(getString(R.string.isGroup), true)
                    .whereEqualTo(user.getUid(), userDetails)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                for (DocumentSnapshot doc : task.getResult()) {
                                    notificationManager.notify(0, notification);      //TODO
                                }
                            }
                        }
                    });

        }
    }

    private void approveRequest(@NonNull final DocumentSnapshot documentSnapshot) {
        final Map<String, Object> userDetails = new HashMap<>();
        userDetails.put(getString(R.string.name), user.getDisplayName());
        userDetails.put(getString(R.string.email), user.getEmail());
        userDetails.put(getString(R.string.phone), user.getPhoneNumber());           //TODO: find alternative
        userDetails.put(getString(R.string.photoUri), user.getPhotoUrl());
        userDetails.put("UID", user.getUid());
        userDetails.put(getString(R.string.status), "pending");
        findViewById(R.id.loadingIcon).setVisibility(View.VISIBLE);

        documentSnapshot
                .getReference()
                .update(user.getUid() + ".status", "approved",
                        "members", Integer.parseInt(documentSnapshot.getString("members")) + 1)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        //Remove from all others
                        documentSnapshot.getReference()
                                .getParent()
                                .whereEqualTo(getString(R.string.isGroup), true)
                                .whereEqualTo(user.getUid(), userDetails)
                                .get()
                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                        if (task.isSuccessful()) {
                                            for (DocumentSnapshot doc : task.getResult()) {
                                                doc.getReference()
                                                        .update(user.getUid(), FieldValue.delete())
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                findViewById(R.id.loadingIcon).setVisibility(View.GONE);
                                                            }
                                                        });
                                            }
                                        }
                                    }
                                });
                    }
                });
    }

    private void initializeUberSDK() {               //TODO: Place code in right location (button currently in get_list.xml)
        SessionConfiguration config = new SessionConfiguration.Builder()
                // mandatory
                .setClientId(getString(R.string.Uber_Client_Id))
                // required for enhanced button features
                .setServerToken(getString(R.string.Uber_Server_Token))
                // required for implicit grant authentication
                .setRedirectUri("<REDIRECT_URI>")
                // optional: set sandbox as operating environment
                .setEnvironment(SessionConfiguration.Environment.SANDBOX)
                .build();

        UberSdk.initialize(config);

        RideRequestButton uberButton = findViewById(R.id.getUber);

        ServerTokenSession session = new ServerTokenSession(config);
        uberButton.setSession(session);

        //TODO: Replace Sample Data
        RideParameters rideParams = new RideParameters.Builder()
                // Optional product_id from /v1/products endpoint (e.g. UberX). If not provided, most cost-efficient product will be used
                .setProductId("a1111c8c-c720-46c3-8534-2fcdd730040d")
                // Required for price estimates; lat (Double), lng (Double), nickname (String), formatted address (String) of dropoff location
                .setDropoffLocation(
                        37.775304, -122.417522, "Uber HQ", "1455 Market Street, San Francisco")
                // Required for pickup estimates; lat (Double), lng (Double), nickname (String), formatted address (String) of pickup location
                .setPickupLocation(37.775304, -122.417522, "Uber HQ", "1455 Market Street, San Francisco")
                .build();
// set parameters for the RideRequestButton instance
        uberButton.setRideParameters(rideParams);
        findViewById(R.id.loadingIcon).setVisibility(View.VISIBLE);
        uberButton.loadRideInformation();           //TODO: separate loader for this (use RideRequestButtonCallback interface)
    }

    private void createNotificationChannel() {                  //TODO
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Channel name";
            String description = "Channel Description";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
