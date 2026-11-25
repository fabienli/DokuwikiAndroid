package com.fabienli.dokuwiki.tools;

import android.text.Editable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WikiUtils {
    // name of the default page of a namespace in DokuWiki's default configuration
    public static final String DEFAULT_START_PAGE = "start";

    static public String getPageTitle(String pageId){
        return getPageTitle(pageId, DEFAULT_START_PAGE);
    }

    /**
     * Short, readable title for a page id, to be shown in the toolbar:
     * - "network:vpn_setup" gives "vpn setup"
     * - the start page of a namespace is named after its namespace: "network:start" gives "network"
     * startPageName is the start page's name from the settings; it is used only when it is a
     * plain name, as the setting can also hold a full page id (the page opened at launch)
     */
    static public String getPageTitle(String pageId, String startPageName){
        if(pageId == null)
            return "";
        String id = trimColons(pageId);
        if(id.isEmpty())
            return pageId;
        String startName = startPageName == null ? "" : trimColons(startPageName);
        // a page id with a namespace is the page opened at launch, not DokuWiki's start page name
        if(startName.isEmpty() || startName.contains(":"))
            startName = DEFAULT_START_PAGE;
        String[] parts = id.split(":");
        String name = parts[parts.length-1];
        if(name.equals(startName) && parts.length > 1)
            name = parts[parts.length-2];
        return name.replace('_', ' ');
    }

    static private String trimColons(String s){
        String r = s.trim();
        while(r.startsWith(":"))
            r = r.substring(1);
        while(r.endsWith(":"))
            r = r.substring(0, r.length()-1);
        return r;
    }

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

    static public class EditedText {
        public String text;
        public int cursorPosition;
    }
    static public EditedText autoIndentLists(CharSequence cs, int start, /*int before,*/ int count) {
        EditedText aNewText = new EditedText();
        // enter a new list item or remove the previous one
        if(count==1 && cs.charAt(start)=='\n') {
            // get the previous line
            String[] lines = cs.subSequence(0, start).toString().split("\n");
            if(lines.length > 0){
                //Log.d("keycode","previous line:"+lines[lines.length-1]);
                String previousLine = lines[lines.length-1];
                // check if the line starts with a list char:
                Pattern listPattern = Pattern.compile("^([ ]+)([\\*-]) (.*)");
                Matcher m = listPattern.matcher(previousLine);
                if (m.matches()) {
                    // previous list item is filled; we go on with a new item
                    if(m.group(3).length()>0) {
                        aNewText.text = cs.subSequence(0, start+1).toString();
                        aNewText.text += m.group(1)+m.group(2)+" ";
                        aNewText.text += cs.subSequence(start+1, cs.length());
                        aNewText.cursorPosition = start + m.group(1).length()+3;
                        return aNewText;
                    }
                    // previous list item is empty, so we remove it
                    else {
                        aNewText.text = cs.subSequence(0, start-m.group(1).length()-2).toString();
                        aNewText.text += cs.subSequence(start, cs.length());
                        aNewText.cursorPosition = start - m.group(1).length() - 1;
                        return aNewText;
                    }
                }
            }
        }
        // indent more a list level
        if(count==1 && cs.charAt(start)==' ') {
            // get the current line
            int lineStart = cs.subSequence(0, start).toString().lastIndexOf("\n");
            String currentLine = cs.subSequence(lineStart+1, start+1).toString();
            // check if the line starts with a list char:
            Pattern listPattern = Pattern.compile("^([ ]+)([\\*-])  ");
            Matcher m = listPattern.matcher(currentLine);
            if (m.matches()) {
                aNewText.text = cs.subSequence(0, lineStart+1).toString();
                //aNewText.text = cs.subSequence(start-4, start-1).toString();
                aNewText.text += "  "+m.group(1)+m.group(2)+" ";
                aNewText.text += cs.subSequence(start+1, cs.length());
                aNewText.cursorPosition = start + 2;
                return aNewText;
            }
        }
        // backspace, reduce indent
        if(count==0){
            // get the current line
            int lineStart = cs.subSequence(0, start).toString().lastIndexOf("\n");
            String currentLine = cs.subSequence(lineStart+1, start).toString();
            // check if the line starts with a list char:
            Pattern listPattern = Pattern.compile("^([ ]+)([\\*-])");
            Matcher m = listPattern.matcher(currentLine);
            if (m.matches() && m.group(1).length()>2) {
                aNewText.text = cs.subSequence(0, lineStart+1).toString();
                aNewText.text += m.group(1).substring(2)+m.group(2)+" ";
                aNewText.text += cs.subSequence(start, cs.length());
                aNewText.cursorPosition = start - 1;
                return aNewText;
            }
            else if (m.matches() && m.group(1).length()==2) {
                aNewText.text = cs.subSequence(0, lineStart+1).toString();
                aNewText.text += cs.subSequence(start, cs.length());
                aNewText.cursorPosition = start - 3;
                return aNewText;
            }
        }
        // nothing updated, return null object
        return null;
    }

    public static boolean isUnencryptedUrl(String url) {
        return url != null && url.trim().toLowerCase(java.util.Locale.ROOT).startsWith("http://");
    }
}
