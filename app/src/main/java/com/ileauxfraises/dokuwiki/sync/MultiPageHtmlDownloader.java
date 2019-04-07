package com.ileauxfraises.dokuwiki.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;
import com.ileauxfraises.dokuwiki.R;
import com.ileauxfraises.dokuwiki.WikiManager;

import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import java.net.URL;
import java.util.HashMap;
import java.util.Vector;

import androidx.appcompat.app.AppCompatActivity;

public class MultiPageHtmlDownloader extends XmlRpcDownload {
    String TAG = "MultiPageHtmlDownloader";
    WikiManager _wikiMngr;
    public HashMap<String, String> results = new HashMap<String, String>();

    public MultiPageHtmlDownloader(Context context, WikiManager wikiManager) {
        super(context);
        _wikiMngr = wikiManager;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        Log.d(TAG, "Download multiple pages Commencing");
    }

    @Override
    protected String doInBackground(String... params) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(pContext);
        String password = settings.getString("password", "");
        String user = settings.getString("user", "");;
        String urlserver = settings.getString("serverurl", "");;

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
                Vector parametersLogin = new Vector();
                parametersLogin.addElement(user);
                parametersLogin.addElement(password);
                Object result = client.execute("dokuwiki.login",parametersLogin);
                Log.d(TAG,"The result login is: "+ result);
                Log.d(TAG,"The cookies size is: "+ cookies.size());
                if(! ((Boolean) result)){
                    View toastView = ((AppCompatActivity)pContext).findViewById(R.id.view_content);
                    if(toastView == null){
                        toastView = ((AppCompatActivity)pContext).findViewById(R.id.webview);
                    }
                    Snackbar.make(toastView, "Login error ! please check user/password", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    return "Failed!";
                }
            }
            Vector parameters = new Vector();
            Log.d(TAG,"3 "+parameters.toString());
            //for (int i=0;i<params.length; i++) {params[i]);}
            for(String pagename : params){
                Log.d(TAG,"retrieve page: "+pagename);
                Object result = "";
                parameters.clear();
                parameters.addElement(pagename);
                try {
                    result = client.execute(methodName, parameters);
                    Log.d(TAG,"The result is: "+ result);
                    results.put(pagename, result.toString());
                    //TimeUnit.SECONDS.sleep(10);
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
            _wikiMngr.retrievedPageHtml(pagename, results.get(pagename));
        }

    }
}
