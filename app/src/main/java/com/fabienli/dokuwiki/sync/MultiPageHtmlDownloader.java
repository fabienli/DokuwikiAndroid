package com.fabienli.dokuwiki.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;
import com.fabienli.dokuwiki.R;
import com.fabienli.dokuwiki.WikiCacheUiOrchestrator;

import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import java.net.URL;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import androidx.appcompat.app.AppCompatActivity;

public class MultiPageHtmlDownloader extends XmlRpcDownload {

    String TAG = "MultiPageHtmlDownloader";
    WikiCacheUiOrchestrator _wikiMngr;
    public HashMap<String, String> results = new HashMap<String, String>();

    public MultiPageHtmlDownloader(Context context, WikiCacheUiOrchestrator wikiCacheUiOrchestrator) {
        super(context);
        _wikiMngr = wikiCacheUiOrchestrator;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        Log.d(TAG, "Download multiple pages Commencing");
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        // update sync information to user
        if(values.length==1) {
            String message = "Progress: " + String.valueOf(values[0]) + "%";
            SpannableString ss2 = new SpannableString(message);
            ss2.setSpan(new RelativeSizeSpan(2f), 0, ss2.length(), 0);
            ss2.setSpan(new ForegroundColorSpan(Color.BLACK), 0, ss2.length(), 0);
            _dialog.setMessage(ss2);
        }
        else if(values.length==2) {
            String message = "Progress: " + String.valueOf(values[0]) + "/" + String.valueOf(values[1]);
            SpannableString ss2 = new SpannableString(message);
            ss2.setSpan(new RelativeSizeSpan(2f), 0, ss2.length(), 0);
            ss2.setSpan(new ForegroundColorSpan(Color.BLACK), 0, ss2.length(), 0);
            _dialog.setMessage(ss2);
        }
    }


    @Override
    protected String doInBackground(String... params) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(_context);
        String password = settings.getString("password", "");
        String user = settings.getString("user", "");;
        String urlserver = settings.getString("serverurl", "");
        Integer urldelay = Integer.parseInt(settings.getString("list_delay_sync", "0"));

        try {
            Log.d(TAG,"Connecting to server <"+urlserver + "> with user <"+user+">");
            String methodName = "wiki.getPageHTML";
            XmlRpcClientConfigImpl xmlConfig = new XmlRpcClientConfigImpl();
            xmlConfig.setServerURL(new URL(urlserver));
            client.setTransportFactory(getXmlRpcSunHttpTransportFactoryInstance());
            client.setConfig(xmlConfig);
            Log.d(TAG,"2");
            // login first
            if(cookies.size() == 0) {
                publishProgress(0);
                Vector parametersLogin = new Vector();
                parametersLogin.addElement(user);
                parametersLogin.addElement(password);
                Object result = client.execute("dokuwiki.login",parametersLogin);
                Log.d(TAG,"The result login is: "+ result);
                Log.d(TAG,"The cookies size is: "+ cookies.size());
                if(! ((Boolean) result)){
                    View toastView = ((AppCompatActivity) _context).findViewById(R.id.view_content);
                    if(toastView == null){
                        toastView = ((AppCompatActivity) _context).findViewById(R.id.webview);
                    }
                    Snackbar.make(toastView, "Login error ! please check user/password", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    return "Failed!";
                }
            }
            Vector parameters = new Vector();
            Log.d(TAG,"3 "+parameters.toString());
            int i=0;
            for(String pagename : params){
                Log.d(TAG,"retrieve page: "+pagename);
                i++;
                // progress in percentage:
                // publishProgress((int)(i*100/(float)params.length));
                // progress in ratio:
                publishProgress(i, params.length);
                // get this page
                Object result = "";
                parameters.clear();
                parameters.addElement(pagename);
                try {
                    result = client.execute(methodName, parameters);
                    Log.d(TAG,"The result is: "+ result);
                    results.put(pagename, result.toString());
                    TimeUnit.SECONDS.sleep(urldelay);
                    Log.d(TAG,"Delay in sync: "+ urldelay + "s");
                } catch (org.apache.xmlrpc.client.XmlRpcClientException exception) {
                    Log.e(TAG,"JavaClient for page "+pagename+": " + exception);

                }
            }
            Log.d(TAG,"4");
            Log.d(TAG,"The result size: "+ results.size()+" / "+ params.length);
        } catch (Exception exception) {
            Log.e(TAG,"JavaClient: " + exception);
        }

        return "Executed!";

    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        for(String pagename : results.keySet()){
            _wikiMngr.updatePageInCache(pagename, results.get(pagename));
        }

    }
}
