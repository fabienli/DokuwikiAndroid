package com.fabienli.dokuwiki.tools;

public class WikiUtils {
    static public String convertBaseUrlToMainWikiUrl(String syncUrl){
        String baseurl = convertBaseUrlToRootUrl(syncUrl);
        // add default main php script
        baseurl += "doku.php?id=";
        return baseurl;
    }


    static public String convertBaseUrlToRootUrl(String syncUrl){
        String baseurl = syncUrl;
        if(!baseurl.startsWith("http"))
            baseurl = "http://" + baseurl; // assumption that server auto-redirect to https if available
        // remove ending php script from url
        while(!baseurl.endsWith("/") && baseurl.length() > 0){
            baseurl = baseurl.substring(0, baseurl.length()-1);
        }
        // if we're in a specific dokuwiki's subolder /lib/exe :
        baseurl = baseurl.replaceAll("/lib/exe/$","/");
        return baseurl;
    }

}
