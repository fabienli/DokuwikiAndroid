package com.ileauxfraises.dokuwiki.sync;

import android.content.Context;
import com.google.android.material.navigation.NavigationView;
import com.ileauxfraises.dokuwiki.MainActivity;
import com.ileauxfraises.dokuwiki.R;

import android.util.Base64;
import android.util.Log;
import android.webkit.WebView;

public class GenericXmlRpc extends XmlRpcDownload {
    public GenericXmlRpc(Context context) {
        super(context);
    }


    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        Log.d("Hi", "Done Downloading.");

        String unencodedHtml = "<html><body>" + result + "<ul>";
        if (_isRawResult)
        {
            unencodedHtml = _xmlrpc_results.get(0);
        }
        else {
            for (int i = 0; i < _xmlrpc_results.size(); i++) {
                unencodedHtml += "\n<li>" + _xmlrpc_results.get(i) + "</li>";
            }
            unencodedHtml += "\n</ul></body></html>";
        }
        unencodedHtml = unencodedHtml.replaceAll("href=\"/", "href=\"http://dokuwiki/");
        Log.d(TAG, "unencodedHtml: "+unencodedHtml);
        String encodedHtml = Base64.encodeToString(unencodedHtml.getBytes(), Base64.NO_PADDING);
        WebView myWebView = (WebView) ((MainActivity) _context).findViewById(R.id.webview);
        myWebView.loadData(encodedHtml, "text/html", "base64");

        //menu
        if(_xmlrpc_results.size()>1){
            NavigationView myNavView = (NavigationView) ((MainActivity) _context).findViewById(R.id.nav_view);
            for (int i = 0; i < _xmlrpc_results.size(); i++) {
                String[] parts = _xmlrpc_results.get(i).split(",");
                for (String a : parts) {
                    if(a.startsWith("id=")){
                        myNavView.getMenu().add(a.substring(3));
                    }
                    if(a.startsWith(" id=")){
                        myNavView.getMenu().add(a.substring(4));
                    }
                }
            }
        }
    }
}
