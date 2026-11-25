package com.fabienli.dokuwiki.tools;

import android.text.Editable;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WikiUtils {
    private static final Pattern HEADING_PATTERN =
            Pattern.compile("<h[1-6]\\b[^>]*>(.*?)</h[1-6]>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");
    private static final Pattern ENTITY_PATTERN = Pattern.compile("&(?:#([xX]?)([0-9a-fA-F]+)|([a-zA-Z]+));");
    // DokuWiki writes most characters as UTF-8; these are the named references it or plugins may still emit
    private static final Map<String, String> NAMED_ENTITIES = new HashMap<>();
    static {
        String[][] entities = {
                {"amp", "&"}, {"lt", "<"}, {"gt", ">"}, {"quot", "\""}, {"apos", "'"}, {"nbsp", " "},
                {"copy", "\u00A9"}, {"reg", "\u00AE"}, {"trade", "\u2122"}, {"deg", "\u00B0"},
                {"middot", "\u00B7"}, {"times", "\u00D7"}, {"divide", "\u00F7"}, {"euro", "\u20AC"},
                {"ndash", "\u2013"}, {"mdash", "\u2014"}, {"hellip", "\u2026"},
                {"lsquo", "\u2018"}, {"rsquo", "\u2019"}, {"sbquo", "\u201A"},
                {"ldquo", "\u201C"}, {"rdquo", "\u201D"}, {"bdquo", "\u201E"},
                {"laquo", "\u00AB"}, {"raquo", "\u00BB"},
                {"larr", "\u2190"}, {"uarr", "\u2191"}, {"rarr", "\u2192"}, {"darr", "\u2193"}, {"harr", "\u2194"},
                {"lArr", "\u21D0"}, {"rArr", "\u21D2"}, {"hArr", "\u21D4"},
        };
        for(String[] e : entities)
            NAMED_ENTITIES.put(e[0], e[1]);
    }

    /**
     * Title of a page, as DokuWiki defines it: the text of its first heading.
     * Returns null when the page html has no heading.
     */
    static public String getPageTitle(String pageHtml){
        if(pageHtml == null)
            return null;
        Matcher m = HEADING_PATTERN.matcher(pageHtml);
        if(!m.find())
            return null;
        String title = HTML_TAG_PATTERN.matcher(m.group(1)).replaceAll("");
        title = decodeHtmlEntities(title).replaceAll("\\s+", " ").trim();
        return title.isEmpty() ? null : title;
    }

    /**
     * Decodes numeric and common named character references in one pass,
     * so an escaped reference like "&amp;lt;" gives "&lt;" and not "<"
     */
    static String decodeHtmlEntities(String text){
        Matcher m = ENTITY_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while(m.find()){
            String decoded = m.group(0);
            if(m.group(3) != null){
                String named = NAMED_ENTITIES.get(m.group(3));
                if(named != null)
                    decoded = named;
            }
            else {
                boolean isHex = !m.group(1).isEmpty();
                String digits = m.group(2);
                if(isHex || digits.matches("[0-9]+")){
                    int codePoint;
                    try {
                        codePoint = Integer.parseInt(digits, isHex ? 16 : 10);
                    } catch (NumberFormatException e) {
                        codePoint = -1;
                    }
                    if(!Character.isValidCodePoint(codePoint))
                        codePoint = 0xFFFD;
                    decoded = new String(Character.toChars(codePoint));
                }
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(decoded));
        }
        m.appendTail(sb);
        return sb.toString();
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
