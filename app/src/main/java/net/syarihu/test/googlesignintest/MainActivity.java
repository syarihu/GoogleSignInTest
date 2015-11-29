package net.syarihu.test.googlesignintest;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;

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
        mScope = new Scope("https://www.googleapis.com/auth/urlshortener");
        mGoogleSignInOptions = new GoogleSignInOptions
                .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(mScope)
                .requestProfile()
                .requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addScope(mScope)
                .addApi(Auth.GOOGLE_SIGN_IN_API, mGoogleSignInOptions)
                .build();

        SignInButton signInButton = (SignInButton) findViewById(R.id.sign_in_button);
//        signInButton.setScopes(new Scope[]{new Scope(Scopes.PLUS_LOGIN)});
//        signInButton.setSize(SignInButton.SIZE_WIDE);
        signInButton.setScopes(mGoogleSignInOptions.getScopeArray());
        signInButton.setOnClickListener(this);
        mStatusTextView = (TextView) findViewById(R.id.status);
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.sign_in_button) {
            signIn();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    private void handleSignInResult(GoogleSignInResult result) {
        Log.d(TAG, "handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            // サインインが成功したら、サインインボタンを消して、ユーザー名を表示する
            mGoogleSignInAccount = result.getSignInAccount();
            mStatusTextView.setText("DisplayName: " + mGoogleSignInAccount.getDisplayName());
            updateUI(true);
        } else {
            // サインアウトしたら、サインインボタンを表示する
            updateUI(false);
        }
    }

    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void updateUI(boolean signedIn) {
        if (signedIn) {
            findViewById(R.id.sign_in_button).setVisibility(View.GONE);
        } else {
            mStatusTextView.setText("status: サインアウト");
            findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    public void shorten(final String authToken) {
        new AsyncTask<Void, Void, Boolean>() {
            boolean mResult;

            @Override
            protected Boolean doInBackground(Void... voids) {
                mResult = true;
                try {
                    // POST URLの生成
                    Uri.Builder builder = new Uri.Builder();
                    builder.path("https://www.googleapis.com/urlshortener/v1/url");
                    // AccountManagerで取得したAuthTokenをaccess_tokenパラメータにセットする
                    builder.appendQueryParameter("access_token", authToken);
                    String postUrl = Uri.decode(builder.build().toString());

                    JSONObject jsonRequest = new JSONObject();
                    jsonRequest.put("longUrl", "http://www.google.co.jp/");
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
                    String postResponse = "";
                    while ((s = reader.readLine()) != null) {
                        postResponse += s + "\n";
                    }
                    reader.close();
                    Log.v(TAG, postResponse);

                    JSONObject shortenInfo = new JSONObject(postResponse);
                    // エラー判定
                    if (shortenInfo.has("error")) {
                        Log.e(TAG, postResponse);
                        mResult = false;
                    }
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                    mResult = false;
                }
                Log.v(TAG, "shorten finished.");

                return mResult;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (result) return;
            }
        }.execute();
    }
}
