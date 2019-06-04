package com.fabienli.dokuwiki;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import com.google.android.material.navigation.NavigationView;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private WebView _webView;
    protected Context context;
    private final int SELECT_PHOTO = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // floating action: to be deleted?
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        fab.hide();

        // navigation drawer
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
            this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // the main web page
        _webView = (WebView) findViewById(R.id.webview);
        _webView.setWebViewClient(new MyWebViewClient());
        WebSettings webSettings = _webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAppCachePath(context.getCacheDir().getAbsolutePath());
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        displayHtml("Loading ...");

        // first page initiate
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String startpage = settings.getString("startpage", "start");
        displayPage(startpage);
    }

    @Override
    public void onResume() {
        super.onResume();

        //ensure cache is initiated
        WikiCacheUiOrchestrator.instance(this);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if(_webView.canGoBack()) {
            _webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Check if the key event was the Back button and if there's history
        if ((keyCode == KeyEvent.KEYCODE_BACK) && _webView.canGoBack()) {
            _webView.goBack();
            return true;
        }
        // If it wasn't the Back key or there's no web page history, bubble up to the default
        // system behavior (probably exit the activity)
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(MainActivity.this,SettingsActivity.class);
            startActivityForResult(intent, 0);
            return true;
        }
        else if (id == R.id.action_edit) {
            Intent intent = new Intent(MainActivity.this, EditActivity.class);
            intent.putExtra("pagename", WikiCacheUiOrchestrator.instance(this)._currentPageName);
            startActivityForResult(intent, 0);
            return true;
        }
        else if (id == R.id.action_force_sync) {
            WikiCacheUiOrchestrator.instance(this).forceDownloadPageHTMLforDisplay(_webView);
            return true;
        }
        else if (id == R.id.action_refresh) {
            WikiCacheUiOrchestrator.instance(this).refreshPage();
            return true;
        }
        else if (id == R.id.action_web_link) {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
            String baseurl = settings.getString("serverurl", "");
            String url = baseurl.replace("lib/exe/xmlrpc.php", "doku.php?id=")
                    + WikiCacheUiOrchestrator.instance(this)._currentPageName;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.startpage) {
            // get start page name from options
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this.context);
            String startpage = settings.getString("startpage", "start");
            displayPage(startpage);
        } else if (id == R.id.synchro) {
            WikiCacheUiOrchestrator.instance(this).updatePageListFromServer();
        } else if (id == R.id.logs) {
            String html = WikiCacheUiOrchestrator.instance(this).getLogsHtml();
            String encodedHtml = Base64.encodeToString(html.getBytes(), Base64.NO_PADDING);
            WebView myWebView = (WebView) findViewById(R.id.webview);
            myWebView.loadData(encodedHtml, "text/html", "base64");
        } else if (id == R.id.pagelist) {
            WikiCacheUiOrchestrator.instance(this).displayPageListHtml(_webView);
        } else if (id == R.id.actionList) {
            WikiCacheUiOrchestrator.instance(this).displayActionListPage(_webView);
        } else if (id == R.id.create) {
            WikiCacheUiOrchestrator.instance(this).createNewPageHtml(_webView);
        } else if (id == R.id.upload) {
            Intent photoPickerIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(photoPickerIntent, SELECT_PHOTO);
        } else if (id == R.id.mediamanager) {
            WikiCacheUiOrchestrator.instance(this).mediaManagerPageHtml(_webView);
        }
        else { // shortcuts to a page
            Log.d("Menu", String.valueOf(item));
            displayPage(String.valueOf(item));
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void displayPage(String pagename){
        WikiCacheUiOrchestrator.instance(this).retrievePageHTMLforDisplay(pagename, _webView);
    }

    public void displayHtml(String html){
        String encodedHtml = Base64.encodeToString(html.getBytes(), Base64.NO_PADDING);
        _webView.loadData(encodedHtml, "text/html", "base64");
    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.d("WebView", "link to: "+ url);
            Log.d("WebView", "link to: "+ Uri.parse(url));
            Log.d("WebView", "link to: "+ Uri.parse(url).getHost());
            if(url.startsWith("http://dokuwiki/doku.php?id=")){
                WebView myWebView = (WebView) findViewById(R.id.webview);
                String pagename = url.replace("http://dokuwiki/doku.php?id=", "");
                WikiCacheUiOrchestrator.instance(view.getContext()).retrievePageHTMLforDisplay(pagename, myWebView);

                return false;
            }
            else if(url.startsWith("http://dokuwiki_create/")){
                String pagename = url.replace("http://dokuwiki_create/?id=", "");
                // convert url characters
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        pagename = URLDecoder.decode(pagename, StandardCharsets.UTF_8.name());
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                // replace special characters with underscore
                pagename = pagename.replaceAll("[^-a-z0-9:]","_").toLowerCase();

                // call the edit window
                Intent intent = new Intent(MainActivity.this, EditActivity.class);
                intent.putExtra("pagename", pagename);
                startActivityForResult(intent, 0);

                return false;
            }
            String aBaseUrl = "file://"+context.getCacheDir().getAbsolutePath();
            if(url.startsWith(aBaseUrl)) // local cache folder, means invalid link
                return true;
            // Otherwise, the link is not for a page on my site, so launch another Activity that handles URLs
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
            return true;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch(requestCode) {
            case SELECT_PHOTO:
                if(resultCode == RESULT_OK){
                    try {
                        final Uri imageUri = imageReturnedIntent.getData();
                        Log.d("Upload", "got image: "+imageUri);
                        final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                        String newFileName = imageUri.getLastPathSegment();
                        Log.d("Upload", "to be saved as: "+newFileName);
                        WikiCacheUiOrchestrator.instance(this).savePictureAndShowMediaManagerPageHtml(newFileName, imageStream, _webView);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                else
                    WikiCacheUiOrchestrator.instance(this).mediaManagerPageHtml(_webView);
        }
    }

}


