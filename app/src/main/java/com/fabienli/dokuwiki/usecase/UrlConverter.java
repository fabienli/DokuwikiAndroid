package com.fabienli.dokuwiki.usecase;

import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlConverter {
    protected String TAG = "UrlConverter";
    public static String WIKILINKURL = "http://dokuwiki/doku.php?id=";
    public static String WIKIBASEPATTERN = "(/[-~_:/a-zA-Z0-9]+)";
    public static String WIKILINKPATTERN = "href=\""+WIKIBASEPATTERN+"?/doku.php\\?id=";
    public static String WIKILINKPATTERN_NICEURL = "href=\""+WIKIBASEPATTERN+"?(/doku.php)?/";
    public static String WIKICREATEURL = "http://dokuwiki_create/?id=";
    public static String WIKIMEDIAPATTERN = "src=\""+WIKIBASEPATTERN+"?/lib/exe/fetch.php\\?";
    public static String WIKIMEDIAPATTERN_NICEURL = "src=\""+WIKIBASEPATTERN+"?/(lib/exe/fetch.php|_media)/(\\S+)\"";
    public static String WIKIMEDIALINKPATTERN = "href=\""+WIKIBASEPATTERN+"?/lib/exe/fetch.php\\?[^\"]*media=(\\S+)\"";
    public static String WIKIMEDIALINKPATTERN_NICEURL = "href=\""+WIKIBASEPATTERN+"?/(lib/exe/fetch.php|_media)/(\\S+)\"";
    public static String WIKIMEDIAMANAGERURL = "http://dokuwiki_media_manager/?";
    //TODO: handle link detail to media manager
    // format is: /lib/exe/detail.php?(\\S+)
    // nice url format is: /(lib/exe/detail.php|_detail)/(\\S+)
    protected String _cacheDir;
    public List<ImageRefData> _imageList;
    public List<String> _staticImageList;

    public UrlConverter(String cacheDir){
        _cacheDir = cacheDir;
        _imageList = new ArrayList<>();
        _staticImageList = new ArrayList<>();
    }

    public static boolean isPluginActionOnline(String url) {
        if(url.contains("do=plugin_do"))
            return true;
        return false;
    }

    public String getHtmlContentConverted(String htmlContent){
        String html = htmlContent
                .replaceAll(WIKILINKPATTERN, "href=\""+WIKILINKURL);
        html = html.replaceAll(WIKILINKPATTERN_NICEURL, "href=\""+WIKILINKURL);
        // find the list of media, to ensure they're here
        Pattern mediaPattern = Pattern.compile(WIKIMEDIAPATTERN +"(\\S+)\"");
        Matcher m = mediaPattern.matcher(htmlContent);
        while (m.find()) {
            ImageRefData imageData = new ImageRefData();
            imageData.width = 0;
            imageData.height = 0;
            imageData.id="";
            String[] args = m.group(2).split("&amp;");
            for (String v:args) {
                String[] opt = v.split("=");
                if(opt.length == 2){
                    if(opt[0].compareTo("w") == 0)
                        imageData.width = Integer.parseInt(opt[1]);
                    else if(opt[0].compareTo("h") == 0)
                        imageData.height = Integer.parseInt(opt[1]);
                    else if(opt[0].compareTo("media") == 0)
                        imageData.id = opt[1];
                }
            }
            Log.d(TAG, "Found image: "+imageData.toString());
            if(imageData.id.startsWith("http%3A%2F%2F") || imageData.id.startsWith("https%3A%2F%2F"))
            {
                String mediaUrl = imageData.id.replaceAll("%3A",":").replaceAll("%2F","/");
                html = html.replaceAll(WIKIMEDIAPATTERN + m.group(2) + "\"", "src=\"" + mediaUrl + "\"");
            }
            else {
                // id now contains <namespace:file.ext>
                imageData.imageFilePath = imageData.id.replaceAll(":", "/");
                _imageList.add(imageData);

                String localFilename = getLocalFileName(imageData.imageFilePath, imageData.width, imageData.height);
                html = html.replaceAll(WIKIMEDIAPATTERN + m.group(2) + "\"", "src=\"" + _cacheDir + "/" + localFilename + "\"");
            }
        }

        mediaPattern = Pattern.compile(WIKIMEDIAPATTERN_NICEURL);
        m = mediaPattern.matcher(htmlContent);
        while (m.find()) {
            Log.d(TAG, "Found image nice url: "+m.group(2));
            ImageRefData imageData = new ImageRefData();
            imageData.width = 0;
            imageData.height = 0;
            imageData.id = m.group(3);
            if(m.group(3).contains("?")) {
                String[] mediaargs = m.group(3).split("\\?");
                imageData.id = mediaargs[0];
                String[] args = mediaargs[1].split("&amp;");
                for (String v : args) {
                    String[] opt = v.split("=");
                    if (opt.length == 2) {
                        if (opt[0].compareTo("w") == 0)
                            imageData.width = Integer.parseInt(opt[1]);
                        else if (opt[0].compareTo("h") == 0)
                            imageData.height = Integer.parseInt(opt[1]);
                    }
                }
            }
            Log.d(TAG, "Found image nice url: "+imageData.toString());
            String replacementString = m.group().replace("?","\\?");
            if(imageData.id.startsWith("http%3A%2F%2F") || imageData.id.startsWith("https%3A%2F%2F"))
            {
                String mediaUrl = imageData.id.replaceAll("%3A",":").replaceAll("%2F","/");
                html = html.replaceAll(replacementString, "src=\"" + mediaUrl + "\"");
            }
            else {
                // id now contains <namespace:file.ext>
                imageData.imageFilePath = imageData.id.replaceAll(":", "/");
                _imageList.add(imageData);

                String localFilename = getLocalFileName(imageData.imageFilePath, imageData.width, imageData.height);
                Log.d(TAG, "html avant:"+html);
                Log.d(TAG, "html replace:"+replacementString);
                html = html.replaceAll(replacementString , "src=\"" + _cacheDir + "/" + localFilename + "\"");
                Log.d(TAG, "html apres:"+html);
            }
        }

        // update internal links
        Pattern linkPattern = Pattern.compile(WIKIMEDIALINKPATTERN);
        m = linkPattern.matcher(html);
        while (m.find())
        {
            Log.d(TAG, "Found link: "+m.group(2));
            if(m.group(2).startsWith("http%3A%2F%2F") || m.group(2).startsWith("https%3A%2F%2F"))
            {
                String newUrl = m.group(2).replaceAll("%3A", ":").replaceAll("%2F", "/");
                html = html.replaceAll(WIKIMEDIALINKPATTERN + m.group(2), "href=\"" + newUrl);
            }
            else
            {
                //String newUrl = m.group(2).replaceAll("%3A", ":").replaceAll(":", "/").replaceAll("%2F", "/");
                String localFilename = getLocalFileName(m.group(2).replaceAll("%3A", ":"), 0, 0);
                localFilename = m.group(2).replaceAll("%3A", ":").replaceAll(":", "/").replaceAll("%2F", "/");
                //html = html.replaceAll(WIKIMEDIALINKPATTERN + m.group(2), "href=\"file://" + _cacheDir + "/" + newUrl);
                html = html.replaceAll(WIKIMEDIALINKPATTERN + m.group(2), "href=\"file://" + _cacheDir + "/" + localFilename);
            }
        }

        // internal links with nice URLs:
        linkPattern = Pattern.compile(WIKIMEDIALINKPATTERN_NICEURL);
        m = linkPattern.matcher(html);
        while (m.find()) {
            Log.d(TAG, "Found nice url link: "+m.group(3));
            String replacementString = m.group().replace("?","\\?");
            if(m.group(3).startsWith("http%3A%2F%2F") || m.group(3).startsWith("https%3A%2F%2F"))
            {
                String newUrl = m.group(3).replaceAll("%3A", ":").replaceAll("%2F", "/");
                html = html.replaceAll(replacementString, "href=\"" + newUrl+"\"");
            }
            else
            {
                String localFilename = getLocalFileName(m.group(3).replaceAll("%3A", ":"), 0, 0);
                localFilename = m.group(3).replaceAll("%3A", ":").replaceAll(":", "/").replaceAll("%2F", "/");
                html = html.replaceAll(replacementString, "href=\"file://" + _cacheDir + "/" + localFilename+"\"");
            }

        }

        // update plugin images link
        String PLUGINIMGPATTERN = "src=\""+WIKIBASEPATTERN+"?/lib/plugins/";
        Pattern pluginDoPattern = Pattern.compile(PLUGINIMGPATTERN + "(\\S+)\"");
        m = pluginDoPattern.matcher(html);
        while(m.find())
        {
            Log.d(TAG, "Found plugin image: "+m.group(2));
            String localFilename = m.group(2).replaceAll("%3A", ":").replaceAll("%2F", "/");
            html = html.replaceAll(PLUGINIMGPATTERN + m.group(2), "src=\"" + _cacheDir + "/lib/plugins/" + localFilename + "\"");
            _staticImageList.add("lib/plugins/" + m.group(2));
        }


        html = addHeaders(html);
        return html;
    }


    public static String getLocalFileName(String localPath, int width, int height){
        if(width == 0 && height == 0)
            return localPath;
        else if(width == 0)
            return localPath + "__" +Integer.toString(height);
        else if(height == 0)
            return localPath + "_" +Integer.toString(width)+"_";
        return localPath + "_" + Integer.toString(width)+"_" + Integer.toString(height);
    }

    public static String getPageName(String url){
        String result = "";
        if(isInternalPageLink(url)) {
            result = url.replace(WIKILINKURL, "");
            if (result.contains("#")){
                result = result.substring(0, result.indexOf("#"));
            }
        }
        else if(isCreatePageLink(url)) {
            result = url.replace(WIKICREATEURL, "");
            // convert url characters
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    result = URLDecoder.decode(result, StandardCharsets.UTF_8.name());
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            // replace special characters (all but a-z 0-9 A-Z - . or :) with underscore
            result = result.replaceAll("[^-a-zA-Z0-9:\\.]+","_").toLowerCase();

        }
        else if(isMediaManagerPageLink(url)){
            result = url.replace(WIKIMEDIAMANAGERURL, "");
        }
        else if(isLocalMediaLink(url)){
            Log.d("URL", url);
            result = url.replace("file://", "");
        }
        return result;
    }

    public static String getFileName(String filename){
        return filename.replaceAll("[^-a-zA-Z0-9.]+","_").toLowerCase();
    }

    public static boolean isInternalPageLink(String url){
        return url.startsWith(WIKILINKURL);
    }

    public static boolean isCreatePageLink(String url){
        return url.startsWith(WIKICREATEURL);
    }

    public static boolean isMediaManagerPageLink(String url){
        return url.startsWith(WIKIMEDIAMANAGERURL);
    }


    public static boolean isLocalMediaLink(String url){
        return url.startsWith("file://");
    }

    private String addHeaders(String html) {
        File cssFile = new File(_cacheDir, "default.css");
        final String START_HEADERS = "<html>\n<head>\n<link rel=\"stylesheet\" type=\"text/css\" href=\"" + cssFile.getAbsolutePath() +"\">\n</head>\n<body>\n";
        final String END_HEADERS = "\n</body>\n</html>";
        return START_HEADERS + html + END_HEADERS;
    }

    public class ImageRefData {
        public String id;
        public String imageFilePath;
        public int width = 0;
        public int height = 0;
        public String toString(){
            return id+ " width="+width+" height="+height;
        }
    }
}
