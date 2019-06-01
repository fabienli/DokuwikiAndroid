package com.fabienli.dokuwiki.usecase;

import android.os.AsyncTask;

import com.fabienli.dokuwiki.db.AppDatabase;
import com.fabienli.dokuwiki.db.Page;
import com.fabienli.dokuwiki.usecase.callback.PageHtmlRetrieveCallback;

import java.util.SortedSet;
import java.util.TreeSet;

public class StaticPagesDisplay extends AsyncTask<String, Integer, String> {
    /**
     * aim of this class is to build a static html page to be displayed for various usecase
     */

    protected AppDatabase _db;
    PageHtmlRetrieveCallback _pageHtmlRetrieveCallback = null;
    String _pageContent = "";

    public StaticPagesDisplay(AppDatabase db) {
        _db = db;
    }


    public String getCreatePageHtml(){
        String html = "<script>function appendNS(ns){\n" +
                "      val=document.getElementById('id').value;\n" +
                "      pos=val.lastIndexOf(':')+1;\n" +
                "      document.getElementById('id').value=ns+val.substr(pos);\n" +
                "    }" +
                "</script>" +
                "<form action=\"http://dokuwiki_create/\" method=\"GET\">" +
                "Enter page name: " +
                "<input id=\"id\" name=\"id\"/>" +
                "<input type=\"submit\" value=\"Create\">" +
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
}
