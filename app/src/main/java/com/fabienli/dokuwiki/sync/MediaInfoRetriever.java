package com.fabienli.dokuwiki.sync;

import android.util.Log;

import com.fabienli.dokuwiki.db.Media;

import java.util.ArrayList;

public class MediaInfoRetriever {
    protected String TAG = "MediaInfoRetriever";
    protected XmlRpcAdapter _xmlRpcAdapter = null;

    public MediaInfoRetriever(XmlRpcAdapter xmlRpcAdapter) {
        _xmlRpcAdapter = xmlRpcAdapter;
    }

    public Media retrieveMediaInfo(String mediaId){
        Log.d(TAG,"GetMedia info "+mediaId);
        ArrayList<String> resultList = _xmlRpcAdapter.callMethod("wiki.getAttachmentInfo", mediaId);

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
                else if(a.startsWith("isimg=")){
                    media.isimg = a.substring(6);
                }
                else if(a.startsWith("mtime=")){
                    media.mtime = a.substring(6);
                }
                else if(a.startsWith("lastModified=")){
                    media.lastModified = a.substring(13);
                }
            }
        }
        if(media.id.length() == 0)
            media.id = mediaId;

        return media;
    }
}
