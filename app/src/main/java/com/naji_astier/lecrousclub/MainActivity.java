package com.naji_astier.lecrousclub;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener {

    private static final String TAG = "LeCrousClub";
    private static final int RC_SIGN_IN = 9001;

    // [START declare_auth]
    private FirebaseAuth mAuth;
    // [END declare_auth]

    // [START declare_auth_listener]
    private FirebaseAuth.AuthStateListener mAuthListener;
    // [END declare_auth_listener]

    FirebaseDatabase database;

    private GoogleApiClient mGoogleApiClient;
    private TextView mStatusTextView;
    private TextView mRealBrosHeadTextView;
    private TextView mRealBrosBodyTextView;
    private TextView mFalseBrosHeadTextView;
    private TextView mFalseBrosBodyTextView;

    private FirebaseUser user;
    private String today;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Views
        mStatusTextView = (TextView) findViewById(R.id.status);
        mRealBrosHeadTextView = (TextView) findViewById(R.id.headRealBros);
        mRealBrosBodyTextView = (TextView) findViewById(R.id.bodyRealBros);
        mFalseBrosHeadTextView = (TextView) findViewById(R.id.headFalseBros);
        mFalseBrosBodyTextView = (TextView) findViewById(R.id.bodyFalseBros);
        final Button cancelButton = (Button) findViewById(R.id.button_cancel);

        // Button listeners
        findViewById(R.id.sign_in_button).setOnClickListener(this);
        //findViewById(R.id.sign_out_button).setOnClickListener(this);
        //findViewById(R.id.disconnect_button).setOnClickListener(this);
        findViewById(R.id.button_yes).setOnClickListener(this);
        findViewById(R.id.button_no).setOnClickListener(this);
        findViewById(R.id.button_cancel).setOnClickListener(this);

        // [START config_signin]
        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        // [END config_signin]

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        // [START initialize_auth]
        mAuth = FirebaseAuth.getInstance();
        // [END initialize_auth]

        // Database
        database = FirebaseDatabase.getInstance();

        final LinearLayout askWantToParticipate = (LinearLayout) this.findViewById(R.id.want_to_participate);
        askWantToParticipate.setVisibility(LinearLayout.GONE);

        Date date = new Date();
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour > 14)
        {
            date.setTime(date.getTime() + 86400000);
        }
        today = new SimpleDateFormat("dd-MM-yyyy", Locale.FRENCH).format(date);
        TextView todaysDate = (TextView) findViewById(R.id.todays_date);
        todaysDate.setText(new SimpleDateFormat("EEEE dd MMMM yyyy", Locale.FRENCH).format(date));

        // [START auth_state_listener]
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());

                    FirebaseMessaging.getInstance().subscribeToTopic("notifications");
                    final String token = FirebaseInstanceId.getInstance().getToken();
                    //Log.d(TAG, "Token: " + token);

                    // We write the user name in database if it does not exist
                    final DatabaseReference userNameRef = database.getReference("users/" + user.getUid());
                    userNameRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            // This method is called once with the initial value and again
                            // whenever data at this location is updated.
                            if (!dataSnapshot.exists()) {
                                userNameRef.child("name").setValue(user.getDisplayName());
                            }
                            userNameRef.child("token").setValue(token);
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            // Failed to read value
                            Log.w(TAG, "Failed to read value.", error.toException());
                        }
                    });

                    if (getIntent().getExtras() != null) {
                        boolean participation = getIntent().getExtras().getBoolean("participation");

                        Log.d(TAG, "Extra received : " + participation);
                    }

                    final DatabaseReference userParticipationRef = database.getReference("resas/" + today + "/" + user.getDisplayName());

                    userParticipationRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            // This method is called once with the initial value and again
                            // whenever data at this location is updated.
                            Boolean value = dataSnapshot.getValue(Boolean.class);
                            //Log.d(TAG, "Value is: " + value);

                            if (value == null) {
                                cancelButton.setVisibility(Button.GONE);
                                askWantToParticipate.setVisibility(LinearLayout.VISIBLE);
                            } else {
                                askWantToParticipate.setVisibility(LinearLayout.GONE);
                                if (value)
                                    cancelButton.setText(getString(R.string.cancel_participation));
                                else
                                    cancelButton.setText(getString(R.string.cancel_no_participation));

                                cancelButton.setVisibility(Button.VISIBLE);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            // Failed to read value
                            Log.w(TAG, "Failed to read value.", error.toException());
                        }
                    });

                    DatabaseReference usersParticipationRef = database.getReference("resas/" + today);

                    usersParticipationRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            final StringBuffer participating = new StringBuffer("");
                            final StringBuffer notParticipating = new StringBuffer("");
                            for (DataSnapshot userSnapshot: dataSnapshot.getChildren()) {
                                Boolean value = userSnapshot.getValue(Boolean.class);
                                String userName = userSnapshot.getKey();
                                if (value) {
                                    participating.append("\n - " + userName);
                                } else {
                                    notParticipating.append("\n - " + userName);
                                }
                            }

                            //Log.w(TAG, participants.toString());
                            mRealBrosHeadTextView.setText("Les vrais bros");
                            mFalseBrosHeadTextView.setText("Les faux frères");
                            mRealBrosBodyTextView.setText(participating.toString());
                            mFalseBrosBodyTextView.setText(notParticipating.toString());
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            // Getting Post failed, log a message
                            Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
                        }
                    });
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
                // [START_EXCLUDE]
                updateUI(user);
                // [END_EXCLUDE]
            }
        };
        // [END auth_state_listener]
        Intent i = new Intent(this, MyFirebaseInstanceIDService.class);
        startService(i);
        i = new Intent(this, MyFirebaseMessagingService.class);
        startService(i);
    }

    // [START on_start_add_listener]
    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }
    // [END on_start_add_listener]

    // [START on_stop_remove_listener]
    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }
    // [END on_stop_remove_listener]

    // [START onactivityresult]
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = result.getSignInAccount();
                firebaseAuthWithGoogle(account);
            } else {
                // Google Sign In failed, update UI appropriately
                // [START_EXCLUDE]
                updateUI(null);
                Log.d(TAG, result.getStatus().toString());
                // [END_EXCLUDE]
            }
        }
    }
    // [END onactivityresult]

    // [START auth_with_google]
    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());
        // [START_EXCLUDE silent]
        //showProgressDialog();
        // [END_EXCLUDE]

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInWithCredential:onComplete:" + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "signInWithCredential", task.getException());
                            Toast.makeText(MainActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                        // [START_EXCLUDE]
                        //hideProgressDialog();
                        // [END_EXCLUDE]
                    }
                });
    }
    // [END auth_with_google]

    // [START signin]
    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
    // [END signin]

    private void signOut() {
        // Firebase sign out
        mAuth.signOut();

        // Google sign out
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        updateUI(null);
                    }
                });
    }

    private void revokeAccess() {
        // Firebase sign out
        mAuth.signOut();

        // Google revoke access
        Auth.GoogleSignInApi.revokeAccess(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        updateUI(null);
                    }
                });
    }

    private void updateUI(FirebaseUser user) {
        //hideProgressDialog();
        if (user != null) {
            mStatusTextView.setText(getString(R.string.hello_username, user.getDisplayName()));
            //mDetailTextView.setText(getString(R.string.firebase_status_fmt, user.getUid()));

            findViewById(R.id.sign_in_button).setVisibility(View.GONE);
            //findViewById(R.id.sign_out_and_disconnect).setVisibility(View.VISIBLE);
        } else {
            mStatusTextView.setText(R.string.signed_out);
            mRealBrosHeadTextView.setText(null);
            mFalseBrosHeadTextView.setText(null);
            mRealBrosBodyTextView.setText(null);
            mFalseBrosBodyTextView.setText(null);

            findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
            //findViewById(R.id.sign_out_and_disconnect).setVisibility(View.GONE);
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
        Toast.makeText(this, "Google Play Services error.", Toast.LENGTH_SHORT).show();
    }

    public void setParticipationTo(Boolean value) {
        if (value == null) {
            database.getReference("resas/" + today + "/" + user.getDisplayName()).removeValue();
        } else {
            database.getReference("resas/" + today + "/" + user.getDisplayName()).setValue(value);
        }
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.sign_in_button) {
            signIn();
        } else if (i == R.id.button_yes) {
            setParticipationTo(true);
        } else if (i == R.id.button_no) {
            setParticipationTo(false);
        } else if (i == R.id.button_cancel) {
            setParticipationTo(null);
        }
        /* else if (i == R.id.sign_out_button) {
            signOut();
        } else if (i == R.id.disconnect_button) {
            revokeAccess();
        }*/
    }
}
