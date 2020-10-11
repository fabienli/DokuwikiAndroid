package com.fabienli.dokuwiki.sync;

import android.os.AsyncTask;
import android.util.Log;

import com.fabienli.dokuwiki.tools.Utils;
import com.fabienli.dokuwiki.usecase.PoolAsyncTask;
import com.fabienli.dokuwiki.usecase.callback.MediaRetrieveCallback;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPInputStream;

public class StaticDownloader extends PoolAsyncTask {

    String TAG = "StaticDownloader";
    MediaRetrieveCallback _mediaRetrieveCallback = null;
    String _mediaPathName = "";
    String _urlserver = "";
    protected String _mediaLocalDir = "";
    protected Boolean _mediaDownloaded = false;

    public StaticDownloader(String urlserver, String mediaLocalDir) {
        _urlserver = convertUrlServer(urlserver);
        _mediaLocalDir = mediaLocalDir + "/";
    }

    public String convertUrlServer(String urlinput){
        String urlserver;
        String [] splitUrl = urlinput.split("/");
        if((urlinput.startsWith("http://") || urlinput.startsWith("https://"))) {
            if(splitUrl.length >= 3){
                urlserver = urlinput.substring(0, urlinput.lastIndexOf("/")) + "/";
            }
            else {
                urlserver = urlinput + "/";
            }
        }
        else {
            if(splitUrl.length > 1){
                urlserver = urlinput.substring(0, urlinput.lastIndexOf("/")) + "/";
            }
            else {
                urlserver = urlinput + "/";
            }

        }
        if(urlserver.endsWith("/lib/exe/"))
            urlserver = urlserver.substring(0, urlserver.length()-8);

        return  urlserver;
    }
    public String getStaticFile(String imageUrlStr) {
        File file = new File(_mediaLocalDir + imageUrlStr);
        if(file.exists()) {
            _mediaDownloaded = false;
        }
        else {
            try {
                Log.d(TAG, "Getting image " + imageUrlStr);
                URL imageUrl = new URL(_urlserver + imageUrlStr);
                Log.d(TAG, "From server url: " + imageUrl);
                HttpURLConnection conn = (HttpURLConnection) imageUrl
                        .openConnection();
                conn.setRequestProperty("Accept-Encoding", "gzip");
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(30000);
                conn.setInstanceFollowRedirects(true);
                InputStream is;
                if ("gzip".equals(conn.getContentEncoding())) {
                    is = new GZIPInputStream(conn.getInputStream());
                } else {
                    is = conn.getInputStream();
                }
                //String filePath = "file://" + _mediaLocalDir + imageUrlStr;
                File parent = file.getParentFile();
                if (!parent.exists()) {
                    parent.mkdirs();
                    Log.d(TAG, "creating parent folders for: " + parent.getAbsolutePath());
                }
                OutputStream os = new FileOutputStream(file);
                Utils.CopyStream(is, os);
                os.close();

            } catch (Exception ex) {
                Log.e(TAG, "Could not download image " + imageUrlStr);
                Log.e(TAG, ex.getMessage());
            }
            _mediaDownloaded = true;
        }
        return imageUrlStr;
    }


    public void getStaticAsync(String imageUrlStr, MediaRetrieveCallback mediaRetrieveCallback) {
        _mediaRetrieveCallback = mediaRetrieveCallback;
        executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,imageUrlStr);
    }


    @Override
    protected String doInBackground(String... params) {
        if(params.length == 1)
            _mediaPathName = getStaticFile(params[0]);
        return "ok";
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        if(_mediaRetrieveCallback!=null)
            if(_mediaDownloaded)
                _mediaRetrieveCallback.mediaRetrieved(_mediaPathName);
            else
                _mediaRetrieveCallback.mediaWasAlreadyThere(_mediaPathName);
    }
}
