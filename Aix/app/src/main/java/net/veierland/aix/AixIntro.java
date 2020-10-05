package net.veierland.aix;

import android.app.Activity;
import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class AixIntro extends Activity {
	
	public final static String ACTION_SHOW_HELP = "aix.intent.action.SHOW_HELP";
	public final static String ACTION_SHOW_INTRODUCTION = "aix.intent.action.SHOW_INTRODUCTION";
	public final static String ACTION_SHOW_DEVICE_PROFILE_GUIDE = "aix.intent.action.SHOW_DEVICE_PROFILE_GUIDE";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		setContentView(R.layout.intro);
		
		Uri uri = getIntent().getData();
		String loadingMessage = null, url = null;
		
		if (uri != null && uri.equals(ACTION_SHOW_HELP))
		{
			url = "https://www.veierland.net/aix/help/?https";
			loadingMessage = "Loading Aix help..";
		}
		else if (uri != null && uri.equals(ACTION_SHOW_DEVICE_PROFILE_GUIDE))
		{
			url = "https://www.veierland.net/aix/device_profiles/?https";
			loadingMessage = "Loading Device Profile guide..";
		}
		else
		{
			// Show intro as default
			url = "https://www.veierland.net/aix/introduction/?https";
			loadingMessage = "Loading Aix introduction..";
		}
		
		WebView web = findViewById(R.id.webview);
		
	    final ProgressDialog progressBar = ProgressDialog.show(this, "Aix Weather Widget", loadingMessage);
        
        web.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                if (progressBar.isShowing()) {
                    progressBar.dismiss();
                }
            }

            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                if (failingUrl.startsWith("https://")) {
                    view.loadUrl(failingUrl.replace("https://", "http://").concat("?http"));
                }
            }
            
            public boolean shouldOverrideUrlLoading(WebView view, String url){
            	view.loadUrl(url);
            	return false;
            }
		});
        
        web.loadUrl(url);
	}
	
}
