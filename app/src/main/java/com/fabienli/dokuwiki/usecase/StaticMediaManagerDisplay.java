package com.fabienli.dokuwiki.usecase;

import android.util.Log;

import com.fabienli.dokuwiki.WikiCacheUiOrchestrator;
import com.fabienli.dokuwiki.db.AppDatabase;
import com.fabienli.dokuwiki.db.Media;
import com.fabienli.dokuwiki.db.SyncAction;
import com.fabienli.dokuwiki.usecase.callback.PageHtmlRetrieveCallback;

import java.io.File;
import java.util.SortedSet;
import java.util.TreeSet;

public class StaticMediaManagerDisplay extends StaticPagesDisplay{
    /**
     * aim of this class is to build a static html page to be displayed for various usecase
     */
    String TAG = "StaticMediaManagerDisplay";
    String _mediaManagerParams = "";


    public StaticMediaManagerDisplay(AppDatabase db, String mediaLocalDir) {
        super(db, mediaLocalDir);
    }

    public void getMediaPageHtmlAsync(PageHtmlRetrieveCallback pageHtmlRetrieveCallback) {
        _pageHtmlRetrieveCallback = pageHtmlRetrieveCallback;
        execute();
    }
    @Override
    protected String doInBackground(String... params) {
        _pageContent = getMediaPageHtml();
        return "ok";
    }

    public String getMediaPageHtml() {
        Log.d(TAG, "_mediaManagerParams: "+_mediaManagerParams);
        // select the action to be executed
        String action = "";
        String mediaName = "";
        String moveFolder = "";
        String[] argsList = _mediaManagerParams.split("&");

        for(String arg : argsList) {
            Log.d(TAG, "arg: " + arg);
            String[] argDuple = arg.split("=");
            String argValue = "";
            if (argDuple.length > 1) {
                argValue = argDuple[1];
            }
            if (argDuple.length > 0) {
                if (argDuple[0].compareTo("action") == 0)
                    action = argValue;
                else if (argDuple[0].compareTo("media") == 0)
                    mediaName = argValue.replace("%3A", ":");
                else if (argDuple[0].compareTo("folder") == 0)
                    setSubfolder(argValue);
                else if (argDuple[0].compareTo("destmove") == 0)
                    moveFolder = argValue.replace("%3A", ":");
            }
        }

        if(action.compareTo("details") == 0){
            return getMediaDetailPage(mediaName);
        }
        else if(action.compareTo("startmove") == 0){
            return getSelectMoveNamespacePageHtml(mediaName);
        }
        else if(action.compareTo("move") == 0){
            return getMoveNamespacePageHtml(mediaName, moveFolder);
        }

        return getSubfolderMediaPageHtml();
    }

    public String getSubfolderMediaPageHtml(){
        String html = "";

        int subfolderLevel = _subfolder.split("/").length;
        if(_subfolder.length()>0) subfolderLevel++;
        Log.d("StaticPagesDisplay", "MediaManager subfolder : "+ _subfolder);

        // back link to upper folder
        if(_subfolder.length()>0){
            int endPath = _subfolder.lastIndexOf("/");
            if(endPath > 0 && endPath == _subfolder.length()-1) {
                _subfolder = _subfolder.substring(0, endPath - 1);
                endPath = _subfolder.lastIndexOf("/", endPath - 1);
            }
            if(endPath < 0) endPath=0;
            String parentFolder = _subfolder.substring(0, endPath);
            html += "<form action=\"http://dokuwiki_media_manager/\" method=\"GET\">" +
                    "<input type=\"hidden\" id=\"folder\" name=\"folder\" value=\""+parentFolder+"\"/><br/>" +
                    "<input type=\"submit\" value=\"< back\"/>" +
                    "</form>";
        }

        // list local media
        html+="<table>";
        for(Media media : _db.mediaDao().getAll()){
            String localFileName = media.id.replace(":", "/");
            int mediaLevel = localFileName.split("/").length;

            //Log.d("StaticPagesDisplay","Media "+localFileName+" "+subfolderLevel+"-"+mediaLevel+"-"+localFileName.startsWith(_subfolder));
            if(subfolderLevel == mediaLevel && localFileName.startsWith(_subfolder)) {
                File f = new File(_mediaLocalDir + "/" + localFileName);
                String a_link = "<a href=\"http://dokuwiki_media_manager/?folder="+_subfolder+"&action=details&media="+media.id+"\">";
                html += "<tr><td>" + a_link;
                if (f.exists() && media.isImage())
                    html += "<img width=\"70\" src=\"" + _mediaLocalDir + "/" + localFileName + "\"/>";

                else
                    html += ".";
                html += "</a></td>";
                html += "<td>" + a_link + media.id + "</a></td>" ;
                html += "<td>" +
                        "<form action=\"http://dokuwiki_media_manager/\" method=\"GET\">" +
                        "<input type=\"hidden\" id=\"folder\" name=\"folder\" value=\""+_subfolder+"\"/>" +
                        "<input type=\"hidden\" id=\"action\" name=\"action\" value=\"details\"/>" +
                        "<input type=\"hidden\" id=\"media\" name=\"media\" value=\""+media.id+"\"/><br/>" +
                        "<input type=\"submit\" value=\"...\"/>" +
                        "</form></td>" +
                        "</tr>\n";

            }
        }
        html+="</table>";

        // list subfolders
        html+="<table style=\"width:100%\">";
        SortedSet<String> subFolders = new TreeSet<>();
        for(Media media : _db.mediaDao().getAll()){
            String[] localFilePath = media.id.split(":");
            String localFileName = media.id.replace(":", "/");
            int mediaLevel = localFilePath.length;
            //Log.d("StaticPagesDisplay", "Folder : "+ localFilePath[0]+mediaLevel+"-"+subfolderLevel+"-"+localFileName.startsWith(_subfolder));

            if(mediaLevel > subfolderLevel && localFileName.startsWith(_subfolder)){
                int i=0;
                String subFolderPath = "";
                while (i<subfolderLevel){
                    subFolderPath+=localFilePath[i]+"/";
                    //Log.d("StaticPagesDisplay", "temp subFolder : "+ subFolderPath);
                    i++;
                }
                //Log.d("StaticPagesDisplay", "New subFolder : "+ subFolderPath.substring(0, subFolderPath.length()-1));

                subFolders.add(subFolderPath.substring(0, subFolderPath.length()-1));
            }
        }
        for(String folder : subFolders){
            html += "<tr style=\"width:100%\">" +
                    "<td style=\"width:100%\"><form action=\"http://dokuwiki_media_manager/\" method=\"GET\">" +
                    "<input type=\"hidden\" id=\"folder\" name=\"folder\" value=\""+folder+"\"/><br/>" +
                    "<input type=\"submit\" value=\""+folder+"\" style=\"width:100%\"/>" +
                    "</form></td>" +
                    "</tr>";
        }
        html+="</table>";

        return html;
    }

    public String getMediaDetailPage(String mediaName){
        String html = "";

        Log.d("StaticPagesDisplay", "MediaManager media details : "+ _subfolder + "/" + mediaName);

        // back link to current folder
        html += "<form action=\"http://dokuwiki_media_manager/\" method=\"GET\">" +
                "<input type=\"hidden\" id=\"folder\" name=\"folder\" value=\""+_subfolder+"\"/><br/>" +
                "<input type=\"submit\" value=\"< back\"/>" +
                "</form>";

        // detail of current media
        html += "<table>";
        for(Media media : _db.mediaDao().getAll()){
            String localFileName = media.id.replace(":", "/");

            //Log.d("StaticPagesDisplay","Media "+mediaName+" <?> "+media.id+" -"+localFileName+"-"+localFileName.startsWith(_subfolder));
            if(mediaName.compareTo(media.id)==0 && localFileName.startsWith(_subfolder)) {
                // ensure the picture is downloaded
                WikiCacheUiOrchestrator.instance().ensureMediaIsDownloaded( // TODO: back design: loop dependency
                        media.id,
                        media.id.replaceAll(":","/"),
                        0, 0);

                File f = new File(_mediaLocalDir + "/" + localFileName);
                html += "<tr><td>folder:</td><td>" + _subfolder + "</td></tr>" +
                        "<tr><td>media id:</td><td>" + media.id + "</td></tr>";

                if (f.exists())
                    html += "<tr><td><a href=\"file://" + _mediaLocalDir + "/" + localFileName + "\">" +
                            "<img width=\"70\" src=\"" + _mediaLocalDir + "/" + localFileName + "\"/>" +
                            "</a></td></tr>";
                else
                    html += "<tr><td>missing image</td></tr>";
                html += "<tr><td><form action=\"http://dokuwiki_media_manager/\" method=\"GET\">" +
                        "<input type=\"hidden\" id=\"folder\" name=\"folder\" value=\""+_subfolder+"\"/>" +
                        "<input type=\"hidden\" id=\"action\" name=\"action\" value=\"startmove\"/>" +
                        "<input type=\"hidden\" id=\"media\" name=\"media\" value=\""+media.id+"\"/>" +
                        "<input type=\"submit\" value=\"move\"/>" +
                        "</form></td>" +
                        "</tr>";
                html += "<tr><td><form action=\"http://dokuwiki_media_manager/\" method=\"GET\">" +
                        "<input type=\"hidden\" id=\"folder\" name=\"folder\" value=\""+_subfolder+"\"/>" +
                        "<input type=\"hidden\" id=\"action\" name=\"action\" value=\"details\"/>" +
                        "<input type=\"hidden\" id=\"media\" name=\"media\" value=\""+media.id+"\"/>" +
                        "<input type=\"submit\" value=\"refresh image\"/>" +
                        "</form></td>" +
                        "</tr>";
            }
        }
        html+="</table>";

        return html;
    }

    public String getSelectMoveNamespacePageHtml(String mediaId){
        String html = "<script>function chooseNS(ns){\n" +
                "      document.getElementById('destmove').value=ns;\n" +
                "    }</script>\n" +
                "<form action=\"http://dokuwiki_media_manager/\" method=\"GET\">" +
                "<input type=\"hidden\" id=\"folder\" name=\"folder\" value=\""+_subfolder+"\"/>" +
                "<input type=\"hidden\" id=\"action\" name=\"action\" value=\"move\"/>" +
                "<input type=\"hidden\" id=\"media\" name=\"media\" value=\""+mediaId+"\"/>"  +
                "<input style=\"width:100%\" id=\"destmove\" name=\"destmove\"/><br/>" +
                "<input style=\"float:right; height:8%\" type=\"submit\" value=\"move\"/>" +
                "</form>";
        SortedSet<String> knownNamespaces = getKnownNamespaces();
        html+="Select namespace:<ul>";
        for(String ns : knownNamespaces){
            html+="<li onclick=\"chooseNS('"+ns+"')\">"+ns+"</li>";
        }
        html+="</ul>";
        return html;
    }
    public String getMoveNamespacePageHtml(String mediaId, String destNamespace){
        String fromFileName = mediaId.replace(":", "/");
        for(Media media : _db.mediaDao().getAll()) {
            if(mediaId.compareTo(media.id)==0) {
                // update local DB
                _db.mediaDao().delete(media);
                if(_subfolder == null || _subfolder.compareTo("")==0) {
                    media.id = (destNamespace + ":" + media.id).replace("::", ":");
                }
                else {
                    media.id = media.id
                            .replace(_subfolder, destNamespace).replace("::", ":");
                }
                String toFileName = media.id.replace(":", "/");
                media.file = toFileName;
                _db.mediaDao().delete(media);
                _db.mediaDao().insertAll(media);

                // move file in correct cache folder
                Log.d(TAG, "new media: "+mediaId+"->"+media.id + " - " + fromFileName+"->"+toFileName);
                File fromFile = new File(_mediaLocalDir + "/" + fromFileName);
                File toFile = new File(_mediaLocalDir + "/" + toFileName);
                fromFile.renameTo(toFile);

                // force update of this media in next synchro
                SyncAction sa = new SyncAction();
                sa.verb = "PUT";
                sa.priority = "1";
                sa.name = media.id;
                sa.rev = "";
                sa.data = _mediaLocalDir + "/" + toFileName;
                Log.d(TAG, "Will sync: "+sa.toText());
                _db.syncActionDao().deleteAll(sa);
                _db.syncActionDao().insertAll(sa);

                SyncAction saDelete = new SyncAction();
                saDelete.verb = "DEL";
                saDelete.priority = "1";
                saDelete.name = mediaId;
                saDelete.rev = "";
                saDelete.data = "";
                Log.d(TAG, "Will sync: "+saDelete.toText());
                _db.syncActionDao().deleteAll(saDelete);
                _db.syncActionDao().insertAll(saDelete);

                break;
            }
        }

        //return getMediaDetailPage(mediaId);
        // redirect to the new folder's page:
        setSubfolder(destNamespace.replace(":","/"));
        return getSubfolderMediaPageHtml();
    }
    public void setMediaManagerParams(String args) {
        _mediaManagerParams = args;
    }
}
