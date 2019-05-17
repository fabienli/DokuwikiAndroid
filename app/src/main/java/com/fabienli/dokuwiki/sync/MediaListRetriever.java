package com.fabienli.dokuwiki.sync;

import android.content.Context;
import android.util.Log;

import com.fabienli.dokuwiki.WikiCacheUiOrchestrator;

import java.util.ArrayList;

public class MediaListRetriever extends XmlRpcDownload {
    static String TAG = "MediaListRetriever";
    WikiCacheUiOrchestrator _wikiMngr;

    public MediaListRetriever(Context context, WikiCacheUiOrchestrator wikiCacheUiOrchestrator) {
        super(context);
        _wikiMngr = wikiCacheUiOrchestrator;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        setDialogMsg("Get list of medias");
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
        _wikiMngr.updateCacheWithMediaListReceived(results);
    }
}
