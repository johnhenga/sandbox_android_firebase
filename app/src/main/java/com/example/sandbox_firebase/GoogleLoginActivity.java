/**
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.sandbox_firebase;

import static com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL;

import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.ClearCredentialException;
import androidx.credentials.exceptions.GetCredentialException;
import com.example.sandbox_firebase.databinding.ActivityGoogleLoginBinding;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import java.util.concurrent.Executors;

public class GoogleLoginActivity extends AppCompatActivity {

    private static final String TAG = "GoogleActivity";

    private FirebaseAuth mAuth;
    private CredentialManager credentialManager;
    private ActivityGoogleLoginBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // EdgeToEdge.enable(this); // You can re-enable this if desired.

        mBinding = ActivityGoogleLoginBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(mBinding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Credential Manager
        credentialManager = CredentialManager.create(this);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Button listeners
        mBinding.signInButton.setOnClickListener(v -> signIn());
        mBinding.signOutButton.setOnClickListener(v -> signOut());

        // Display Credential Manager Bottom Sheet if user isn't logged in
        if (mAuth.getCurrentUser() == null) {
            showBottomSheet();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);
    }

    private void showProgressBar() {
        if (mBinding.progressBar != null) {
            mBinding.progressBar.setVisibility(View.VISIBLE);
        }
    }

    private void hideProgressBar() {
        if (mBinding.progressBar != null) {
            mBinding.progressBar.setVisibility(View.INVISIBLE);
        }
    }

    private void signIn() {
        showProgressBar();
        GetSignInWithGoogleOption signInWithGoogleOption = new GetSignInWithGoogleOption
                .Builder(getString(R.string.default_web_client_id))
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .build();

        launchCredentialManager(request);
    }

    private void showBottomSheet() {
        // No progress bar here, as it's an automatic prompt
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(getString(R.string.default_web_client_id))
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        launchCredentialManager(request);
    }

    private void launchCredentialManager(GetCredentialRequest request) {
        credentialManager.getCredentialAsync(
                this,
                request,
                new CancellationSignal(),
                Executors.newSingleThreadExecutor(),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        createGoogleIdToken(result.getCredential());
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        Log.e(TAG, "Couldn't retrieve user's credentials: " + e.getMessage(), e);
                        hideProgressBar(); // Ensure progress bar is hidden on error
                        Snackbar.make(mBinding.main, "Sign-in failed: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void createGoogleIdToken(Credential credential) {
        // showProgressBar() was called in signIn() or handled by Credential Manager UI
        // For direct calls if needed: runOnUiThread(this::showProgressBar);

        if (credential instanceof CustomCredential customCredential
                && credential.getType().equals(TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
            Bundle credentialData = customCredential.getData();
            try {
                GoogleIdTokenCredential googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credentialData);
                firebaseAuthWithGoogle(googleIdTokenCredential.getIdToken());
            } catch (Exception e) { // Catch potential exceptions from createFrom
                 Log.e(TAG, "Failed to create GoogleIdTokenCredential: " + e.getMessage(), e);
                 hideProgressBar();
                 Snackbar.make(mBinding.main, "Failed to process Google token.", Snackbar.LENGTH_SHORT).show();
            }
        } else {
            Log.w(TAG, "Credential is not of type Google ID Token or is null!");
            hideProgressBar();
            Snackbar.make(mBinding.main, "Received unexpected credential type.", Snackbar.LENGTH_SHORT).show();
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithCredential:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        updateUI(user); // This will hide progress bar
                    } else {
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        Snackbar.make(mBinding.main, "Authentication Failed.", Snackbar.LENGTH_SHORT).show();
                        updateUI(null); // This will hide progress bar
                    }
                    // hideProgressBar() is called within updateUI
                });
    }

    private void signOut() {
        showProgressBar();
        // Firebase sign out
        mAuth.signOut();

        ClearCredentialStateRequest clearRequest = new ClearCredentialStateRequest();
        credentialManager.clearCredentialStateAsync(
                clearRequest,
                new CancellationSignal(),
                Executors.newSingleThreadExecutor(),
                new CredentialManagerCallback<Void, ClearCredentialException>() {
                    @Override
                    public void onResult(@NonNull Void result) {
                        updateUI(null); // This will hide progress bar
                    }

                    @Override
                    public void onError(@NonNull ClearCredentialException e) {
                        Log.e(TAG, "Couldn't clear user credentials: " + e.getMessage(), e);
                        hideProgressBar(); // Explicitly hide on error if updateUI isn't called
                        Snackbar.make(mBinding.main, "Sign-out failed to clear credentials.", Snackbar.LENGTH_SHORT).show();

                    }
                });
    }

    private void updateUI(FirebaseUser user) {
        runOnUiThread(() -> {
            hideProgressBar();
            if (user != null) {
                mBinding.status.setText(getString(R.string.google_status_fmt, user.getEmail()));
                mBinding.detail.setText(getString(R.string.firebase_status_fmt, user.getUid()));

                mBinding.signInButton.setVisibility(View.GONE);
                mBinding.signOutButton.setVisibility(View.VISIBLE);
            } else {
                mBinding.status.setText(R.string.signed_out);
                mBinding.detail.setText(null);

                mBinding.signInButton.setVisibility(View.VISIBLE);
                mBinding.signOutButton.setVisibility(View.GONE);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBinding = null;
    }
}
