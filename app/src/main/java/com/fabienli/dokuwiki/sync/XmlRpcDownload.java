package com.fabienli.dokuwiki.sync;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;
import com.fabienli.dokuwiki.R;

import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcClientException;
import org.apache.xmlrpc.client.XmlRpcSunHttpTransport;
import org.apache.xmlrpc.client.XmlRpcSunHttpTransportFactory;
import org.apache.xmlrpc.client.XmlRpcTransport;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import androidx.appcompat.app.AppCompatActivity;

public class XmlRpcDownload extends AsyncTask<String, Integer, String> {
    ProgressDialog _dialog;
    Context _context;
    static String TAG = "XmlRpcDownload";
    public ArrayList<String> _xmlrpc_results = new ArrayList<String>();
    protected boolean _isRawResult = false;
    protected String _pagename = "";
    final XmlRpcClient client = new XmlRpcClient();
    final List<String> cookies = new ArrayList<>();


    public XmlRpcDownload(Context context){
        _context = context;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        Log.d(TAG, "Download Commencing");

        _dialog = new ProgressDialog(_context);
        _dialog.setMessage("Downloading Database...");

        String message= "Executing Process";

        SpannableString ss2 =  new SpannableString(message);
        ss2.setSpan(new RelativeSizeSpan(2f), 0, ss2.length(), 0);
        ss2.setSpan(new ForegroundColorSpan(Color.BLACK), 0, ss2.length(), 0);

        _dialog.setMessage(ss2);

        _dialog.setCancelable(false);
        _dialog.show();
    }

    public void retrieveTitle(){
        Log.d(TAG,"GetTitle");
        execute("dokuwiki.getTitle");
    }
    public void retrievePage(String pagename){
        _pagename = pagename;
        Log.d(TAG,"GetPage "+pagename);
        execute("wiki.getPage", pagename);
    }
    public void getPageInfo(String pagename){
        _pagename = pagename;
        Log.d(TAG,"GetPageInfo "+pagename);
        execute("wiki.getPageInfo", pagename);
        //execute("dokuwiki.getTime");
    }
    public void retrievePageText(String pagename){
        _isRawResult = true;
        _pagename = pagename;
        Log.d(TAG,"GetPage Text "+pagename);
        execute("wiki.getPage", pagename);
    }
    public void uploadPageText(String pagename, String textcontent){
        _pagename = pagename;
        Log.d(TAG,"Upload Text for: "+pagename);
        execute("wiki.putPage", pagename, textcontent, "{}");
    }
    public void retrievePageList(){
        Log.d(TAG,"Looking for all pages");
        execute("dokuwiki.getPagelist","","{}");
    }

    @Override
    protected String doInBackground(String... params) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(_context);
        String password = settings.getString("password", "");
        String user = settings.getString("user", "");;
        String urlserver = settings.getString("serverurl", "");;

        try {
            Log.d(TAG,"Connecting to server <"+urlserver + "> with user <"+user+">");
            String methodName = new String(params[0]);
            XmlRpcClientConfigImpl xmlConfig = new XmlRpcClientConfigImpl();
            xmlConfig.setServerURL(new URL(urlserver));
            client.setTransportFactory(getXmlRpcSunHttpTransportFactoryInstance());
            client.setConfig(xmlConfig);

            Log.d(TAG,"2");

            Vector parameters = new Vector();
            for (int i=1; i<params.length; i++) {

                parameters.addElement(params[i]);
            }
            // login first
            if(cookies.size() == 0) {
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

            Log.d(TAG,"3 "+parameters.toString());
            Object result = "";
            try {
                result = client.execute(methodName, parameters);
            } catch (org.apache.xmlrpc.client.XmlRpcClientException exception) {
                Log.e(TAG,"JavaClient: " + exception);
                result = exception.toString();
                if(((String) result).contains("Failed to parse date value ")){
                    int a = ((String) result).indexOf("Failed to parse date value ") + 27;
                    int b = ((String) result).indexOf(" at position ");
                    result = ((String) result).substring(a,b);
                }
            }
            Log.d(TAG,"4");
            Log.d(TAG,"type:" + result.getClass());
            if(result.getClass().isArray())
            {
                Log.d(TAG,"The result is a list: "+ result);
                Object[] aRestIt = ((Object[]) result);
                Log.d(TAG,"The result list: "+ aRestIt);
                for(int i=0; i<aRestIt.length;i++){
                    Log.d(TAG,"The result list#"+i+": "+ aRestIt[i]);
                    _xmlrpc_results.add(aRestIt[i].toString());
                }
                Log.d(TAG,"The result size: "+ _xmlrpc_results.size());
            }
            else {
                Log.d(TAG,"The result is: "+ result);
                _xmlrpc_results.add(result.toString());
            }


        } catch (Exception exception) {
            Log.e(TAG,"JavaClient: " + exception);
        }

        return "Executed!";

    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        Log.d("Hi", "Done Downloading.");
        _dialog.dismiss();
    }


    public XmlRpcSunHttpTransportFactory getXmlRpcSunHttpTransportFactoryInstance()
    {
        return new XmlRpcSunHttpTransportFactory(client) {
            public XmlRpcTransport getTransport() {
                return new XmlRpcSunHttpTransport(client) {

                    private URLConnection conn;

                    @Override
                    protected URLConnection newURLConnection(URL pURL) throws IOException {
                        conn = super.newURLConnection(pURL);
                        return conn;
                    }

                    @Override
                    protected void initHttpHeaders(XmlRpcRequest request) throws XmlRpcClientException {
                        super.initHttpHeaders(request);
                        Log.d(TAG,"initHttpHeaders with cookies size: "+ cookies.size());
                        if (cookies.size() > 0) {
                            StringBuilder commaSep = new StringBuilder();
                            for (String str : cookies) {
                                commaSep.append(str);
                                commaSep.append(";");
                            }
                            setRequestHeader("Cookie", commaSep.toString());
                            //expected cookie format: DokuWiki=1afac8db031462045151b6eb372baeb0;DW7fa065a06cb74c216c145cfbc46ac7d3=ZmFibWVu%7C0%7Cj0DduBEkSf1FLprB3MwmFj75UqlLUx20Nonb4vCgCRI%3D;
                            Log.d(TAG,"Cookies: "+ commaSep.toString());
                        }
                    }

                    @Override
                    protected void close() throws XmlRpcClientException {
                        getCookies(conn);
                    }

                    private void getCookies(URLConnection conn) {
                        if (cookies.size() == 0) {
                            Map<String, List<String>> headers = conn.getHeaderFields();
                            if (// avoid NPE
                                    headers.containsKey(// avoid NPE
                                            "Set-Cookie")) {
                                List<String> vals = headers.get("Set-Cookie");
                                for (String str : vals) {
                                    Log.d(TAG, "Cookie origin: " + str);
                                    String[] cookieslice = str.split(";");
                                    if(cookieslice.length>0){
                                        String cookiemain = cookieslice[0];
                                        if (cookiemain.startsWith("DokuWiki")) {
                                            cookies.add(cookiemain);
                                            Log.d(TAG, "Cookie used: " + cookiemain);
                                        }
                                        else if (cookiemain.endsWith("deleted")) {
                                            Log.d(TAG, "Cookie ignored");
                                        }
                                        else if (cookiemain.startsWith("DW")) {
                                            cookies.add(cookiemain);
                                            Log.d(TAG, "Cookie used: " + cookiemain);
                                        }
                                        else {
                                            Log.d(TAG, "Cookie ignored");
                                        }
                                    }
                                }
                            }
                        }
                    }
                };
            }
        };
    }

}
