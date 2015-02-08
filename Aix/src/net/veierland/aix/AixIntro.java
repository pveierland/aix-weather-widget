package net.veierland.aix;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

public class AixIntro extends Activity implements OnClickListener {

	private Button mOkButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		setContentView(R.layout.aix_intro);
		mOkButton = (Button)findViewById(R.id.ok);
		mOkButton.setOnClickListener(this);
		
		WebView web = (WebView) findViewById(R.id.webview);
		
		/*
		web.clearCache(true);
		WebSettings webSettings = web.getSettings();
	    webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
	    */
		
	    final ProgressDialog progressBar = ProgressDialog.show(this, "Aix Help", "Loading...");
        
        web.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                if (progressBar.isShowing()) {
                    progressBar.dismiss();
                }
            }
            
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				if (url != null && url.startsWith("http://")) {
					view.getContext().startActivity(
							new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
					return true;
				} else {
					return false;
				}
			}
        });
        
        web.loadUrl("http://www.veierland.net/aix/intro.html");
	}

	@Override
	public void onClick(View v) {
		if (v == mOkButton) {
			setResult(RESULT_OK);
			finish();
		}
	}
	
}
