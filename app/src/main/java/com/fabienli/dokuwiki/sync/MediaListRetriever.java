package com.fabienli.dokuwiki.sync;

import android.content.Context;
import android.util.Log;

import com.fabienli.dokuwiki.WikiCacheUiOrchestrator;

import java.util.ArrayList;

public class MediaListRetriever extends XmlRpcDownload {
    static String TAG = "MediaListRetriever";
    WikiCacheUiOrchestrator _wikiMngr;
    Boolean _directDisplay = false;

    public MediaListRetriever(Context context, WikiCacheUiOrchestrator wikiCacheUiOrchestrator, Boolean directDisplay) {
        super(context);
        _wikiMngr = wikiCacheUiOrchestrator;
        _directDisplay = directDisplay;
    }

    public void retrieveMediaList(String namespace){
        _isRawResult = true;
        _pagename = namespace;
        Log.d(TAG,"Get media from <"+namespace+">");
        execute("wiki.getAttachments", namespace);
    }


    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        processXmlRpcResult(_xmlrpc_results);
    }

    protected void processXmlRpcResult(ArrayList<String> results) {
        Log.d(TAG,"received "+results.size() + " media(s)");
        for(String result : results){
            Log.d(TAG,"media: "+result );
        }
        _wikiMngr.updateCacheWithMediaListReceived(results);
        if(_directDisplay)
            // refresh the page
            Log.d(TAG, "TODO: refresh the page");
    }
}
