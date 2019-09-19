package com.fabienli.dokuwiki.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.fabienli.dokuwiki.R;
import com.fabienli.dokuwiki.tools.Logs;
import com.google.android.material.snackbar.Snackbar;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcSunHttpTransportFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;

public class XmlRpcAdapter {
    protected String TAG = "XmlRpcAdapter";
    final XmlRpcClient client = new XmlRpcClient();
    protected Context _context;
    private String _password;
    private String _user;
    private String _urlserver;
    private boolean _debug = false;
    protected XmlRpcClientConfigImpl _xmlConfig;

    public XmlRpcAdapter(Context context){
        _context = context;
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(_context);
        _password = settings.getString("password", "");
        _user = settings.getString("user", "");;
        _urlserver = settings.getString("serverurl", "");;
        _debug = settings.getBoolean("debuglogs", false);
        Log.d(TAG,"Connecting to server <"+_urlserver + "> with user <"+_user+">");
        _xmlConfig = new XmlRpcClientConfigImpl();
        try {
            _xmlConfig.setServerURL(new URL(_urlserver));
            //_xmlConfig.setUserAgent("Mozilla/5.0 (Android 9; Mobile; rv:67.0) Gecko/67.0 Firefox/67.0");
            client.setConfig(_xmlConfig);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<String> callMethod(String methodName, String... params) {
        ArrayList<String> results = new ArrayList<String>();

        try {
            client.setTransportFactory(getXmlRpcSunHttpTransportFactoryInstance(false, _debug));
            Vector parameters = getParametersVector(params);

            ensureLogin();

            Object result = "";
            try {
                result = client.execute(methodName, parameters);
            } catch (org.apache.xmlrpc.client.XmlRpcClientException exception) {
                Logs.getInstance().add("XmlRpc decode " + methodName+ " response error: " + exception);
                Log.e(TAG,"XmlRpc decode " + methodName+ " response error: " + exception);
                return null;
            }

            Log.d(TAG,"type:" + result.getClass());
            if(result.getClass().isArray())
            {
                //Log.d(TAG,"The result is a list: "+ result);
                Object[] aRestIt = ((Object[]) result);
                //Log.d(TAG,"The result list: "+ aRestIt);
                for(int i=0; i<aRestIt.length;i++){
                    //Log.d(TAG,"The result list#"+i+": "+ aRestIt[i]);
                    results.add(aRestIt[i].toString());
                }
                Log.d(TAG,"The result size: "+ results.size());
            }
            else {
                Log.d(TAG,"The result is: "+ result);
                results.add(result.toString());
            }


        } catch (Exception exception) {
            Log.e(TAG,"XmlRpc call " + methodName+ " error: " + exception);
            Logs.getInstance().add("XmlRpc call " + methodName+ " error: " + exception);
            return null;
        }
        return results;
    }

    public byte[] callMethodBinary(String methodName, String... params) {
        byte[] results = null;

        try {
            client.setTransportFactory(getXmlRpcSunHttpTransportFactoryInstance(true, false));

            Log.d(TAG,"2");

            Vector parameters = getParametersVector(params);

            ensureLogin();

            Log.d(TAG,"3 "+parameters.toString());
            Object result = "";
            try {
                result = client.execute(methodName, parameters);
            } catch (org.apache.xmlrpc.client.XmlRpcClientException exception) {
                Logs.getInstance().add("XmlRpc decode " + methodName+ " response error: " + exception);
                Log.e(TAG,"XmlRpc decode " + methodName+ " response error: " + exception);
                result = exception.toString();
            }
            Log.d(TAG,"4");
            Log.d(TAG,"type:" + result.getClass());
            if(result.getClass().equals(byte[].class)) {
                Log.d(TAG,"The result is byte[]: "+ result);
                results = (byte[]) result;
            }


        } catch (Exception exception) {
            Log.e(TAG,"XmlRpc call " + methodName+ " error: " + exception);
            Logs.getInstance().add("XmlRpc call " + methodName+ " error: " + exception);
        }
        return results;
    }

    protected void ensureLogin() throws XmlRpcException {
        if(CookiesHolder.Instance().cookies.size() == 0) {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(_context);
            String password = settings.getString("password", "");
            String user = settings.getString("user", "");;

            Vector parametersLogin = new Vector();
            parametersLogin.addElement(user);
            parametersLogin.addElement(password);
            Object result = client.execute("dokuwiki.login",parametersLogin);
            Log.d(TAG,"The result login is: "+ result);
            Log.d(TAG,"The cookies size is: "+ CookiesHolder.Instance().cookies.size());
            if(! ((Boolean) result)) {
                View toastView = ((AppCompatActivity) _context).findViewById(R.id.view_content);
                if (toastView == null) {
                    toastView = ((AppCompatActivity) _context).findViewById(R.id.webview);
                }
                Snackbar.make(toastView, "Login error ! please check user/password", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        }
    }

    protected Vector getParametersVector(String... params){
        Vector parameters = new Vector();
        for (int i=0; i<params.length; i++) {
            parameters.addElement(params[i]);
        }
        return parameters;
    }

    public XmlRpcSunHttpTransportFactory getXmlRpcSunHttpTransportFactoryInstance(Boolean isBinaryExpected, Boolean debug)
    {
        return new DwXmlRpcSunHttpTransportFactory(client, isBinaryExpected, debug);
    }
}
