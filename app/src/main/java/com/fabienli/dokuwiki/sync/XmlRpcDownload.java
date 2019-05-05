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

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcClientException;
import org.apache.xmlrpc.client.XmlRpcSunHttpTransport;
import org.apache.xmlrpc.client.XmlRpcSunHttpTransportFactory;
import org.apache.xmlrpc.client.XmlRpcTransport;
import org.apache.xmlrpc.common.XmlRpcStreamRequestConfig;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
    public byte[] _xmlrpc_binary_results;
    protected boolean _isRawResult = false;
    protected String _pagename = "";
    final XmlRpcClient client = new XmlRpcClient();

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

        Boolean isBinaryExpected = false;
        try {
            Log.d(TAG,"Connecting to server <"+urlserver + "> with user <"+user+">");
            String methodName = new String(params[0]);
            if(methodName.compareTo("wiki.getAttachment")==0)
                isBinaryExpected = true;
            XmlRpcClientConfigImpl xmlConfig = new XmlRpcClientConfigImpl();
            xmlConfig.setServerURL(new URL(urlserver));
            client.setTransportFactory(getXmlRpcSunHttpTransportFactoryInstance(isBinaryExpected));
            client.setConfig(xmlConfig);

            Log.d(TAG,"2");

            Vector parameters = new Vector();
            for (int i=1; i<params.length; i++) {

                parameters.addElement(params[i]);
            }
            // login first
            if(CookiesHolder.Instance().cookies.size() == 0) {
                Vector parametersLogin = new Vector();
                parametersLogin.addElement(user);
                parametersLogin.addElement(password);
                Object result = client.execute("dokuwiki.login",parametersLogin);
                Log.d(TAG,"The result login is: "+ result);
                Log.d(TAG,"The cookies size is: "+ CookiesHolder.Instance().cookies.size());
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
            }
            Log.d(TAG,"4");
            Log.d(TAG,"type:" + result.getClass());
            if(result.getClass().equals(byte[].class)) {
                Log.d(TAG,"The result is byte[]: "+ result);
                _xmlrpc_binary_results = (byte[]) result;
                //TODO: useless?
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                baos.write((byte[]) result);
                _xmlrpc_results.add(baos.toString());
            }
            else if(result.getClass().isArray())
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


    public XmlRpcSunHttpTransportFactory getXmlRpcSunHttpTransportFactoryInstance(Boolean isBinaryExpected)
    {
        return new DwXmlRpcSunHttpTransportFactory(client, isBinaryExpected);
    }

}
