package com.fabienli.dokuwiki;

import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.fabienli.dokuwiki.sync.XmlRpcThrottler;
import com.fabienli.dokuwiki.tools.Logs;
import com.fabienli.dokuwiki.tools.WikiUtils;
import com.fabienli.dokuwiki.usecase.PoolAsyncTask;
import com.fabienli.dokuwiki.usecase.UrlConverter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import com.google.android.material.navigation.NavigationView;

import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.MimeTypeMap;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private WebView _webView;
    protected Context context;
    private final int SELECT_PHOTO = 1;
    protected String _ongoingSearch = "";
    protected FloatingActionButton _fabSearchWiki;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        // check debug settings to displaye the menu with debug or not
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        boolean debugPanel = settings.getBoolean("debugpanel", true);
        if(debugPanel)
            setContentView(R.layout.activity_main_dev);
        else
            setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // floating action: to be deleted?
        _fabSearchWiki = (FloatingActionButton) findViewById(R.id.fab);
        _fabSearchWiki.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSearchRequested();
            }
        });
        _fabSearchWiki.hide();

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
        //webSettings.setAppCachePath(context.getCacheDir().getAbsolutePath());
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        // Get the intent, verify the action and get the query
        Intent intentSearch = getIntent();
        if (intentSearch != null && Intent.ACTION_SEARCH.equals(intentSearch.getAction()) && !WikiCacheUiOrchestrator.instance(this)._isSearchDone) {
            _ongoingSearch = intentSearch.getStringExtra(SearchManager.QUERY);
            Log.d("Search", "query search: " + _ongoingSearch);
            searchResultsPage(_ongoingSearch);
            _fabSearchWiki.show();
        }
        else {
            _ongoingSearch = "";
            displayHtml("Loading ...");

            String urlserver = settings.getString("serverurl", "");
            ;
            if (urlserver.length() == 0) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivityForResult(intent, 0);
            }
            // any page currently shown?
            else if (WikiCacheUiOrchestrator.instance(this).showLastHistory(_webView)) {
                // display done in showLastHistory(...)
            }
            // show default page
            else {
                // first page initiate
                String startpage = settings.getString("startpage", "start");
                displayPage(startpage);
            }
        }
    }

    protected void updateNavigationHeader() {
        try {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
            // update wiki's picture if any
            Boolean isLogoNeeded = settings.getBoolean("toplogo", false);
            if (isLogoNeeded) {
                String wikiTitle = settings.getString("wikiTitle", "");
                if (wikiTitle.length() == 0) {
                    wikiTitle = getResources().getString(R.string.nav_header_title);
                }
                NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
                View header = navigationView.getHeaderView(0);

                // Title
                TextView textTitle = (TextView) header.findViewById(R.id.navHeaderTitle);
                textTitle.setText(wikiTitle);

                // Logo
                ImageView topPicture = (ImageView) header.findViewById(R.id.topBarImageView);
                File filePng = new File(context.getCacheDir(), "logo.png");
                if (filePng.exists()) {
                    topPicture.setImageURI(Uri.fromFile(filePng));
                }
                File fileJpg = new File(context.getCacheDir(), "logo.jpg");
                if (fileJpg.exists()) {
                    topPicture.setImageURI(Uri.fromFile(fileJpg));
                }
            }
        }
        catch(NullPointerException e){
            Logs.getInstance().add("Can't update the header's logo or title");
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        //initiate throttling
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        int throttlingLimit = Integer.parseInt(settings.getString("throttlingPerMin", "1000"));
        XmlRpcThrottler xmlRpcThrottler = XmlRpcThrottler.instance();
        xmlRpcThrottler.setLimit(throttlingLimit);

        //ensure cache is initiated
        WikiCacheUiOrchestrator.instance(this);

        updateNavigationHeader();
    }

    @Override
    public void onBackPressed() {
        Log.d("MainActivity", "onBackPressed");
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        //} else if(_webView.canGoBack()) {
        //    _webView.goBack();
        } else {
            // make sure we don't have pending request ongoing:
            PoolAsyncTask.cleanPendingTasks();
            Boolean hadAPageBack = WikiCacheUiOrchestrator.instance(this).backHistory(_webView);
            if(!hadAPageBack)
                super.onBackPressed();
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Check if the key event was the Back button and if there's history
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            onBackPressed();
            return true;
        }
        /*if ((keyCode == KeyEvent.KEYCODE_BACK) && _webView.canGoBack()) {
            _webView.goBack();
            return true;
        }*/
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
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
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
            String url = WikiUtils.convertBaseUrlToMainWikiUrl(baseurl);
            url += WikiCacheUiOrchestrator.instance(this)._currentPageName;
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
            Snackbar.make(_webView.getRootView(), "Synchronisation starting, check details in notification bar... ", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();

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
            photoPickerIntent.setType("image/*");
            startActivityForResult(photoPickerIntent, SELECT_PHOTO);
        } else if (id == R.id.mediamanager) {
            WikiCacheUiOrchestrator.instance(this).mediaManagerPageHtml(_webView, "");
        } else if (id == R.id.search) {
            onSearchRequested();
        } else { // shortcuts to a page
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

    public void searchResultsPage(String searchLine){
        WikiCacheUiOrchestrator.instance(this).retrieveSearchResultsforDisplay(searchLine, _webView);
    }

    public void displayHtml(String html){
        String encodedHtml = Base64.encodeToString(html.getBytes(), Base64.NO_PADDING);
        _webView.loadData(encodedHtml, "text/html", "base64");
    }

    @Override
    public boolean onSearchRequested() {
        WikiCacheUiOrchestrator.instance(this)._isSearchDone = false;
        startSearch(_ongoingSearch, false, null, false);
        return true;
    }

    public void preventSearch() {
        // used to prevent search to popup again; execept if explicitely called
        WikiCacheUiOrchestrator.instance(this)._isSearchDone = true;
    }

    public void enableSearchButton(Boolean doit){
        if(doit) {
            _fabSearchWiki.show();
        }
        else
            _fabSearchWiki.hide();
    }

    protected class MyWebViewClient extends WebViewClient {
        public String _javascriptToLoadOnceFinished = "";
        private String _refreshUrlAfterJavascript = "";

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.d("WebView", "link to: "+ url);
            Log.d("WebView", "link to: "+ Uri.parse(url));
            Log.d("WebView", "link to: "+ Uri.parse(url).getHost());

            if(UrlConverter.isPluginActionOnline(url)){
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
                String baseurl = settings.getString("serverurl", "");
                String url_server = WikiUtils.convertBaseUrlToMainWikiUrl(baseurl);
                String plugin_url = UrlConverter.redirectPluginActionOnline(url, url_server);
                if(plugin_url.length() > 0) {
                    Log.d("WebView", "call plugin to: "+ plugin_url);
                    Snackbar.make(view, "Calling plugin DO!", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    // call external browser
                    //Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(plugin_url));
                    //startActivity(intent);
                    // call internal browser

                    WebView myWebView = (WebView) findViewById(R.id.webview);
                    MyWebViewClient myWebViewClient = (MyWebViewClient) myWebView.getWebViewClient();
                    String user = settings.getString("user","");
                    String pwd = settings.getString("password","");
                    myWebViewClient._javascriptToLoadOnceFinished =  "function setField(nam, val) {" +
                            "  var namField = document.getElementsByName(nam)[0];" +
                            "  if (namField != null) {" +
                            "    namField.value=val;" +
                            "    console.log(\" field updated : \" + nam + \" = \" + val);" +
                            "    return true;" +
                            "  }" +
                            "  return false;" +
                            "}" +
                            "setField(\"u\", '"+user+"');" +
                            "setField(\"p\", '"+pwd+"');" +
                            "console.log(\" hello : \" + ocument.getElementById('dw__login').action);" +
                            "document.getElementById('dw__login').submit();" +
                            "";
                    myWebViewClient._refreshUrlAfterJavascript = myWebView.getUrl();
                    myWebView.loadUrl(plugin_url);

                    // TODO: do this call in background
                    // TODO: initiate a force sync of the page
                }
                else {
                    Snackbar.make(view, "Unsupported action, please access the online page to do this!", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
                return true;
            }
            else if(UrlConverter.isInternalPageLink(url)){
                WebView myWebView = (WebView) findViewById(R.id.webview);
                String pagename = UrlConverter.getPageName(url);
                WikiCacheUiOrchestrator.instance(view.getContext()).retrievePageHTMLforDisplay(pagename, myWebView);

                return false;
            }
            else if(UrlConverter.isCreatePageLink(url)){
                String pagename = UrlConverter.getPageName(url);

                // call the edit window
                Intent intent = new Intent(MainActivity.this, EditActivity.class);
                intent.putExtra("pagename", pagename);
                startActivityForResult(intent, 0);

                return false;
            }
            else if(UrlConverter.isMediaManagerPageLink(url)){
                String args = UrlConverter.getPageName(url);
                WikiCacheUiOrchestrator.instance(view.getContext()).mediaManagerPageHtml(_webView, args);
                return false;
            }
            else if(UrlConverter.isLocalMediaLink(url)) {
                String filename = UrlConverter.getPageName(url);
                // link is to a local file; then to be displayed/downloaded
                Intent intent = new Intent();
                intent.setAction(android.content.Intent.ACTION_VIEW);
                File file = new File(filename);
                Uri fileURI = FileProvider.getUriForFile(context,
                        "com.fabienli.dokuwiki.fileprovider",
                        file);
                MimeTypeMap myMime = MimeTypeMap.getSingleton();
                String parts[]=url.split("\\.");
                String extension=parts[parts.length-1];
                String mimeType =
                        myMime.getMimeTypeFromExtension(extension);
                Log.d("URL shared", fileURI.toString());
                intent.setDataAndType(fileURI, mimeType);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                try {
                    startActivityForResult(intent, 10);
                }catch (ActivityNotFoundException e)
                {
                    Snackbar.make(view, "No application found to open "+filename, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
            }

            String aBaseUrl = "file://"+context.getCacheDir().getAbsolutePath();
            if(url.startsWith(aBaseUrl)) // local cache folder, means invalid link
                return true;

            // Otherwise, the link is not for a page on my site, so launch another Activity that handles URLs
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
            return true;
        }

        @Override
        public void onPageFinished(final WebView view, String url) {
            super.onPageFinished(view, url);
            Log.d("WebView", "onPageFinished: " + url);
            if(_javascriptToLoadOnceFinished.length() > 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                    view.evaluateJavascript(_javascriptToLoadOnceFinished, null);
                _javascriptToLoadOnceFinished = "";
                // refresh the page
                if(_refreshUrlAfterJavascript.length() > 0) {
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            _refreshUrlAfterJavascript = "";
                            WikiCacheUiOrchestrator.instance().forceDownloadPageHTMLforDisplay(_webView);
                        }
                    }, 1500); //1500 = 1.5 seconds, time in milli before it happens.
                }
            }
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
                        String[] filePathColumn = { MediaStore.Images.Media.DATA };
                        Cursor cursor = getContentResolver().query(imageUri,
                                filePathColumn, null, null, null);
                        cursor.moveToFirst();
                        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                        String fullPathName = cursor.getString(columnIndex);
                        cursor.close();
                        String[] fullPathNameSplit = fullPathName.split("/");
                        String newFileName = fullPathName;
                        if (fullPathNameSplit.length > 1) {
                            newFileName = UrlConverter.getFileName(fullPathNameSplit[fullPathNameSplit.length -1]);
                        }
                        Log.d("Upload", "to be saved as: "+newFileName);
                        WikiCacheUiOrchestrator.instance(this).
                                savePictureAndShowMediaManagerPageHtml(newFileName, imageStream, _webView);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                else
                    WikiCacheUiOrchestrator.instance(this).mediaManagerPageHtml(_webView , "");
        }
    }


}
