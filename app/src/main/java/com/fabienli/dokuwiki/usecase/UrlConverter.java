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
    public static String WIKILINKPATTERN = "href=\"(/[-:/a-zA-Z0-9]+)?/doku.php\\?id=";
    public static String WIKICREATEURL = "http://dokuwiki_create/?id=";
    public static String WIKICREATEPATTERN = "src=\"(/[-:/a-zA-Z0-9]+)?/lib/exe/fetch.php\\?";
    public static String WIKIMEDIAMANAGERURL = "http://dokuwiki_media_manager/?folder=";
    protected String _cacheDir;
    public List<ImageRefData> _imageList;

    public UrlConverter(String cacheDir){
        _cacheDir = cacheDir;
        _imageList = new ArrayList<>();
    }
    public String getHtmlContentConverted(String htmlContent){
        String html = htmlContent
                .replaceAll(WIKILINKPATTERN, "href=\""+WIKILINKURL);
        // find the list of media, to ensure they're here
        Pattern mediaPattern = Pattern.compile(WIKICREATEPATTERN+"(\\S+)\"");
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
            // id now contains <namespace:file.ext>
            imageData.imageFilePath = imageData.id.replaceAll(":","/");
            _imageList.add(imageData);

            String localFilename = getLocalFileName(imageData.imageFilePath, imageData.width, imageData.height);
            html = html.replaceAll(WIKICREATEPATTERN+m.group(2)+"\"", "src=\""+_cacheDir+"/"+localFilename+"\"");
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
        if(isInternalPageLink(url))
            result = url.replace(WIKILINKURL, "");
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

            // replace special characters (all but a-z 0-9 A-Z - or :) with underscore
            result = result.replaceAll("[^-a-zA-Z0-9:]+","_").toLowerCase();

        }
        else if(isMediaManagerPageLink(url)){
            result = url.replace(WIKIMEDIAMANAGERURL, "");
        }
        else if(isLocalMediaLink(url))
            result = url;
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
        return false;
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
