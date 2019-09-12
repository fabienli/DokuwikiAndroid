package com.fabienli.dokuwiki.usecase;

import android.os.AsyncTask;

import com.fabienli.dokuwiki.db.AppDatabase;
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
        SortedSet<String> knownNamespaces = getKnownNamespaces();
        html+="Select namespace:<ul>";
        for(String ns : knownNamespaces){
            html+="<li onclick=\"appendNS('"+ns+"')\">"+ns+"</li>";
        }
        html+="</ul>";
        return html;
    }

    protected SortedSet<String> getKnownNamespaces(){
        SortedSet<String> knownNamespaces = new TreeSet<>();
        for(Page p : _db.pageDao().getAll()){
            int pos = p.pagename.lastIndexOf(":" );
            if(pos != -1 ){
                knownNamespaces.add(p.pagename.substring(0, pos+1));
            }
        }
        return knownNamespaces;
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

    public void getCreatePageHtmlAsync(PageHtmlRetrieveCallback pageHtmlRetrieveCallback) {
        _pageHtmlRetrieveCallback = pageHtmlRetrieveCallback;
        execute();
    }

    @Override
    protected String doInBackground(String... params) {
        _pageContent = getCreatePageHtml();
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
