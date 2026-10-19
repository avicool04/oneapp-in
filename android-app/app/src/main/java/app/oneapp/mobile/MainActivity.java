package app.oneapp.mobile;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.util.concurrent.Executor;

public class MainActivity extends Activity {
    private static final int FILE_CHOOSER_REQUEST = 4102;
    private static final String APP_URL = "https://oneapp-in.vercel.app/";
    private WebView webView;
    private ValueCallback<Uri[]> pendingFileCallback;
    private boolean needsUnlock = true;
    private boolean authenticating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(true);
        }
        window.setStatusBarColor(Color.rgb(85, 71, 216));
        window.setNavigationBarColor(Color.WHITE);
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.WHITE);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.getSettings().setAllowFileAccess(false);
        webView.getSettings().setAllowContentAccess(true);
        webView.getSettings().setSupportMultipleWindows(false);

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            cookies.setAcceptThirdPartyCookies(webView, true);
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String scheme = request.getUrl().getScheme();
                return !("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme));
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (pendingFileCallback != null) pendingFileCallback.onReceiveValue(null);
                pendingFileCallback = filePathCallback;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                try {
                    startActivityForResult(Intent.createChooser(intent, "Choose documents"), FILE_CHOOSER_REQUEST);
                } catch (Exception error) {
                    pendingFileCallback = null;
                    filePathCallback.onReceiveValue(null);
                    return false;
                }
                return true;
            }
        });

        setContentView(webView);
        if (savedInstanceState == null) {
            webView.loadUrl(APP_URL);
        } else {
            webView.restoreState(savedInstanceState);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (needsUnlock && !authenticating) showUnlockPrompt();
    }

    @Override
    protected void onPause() {
        if (!authenticating && !isFinishing()) needsUnlock = true;
        super.onPause();
    }

    private void showUnlockPrompt() {
        KeyguardManager keyguard = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        if (keyguard == null || !keyguard.isDeviceSecure()) {
            // Do not lock the user out if Android has no screen lock configured.
            needsUnlock = false;
            return;
        }

        int authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG
                | BiometricManager.Authenticators.DEVICE_CREDENTIAL;
        BiometricManager biometricManager = BiometricManager.from(this);
        int availability = biometricManager.canAuthenticate(authenticators);
        if (availability != BiometricManager.BIOMETRIC_SUCCESS) {
            // Android device PIN/password/pattern remains a valid fallback when
            // biometrics are unavailable; the prompt asks Android to use that.
            availability = biometricManager.canAuthenticate(
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL);
            if (availability != BiometricManager.BIOMETRIC_SUCCESS) {
                needsUnlock = false;
                return;
            }
        }

        authenticating = true;
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt prompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        authenticating = false;
                        needsUnlock = false;
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        authenticating = false;
                        if (errorCode == BiometricPrompt.ERROR_USER_CANCELED
                                || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                                || errorCode == BiometricPrompt.ERROR_CANCELED) {
                            needsUnlock = true;
                            finish();
                        } else {
                            needsUnlock = true;
                            finish();
                        }
                    }
                });

        BiometricPrompt.PromptInfo.Builder builder = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock OneApp")
                .setSubtitle("Use your fingerprint, face, or device screen lock")
                .setAllowedAuthenticators(authenticators);
        prompt.authenticate(builder.build());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != FILE_CHOOSER_REQUEST || pendingFileCallback == null) return;

        Uri[] results = null;
        if (resultCode == RESULT_OK && data != null) {
            ClipData clipData = data.getClipData();
            if (clipData != null) {
                results = new Uri[clipData.getItemCount()];
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    results[i] = clipData.getItemAt(i).getUri();
                }
            } else if (data.getData() != null) {
                results = new Uri[] { data.getData() };
            }
        }
        pendingFileCallback.onReceiveValue(results);
        pendingFileCallback = null;
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (webView != null) webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        if (pendingFileCallback != null) {
            pendingFileCallback.onReceiveValue(null);
            pendingFileCallback = null;
        }
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
