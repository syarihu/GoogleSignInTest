package net.syarihu.test.googlesignintest;

import android.accounts.Account;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {
    private static final String TAG = "MainActivity";
    private static final String SHORTENER_SCOPE = "https://www.googleapis.com/auth/urlshortener";
    private static final String ACCOUNT_TYPE = "com.google";
    GoogleApiClient mGoogleApiClient;
    TextView mStatusTextView;
    Scope mScope;
    GoogleSignInAccount mGoogleSignInAccount;
    GoogleSignInOptions mGoogleSignInOptions;
    int RC_SIGN_IN = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mScope = new Scope(SHORTENER_SCOPE);
        mGoogleSignInOptions = new GoogleSignInOptions
                .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(mScope)
                .requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, mGoogleSignInOptions)
                .addScope(mScope)
                .build();
        mGoogleApiClient.connect();

        SignInButton signInButton = (SignInButton) findViewById(R.id.sign_in_button);
        signInButton.setOnClickListener(this);
        findViewById(R.id.sign_out_button).setOnClickListener(this);
        mStatusTextView = (TextView) findViewById(R.id.status);
        findViewById(R.id.btn_shorten).setOnClickListener(this);
        findViewById(R.id.long_uri).setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    shorten(((EditText) findViewById(R.id.long_uri)).getText().toString());
                    return true;
                }
                return false;
            }
        });
        mStatusTextView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                cm.setPrimaryClip(ClipData.newPlainText("", ((TextView) v).getText()));
                showClipboardSnackBar(((TextView) v).getText().toString());
                return true;
            }
        });
    }

    private void showSnackBar(String message) {
        Snackbar.make(findViewById(R.id.main_root), message, Snackbar.LENGTH_LONG).show();
    }

    private void showClipboardSnackBar(final String uri) {
        Snackbar.make(findViewById(R.id.main_root), R.string.copy_to_clipboard, Snackbar.LENGTH_LONG)
                .setAction(R.string.open, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                        startActivity(intent);
                    }
                })
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);
        if (opr.isDone()) {
            GoogleSignInResult result = opr.get();
            handleSignInResult(result);
            showShortenBox(true);
            showSignInButton(false);
            showSnackBar(getSignInMessage(result));
            updateStatus(getSignInMessage(result));
        } else {
            opr.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                @Override
                public void onResult(@NonNull GoogleSignInResult googleSignInResult) {
                    showSignInButton(true);
                    showShortenBox(false);
                }
            });
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.sign_in_button) {
            signIn();
        } else if(v.getId() == R.id.sign_out_button) {
            signOut();
        } else if (v.getId() == R.id.btn_shorten) {
            shorten(((EditText) findViewById(R.id.long_uri)).getText().toString());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                showSnackBar(getSignInMessage(result));
            }
            handleSignInResult(result);
        }
    }

    private void handleSignInResult(GoogleSignInResult result) {
        Log.d(TAG, "handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            mGoogleSignInAccount = result.getSignInAccount();
            showSignInButton(false);
            showShortenBox(true);
        } else {
            showSnackBar(getString(R.string.failed_to_signed_in));
        }
    }

    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void signOut() {
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
            new ResultCallback<Status>() {
                @Override
                public void onResult(@NonNull Status status) {
                    showSnackBar(getString(R.string.signed_out));
                    showSignInButton(true);
                    showShortenBox(false);
                    updateStatus("");
                }
        });
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    /**
     * URLを短縮する
     */
    public void shorten(final String longUrl) {
        if (TextUtils.isEmpty(longUrl)) {
            updateStatus(getString(R.string.please_enter_the_url));
            return;
        }
        new AsyncTask<Void, Integer, String>() {
            ProgressDialog mProgressDialog;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mProgressDialog = new ProgressDialog(MainActivity.this);
                mProgressDialog.setMessage(getString(R.string.loading));
                mProgressDialog.setCancelable(false);
                mProgressDialog.show();
            }

            @Override
            protected String doInBackground(Void... voids) {
                String accessToken = "";
                String postResponse = "";
                try {
                    accessToken = GoogleAuthUtil.getToken(
                            MainActivity.this,
                            new Account(mGoogleSignInAccount.getEmail(), ACCOUNT_TYPE),
                            "oauth2:" + SHORTENER_SCOPE
                    );
                    Log.d(TAG, "accessToken: " + accessToken);
                } catch (IOException | GoogleAuthException e) {
                    e.printStackTrace();
                }
                try {
                    // POST URLの生成
                    Uri.Builder builder = new Uri.Builder();
                    builder.path("https://www.googleapis.com/urlshortener/v1/url");
                    builder.appendQueryParameter("access_token", accessToken);
                    String postUrl = Uri.decode(builder.build().toString());

                    JSONObject jsonRequest = new JSONObject();
                    jsonRequest.put("longUrl", longUrl);
                    URL url = new URL(postUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoInput(true);
                    conn.setDoOutput(true);
                    PrintStream ps = new PrintStream(conn.getOutputStream());
                    ps.print(jsonRequest.toString());
                    ps.close();

                    // POSTした結果を取得
                    InputStream is = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    String s;
                    while ((s = reader.readLine()) != null) {
                        postResponse += s + "\n";
                    }
                    reader.close();
                    Log.v(TAG, postResponse);

                    JSONObject shortenInfo = new JSONObject(postResponse);
                    // エラー判定
                    if (shortenInfo.has("error")) {
                        Log.e(TAG, postResponse);
                    } else {
                        return shortenInfo.getString("id");
                    }
                } catch (IOException | JSONException e) {
                    Log.e("MainActivity", "", e);
                }
                Log.v(TAG, "shorten finished.");

                return postResponse;
            }

            @Override
            protected void onPostExecute(String result) {
                mProgressDialog.dismiss();
                if (TextUtils.isEmpty(result)) {
                    updateStatus(getString(R.string.shortening_failure));
                    showSnackBar(getString(R.string.shortening_failure));
                    return;
                }
                Log.d(TAG, result);
                mStatusTextView.setText(result);
                showSnackBar(getString(R.string.shortening_completion));
            }
        }.execute();
    }

    private void updateStatus(String status) {
        if (mStatusTextView == null)
            return;
        if (TextUtils.isEmpty(status))
            mStatusTextView.setVisibility(View.GONE);
        else
            mStatusTextView.setVisibility(View.VISIBLE);
        mStatusTextView.setText(status);
    }

    /**
     * URL短縮のためのUIの表示状態を変える
     */
    private void showShortenBox(boolean visible) {
        if (visible) {
            findViewById(R.id.shorten_box).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.shorten_box).setVisibility(View.GONE);
        }
    }

    private void showSignInButton(boolean isVisible) {
        if (isVisible) {
            findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
            findViewById(R.id.sign_out_button).setVisibility(View.GONE);
        } else {
            findViewById(R.id.sign_out_button).setVisibility(View.VISIBLE);
            findViewById(R.id.sign_in_button).setVisibility(View.GONE);
        }
    }

    private String getSignInMessage(@NonNull GoogleSignInResult result) {
        if (result.getSignInAccount() == null)
            return "";
        return String.format(getString(R.string.signed_in), result.getSignInAccount().getEmail());
    }
}
