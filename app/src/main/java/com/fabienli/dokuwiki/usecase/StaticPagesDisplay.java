package com.fabienli.dokuwiki.usecase;

import android.os.AsyncTask;
import android.util.Log;

import com.fabienli.dokuwiki.db.AppDatabase;
import com.fabienli.dokuwiki.db.Media;
import com.fabienli.dokuwiki.db.Page;
import com.fabienli.dokuwiki.usecase.callback.PageHtmlRetrieveCallback;

import java.io.File;
import java.util.SortedSet;
import java.util.TreeSet;

public class StaticPagesDisplay extends AsyncTask<String, Integer, String> {
    /**
     * aim of this class is to build a static html page to be displayed for various usecase
     */

    protected AppDatabase _db;
    PageHtmlRetrieveCallback _pageHtmlRetrieveCallback = null;
    String _pageContent = "";
    String _subfolder = "";

    protected String _mediaLocalDir = "";

    public StaticPagesDisplay(AppDatabase db, String mediaLocalDir) {
        _db = db;
        _mediaLocalDir = mediaLocalDir;
    }


    public String getCreatePageHtml(){
        String html = "<script>function appendNS(ns){\n" +
                "      val=document.getElementById('id').value;\n" +
                "      pos=val.lastIndexOf(':')+1;\n" +
                "      document.getElementById('id').value=ns+val.substr(pos);\n" +
                "    }" +
                "</script>" +
                "<form action=\"http://dokuwiki_create/\" method=\"GET\">" +
                "Enter page name: <br/>" +
                "<input style=\"width:100%\" id=\"id\" name=\"id\"/><br/>" +
                "<input style=\"float:right; height:8%\" type=\"submit\" value=\"Create\">" +
                "</form>";
        SortedSet<String> knownNamespaces = new TreeSet<>();
        for(Page p : _db.pageDao().getAll()){
            int pos = p.pagename.lastIndexOf(":" );
            if(pos != -1 ){
                knownNamespaces.add(p.pagename.substring(0, pos+1));
            }
        }
        html+="Select namespace:<ul>";
        for(String ns : knownNamespaces){
            html+="<li onclick=\"appendNS('"+ns+"')\">"+ns+"</li>";
        }
        html+="</ul>";
        return html;
    }

    public String getProposeCreatePageHtml(String pagename){
        String html =
                "<form action=\"http://dokuwiki_create/\" method=\"GET\">" +
                "Page was not found on your dokuwiki: either there's an error while connecting to the server, or the page doesn't exist. <br/>" +
                "If the page '"+pagename+"' doesn't exist, do you want to create it? <br/>" +
                "<input type=\"hidden\" id=\"id\" name=\"id\" value=\""+pagename+"\"/><br/>" +
                "<input style=\"float:right; height:8%; width:100%\" type=\"submit\" value=\"Create\">" +
                "</form>";
        return html;
    }

    public String getFolderTree(String currentPath){
        File currentFilename = new File(_mediaLocalDir+"/"+currentPath);
        if(currentFilename.isDirectory()){
            String html = "";
            for (String filepath2 : currentFilename.list()) {
                html += getFolderTree(currentPath + "/" + filepath2);
            }
            return html;
        }
        else {
            return "<li>"+currentPath+"<img width=\"50\" src=\""+_mediaLocalDir+"/"+currentPath+"\"/></li>";
        }
    }

    public String getMediaPageHtml(){
        String html = "";

        int subfolderLevel = _subfolder.split("/").length;
        if(_subfolder.length()>0) subfolderLevel++;
        Log.d("StaticPagesDisplay", "MediaManager subfolder : "+ _subfolder);

        // back link to upper folder
        if(_subfolder.length()>0){
            int endPath = _subfolder.lastIndexOf("/");
            if(endPath < 0) endPath=0;
            String parentFolder = _subfolder.substring(0, endPath);
            html += "<form action=\"http://dokuwiki_media_manager/\" method=\"GET\">" +
                    "<input type=\"hidden\" id=\"folder\" name=\"folder\" value=\""+parentFolder+"\"/><br/>" +
                    "<input type=\"submit\" value=\"back\"/>" +
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
                if (f.exists())
                    html += "<tr>" +
                            "<td>" + media.id + "</td>" +
                            "<td><img width=\"70\" src=\"" + _mediaLocalDir + "/" + localFileName + "\"/></td>" +
                            "<td><input type=\"button\" value=\"...\"/></td>" +
                            "</tr>";
                else
                    html += "<tr>" +
                            "<td>" + media.id + "</td>" +
                            "<td>missing image</td>" +
                            "<td><input type=\"button\" value=\"...\"/></td>" +
                            "</tr>";

            }
        }
        html+="</table>";

        // list subfolders
        html+="<table>";
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
            html += "<tr>" +
                    "<td><form action=\"http://dokuwiki_media_manager/\" method=\"GET\">" +
                    "<input type=\"hidden\" id=\"folder\" name=\"folder\" value=\""+folder+"\"/><br/>" +
                    "<input type=\"submit\" value=\""+folder+"\"/>" +
                    "</form></td>" +
                    "</tr>";
        }
        html+="</table>";

        return html;
    }

    public void getCreatePageHtmlAsync(PageHtmlRetrieveCallback pageHtmlRetrieveCallback) {
        _pageHtmlRetrieveCallback = pageHtmlRetrieveCallback;
        execute("getCreatePageHtml");
    }
    public void getMediaPageHtmlAsync(PageHtmlRetrieveCallback pageHtmlRetrieveCallback) {
        _pageHtmlRetrieveCallback = pageHtmlRetrieveCallback;
        execute("getMediaPageHtml");
    }

    @Override
    protected String doInBackground(String... params) {
        if(params.length==1 && params[0].compareTo("getCreatePageHtml")==0)
            _pageContent = getCreatePageHtml();
        if(params.length==1 && params[0].compareTo("getMediaPageHtml")==0)
            _pageContent = getMediaPageHtml();
        return "ok";
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        if(_pageHtmlRetrieveCallback!=null)
            _pageHtmlRetrieveCallback.pageRetrieved(_pageContent);
    }

    public void setSubfolder(String subfolder) {
        _subfolder = subfolder.replace("%2F","/");
    }
}
