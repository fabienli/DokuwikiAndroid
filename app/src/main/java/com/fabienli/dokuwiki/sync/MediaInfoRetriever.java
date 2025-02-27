package com.fabienli.dokuwiki.sync;

import android.util.Log;

import com.fabienli.dokuwiki.db.Media;

import java.util.ArrayList;

public class MediaInfoRetriever {
    protected String TAG = "MediaInfoRetriever";
    protected XmlRpcAdapter _xmlRpcAdapter;

    public MediaInfoRetriever(XmlRpcAdapter xmlRpcAdapter) {
        _xmlRpcAdapter = xmlRpcAdapter;
    }

    public Media retrieveMediaInfo(String mediaId){
        Log.d(TAG,"GetMedia info "+mediaId);
        ArrayList<String> resultList;
        if (true || _xmlRpcAdapter.useOldApi()) {
            resultList = _xmlRpcAdapter.callMethod("wiki.getAttachmentInfo", mediaId);
        }
        else { //TODO fix before using: not 'mediaid' but 'file' -> e.g. wiki:image.png
            resultList = _xmlRpcAdapter.callMethod("core.getMediaInfo", mediaId);
        }

        if(resultList == null)
            resultList = new ArrayList<>();

        // WARNING, wiki.getAttachmentInfo returns only size and lastModified data.

        Media media = new Media();
        for (String r : resultList) {
            String pageinfo = r.replace("{", "").replace("}", "").replace(", ", ",");
            String[] parts = pageinfo.split(",");

            for (String a : parts) {
                if(a.startsWith("id=")){
                    media.id = a.substring(3);
                }
                else if(a.startsWith("file=")){
                    media.file = a.substring(5);
                }
                else if(a.startsWith("size=")){
                    media.size = a.substring(5);
                }
                else if(a.startsWith("isimg=")){ // deprecated
                    media.isimg = a.substring(6);
                }
                else if(a.startsWith("isimage=")){
                    media.isimg = a.substring(8);
                }
                else if(a.startsWith("mtime=")){ // deprecated
                    media.mtime = a.substring(6);
                }
                else if(a.startsWith("lastModified=")){ // deprecated
                    media.lastModified = a.substring(13);
                }
                else if(a.startsWith("revision=")){
                    media.lastModified = a.substring(9);
                }
            }
        }
        if((media.id == null) || (media.id.length() == 0))
            media.id = mediaId;

        return media;
    }
}
