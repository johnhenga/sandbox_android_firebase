package com.example.sandbox_firebase; // Make sure this is your correct package name

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.sandbox_firebase.databinding.ActivityLoginBinding; // Your binding class
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthMultiFactorException;
import com.google.firebase.auth.FirebaseUser;
// import com.google.firebase.auth.MultiFactorResolver; // Only if using MFA part

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "EmailPassword";

    private ActivityLoginBinding mBinding;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        EdgeToEdge.enable(this); // Optional: if you want edge-to-edge display

        // Adjust window insets listener to use your root view ID from activity_login.xml
        // Assuming your root layout in activity_login.xml has android:id="@+id/main"
        ViewCompat.setOnApplyWindowInsetsListener(mBinding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Buttons Click Listeners
        mBinding.emailSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = mBinding.fieldEmail.getText().toString();
                String password = mBinding.fieldPassword.getText().toString();
                signIn(email, password);
            }
        });

        mBinding.emailCreateAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = mBinding.fieldEmail.getText().toString();
                String password = mBinding.fieldPassword.getText().toString();
                createAccount(email, password);
            }
        });

        mBinding.signOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signOut();
            }
        });

        mBinding.verifyEmailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendEmailVerification();
            }
        });

        mBinding.reloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reload();
            }
        });

        // Set initial UI state (e.g., hide progress bar)
        mBinding.progressBar.setVisibility(View.GONE);
        updateUI(mAuth.getCurrentUser()); // Update UI based on current sign-in state
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);
        // It's common to call updateUI here as well, or if the user is not null,
        // you might want to automatically 'reload' to ensure their state is fresh.
        // if (currentUser != null) {
        //     reload();
        // }
    }

    private void createAccount(String email, String password) {
        Log.d(TAG, "createAccount:" + email);
        if (!validateForm()) {
            return;
        }

        mBinding.progressBar.setVisibility(View.VISIBLE);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                        } else {
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                            updateUI(null);
                        }
                        mBinding.progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void signIn(String email, String password) {
        Log.d(TAG, "signIn:" + email);
        if (!validateForm()) {
            return;
        }

        mBinding.progressBar.setVisibility(View.VISIBLE);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                        } else {
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                            updateUI(null);
                            // checkForMultiFactorFailure(task.getException()); // Commented out: MFA specific
                        }

                        // This status update was originally only if !task.isSuccessful(),
                        // but it might be better placed within the else block above.
                        // if (!task.isSuccessful()) {
                        //     mBinding.status.setText(R.string.auth_failed); // Ensure R.string.auth_failed exists
                        // }
                        mBinding.progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void signOut() {
        mAuth.signOut();
        updateUI(null);
    }

    private void sendEmailVerification() {
        mBinding.verifyEmailButton.setEnabled(false);
        final FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            mBinding.verifyEmailButton.setEnabled(true);
            Toast.makeText(this, "Not signed in, cannot send verification email.", Toast.LENGTH_SHORT).show();
            return;
        }

        user.sendEmailVerification()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        mBinding.verifyEmailButton.setEnabled(true);
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this,
                                    "Verification email sent to " + user.getEmail(),
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e(TAG, "sendEmailVerification", task.getException());
                            Toast.makeText(LoginActivity.this,
                                    "Failed to send verification email.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void reload() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Not signed in, cannot reload.", Toast.LENGTH_SHORT).show();
            return; // Not signed in, nothing to reload
        }
        mBinding.progressBar.setVisibility(View.VISIBLE);
        currentUser.reload().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    updateUI(mAuth.getCurrentUser()); // Re-fetch user in case state changed (e.g., email verified)
                    Toast.makeText(LoginActivity.this,
                            "Reload successful!",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "reload", task.getException());
                    Toast.makeText(LoginActivity.this,
                            "Failed to reload user. " + task.getException().getMessage(),
                            Toast.LENGTH_LONG).show();
                    // If reload fails (e.g. user deleted, token expired), sign them out locally
                    if (task.getException() instanceof com.google.firebase.auth.FirebaseAuthInvalidUserException) {
                        signOut();
                    }
                }
                mBinding.progressBar.setVisibility(View.GONE);
            }
        });
    }

    private boolean validateForm() {
        boolean valid = true;

        String email = mBinding.fieldEmail.getText().toString();
        if (TextUtils.isEmpty(email)) {
            mBinding.fieldEmail.setError("Required.");
            valid = false;
        } else {
            mBinding.fieldEmail.setError(null);
        }

        String password = mBinding.fieldPassword.getText().toString();
        if (TextUtils.isEmpty(password)) {
            mBinding.fieldPassword.setError("Required.");
            valid = false;
        } else {
            mBinding.fieldPassword.setError(null);
        }

        return valid;
    }

    private void updateUI(FirebaseUser user) {
        if (mBinding == null) return; // Guard against calls if binding is null (e.g. during very early/late lifecycle)

        mBinding.progressBar.setVisibility(View.GONE);
        if (user != null) {
            mBinding.status.setText(getString(R.string.emailpassword_status_fmt,
                    user.getEmail(), user.isEmailVerified())); // Pass the boolean directly
//            mBinding.status.setText(
//                    getString(
//                            R.string.emailpassword_status_fmt,
//                            user.getEmail(),
//                            String.valueOf(user.isEmailVerified()
//                            )));

            // R.string.firebase_status_fmt requires one argument: UID
            mBinding.detail.setText(getString(R.string.firebase_status_fmt, user.getUid()));

            mBinding.emailPasswordButtons.setVisibility(View.GONE);
            mBinding.emailPasswordFields.setVisibility(View.GONE);
            mBinding.signedInButtons.setVisibility(View.VISIBLE);

            if (user.isEmailVerified()) {
                mBinding.verifyEmailButton.setVisibility(View.GONE);
            } else {
                mBinding.verifyEmailButton.setVisibility(View.VISIBLE);
            }
        } else {
            mBinding.status.setText(R.string.signed_out); // Ensure R.string.signed_out exists
            mBinding.detail.setText(null);

            mBinding.emailPasswordButtons.setVisibility(View.VISIBLE);
            mBinding.emailPasswordFields.setVisibility(View.VISIBLE);
            mBinding.signedInButtons.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBinding = null; // Good practice to nullify binding in Activity's onDestroy
    }
}
