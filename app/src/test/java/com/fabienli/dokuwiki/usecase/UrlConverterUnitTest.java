package com.fabienli.dokuwiki.usecase;

import android.util.Log;

import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlConverterUnitTest {
    /**
     * Unit tests run from the app module directory; also accept the repository root
     */
    private static File mainSourceFile(String relativePath){
        File f = new File("src/main/" + relativePath);
        if(!f.exists())
            f = new File("app/src/main/" + relativePath);
        return f;
    }

    /**
     * The default stylesheet must be a file that is really shipped in res/raw,
     * otherwise pages are displayed unstyled until a style is picked in the settings
     */
    @Test
    public void UrlConverter_defaultCss_existsInRawResources(){
        File cssFile = mainSourceFile("res/raw/" + UrlConverter.DEFAULT_CSS_FILE);
        assert(cssFile.isFile());
    }

    /**
     * The default used in code must be the default declared for the "css_style" preference
     */
    @Test
    public void UrlConverter_defaultCss_matchesPreferenceDefault() throws IOException {
        File prefFile = mainSourceFile("res/xml/general_preferences.xml");
        String prefXml = new String(Files.readAllBytes(prefFile.toPath()), StandardCharsets.UTF_8);
        assert(prefXml.contains("app:defaultValue=\"" + UrlConverter.DEFAULT_CSS_FILE + "\""));
    }

    /**
     * When the stylesheet is present in the cache dir, it is embedded in the page
     */
    @Test
    public void UrlConverter_htmlConversion_embedsDefaultCss() throws IOException {
        File cacheDir = Files.createTempDirectory("dokuwiki_css").toFile();
        File cssFile = new File(cacheDir, UrlConverter.DEFAULT_CSS_FILE);
        FileOutputStream fos = new FileOutputStream(cssFile);
        fos.write("body { color:#331; }".getBytes(StandardCharsets.UTF_8));
        fos.close();

        UrlConverter urlConverter = new UrlConverter(cacheDir.getAbsolutePath(), UrlConverter.DEFAULT_CSS_FILE);
        String newContent = urlConverter.getHtmlContentConverted("<p>text</p>");

        assert(newContent.contains("<style>body { color:#331; }</style>"));
        assert(!newContent.contains("rel=\"stylesheet\""));

        cssFile.delete();
        cacheDir.delete();
    }

    /**
     * Test the create page link conversions, simple pagename
     */
    @Test
    public void UrlConverter_createPageUrlTransform_simple(){
        String url = UrlConverter.WIKICREATEURL+"pagename1";
        String newUrl = UrlConverter.getPageName(url);
        assert(newUrl.compareTo("pagename1")==0);
    }

    /**
     * Test the create page link conversions, upper char pagename
     */
    @Test
    public void UrlConverter_createPageUrlTransform_Upper(){
        String url = UrlConverter.WIKICREATEURL+"paGenAme1";
        String newUrl = UrlConverter.getPageName(url);
        assert(newUrl.compareTo("pagename1")==0);
    }

    /**
     * Test the create page link conversions, special char in pagename
     */
    @Test
    public void UrlConverter_createPageUrlTransform_special(){
        String url = UrlConverter.WIKICREATEURL+"P+a#g$e(n]a@m,e.1";
        String newUrl = UrlConverter.getPageName(url);
        assert(newUrl.compareTo("p_a_g_e_n_a_m_e.1")==0);
    }

    /**
     * Test the create page link conversions, consecutive special char in pagename
     */
    @Test
    public void UrlConverter_createPageUrlTransform_consecutiveSpecial(){
        String url = UrlConverter.WIKICREATEURL+"Pagename+#$(_]@,.1";
        String newUrl = UrlConverter.getPageName(url);
        assert(newUrl.compareTo("pagename_.1")==0);
    }

    /**
     * Test the create page link conversions, pagename with namespace
     */
    @Test
    public void UrlConverter_createPageUrlTransform_namesapce(){
        String url = UrlConverter.WIKICREATEURL+"wiki:pagename1";
        String newUrl = UrlConverter.getPageName(url);
        assert(newUrl.compareTo("wiki:pagename1")==0);
    }


    /**
     * Test the file name conversions, file with special char
     */
    @Test
    public void UrlConverter_getFileName_special(){
        String filename = "My_File-name+%1.jpg";
        String newFilename = UrlConverter.getFileName(filename);
        //System.out.println(newFilename);
        assert(newFilename.compareTo("my_file-name_1.jpg")==0);
    }

    /**
     * Test the html conversion to local html
     */
    @Test
    public void UrlConverter_htmlConversion_basicurl(){
        UrlConverter urlConverter =  new UrlConverter("/cache/dir/", "default_css.css");
        String htmlContent = "<a href=\"/doku.php?id=page1\">page1 link</p>";
        String newContent = urlConverter.getHtmlContentConverted(htmlContent);
        //System.out.println(newContent);
        assert(newContent.contains(UrlConverter.WIKILINKURL+"page1"));
    }

    /**
     * Test the html conversion to local html, sub folder un url
     */
    @Test
    public void UrlConverter_htmlConversion_folderurl(){
        UrlConverter urlConverter =  new UrlConverter("/cache/dir/", "default_css.css");
        String htmlContent = "<a href=\"/my/wiki/subfolder/doku.php?id=page1\">page1 link</p>";
        String newContent = urlConverter.getHtmlContentConverted(htmlContent);
        //System.out.println(newContent);
        assert(newContent.contains(UrlConverter.WIKILINKURL+"page1"));
    }

    /**
     * Test the html conversion to local html, nice url
     */
    @Test
    public void UrlConverter_htmlConversion_basicniceurl(){
        UrlConverter urlConverter =  new UrlConverter("/cache/dir/", "default_css.css");
        String htmlContent = "<a href=\"/doku.php/page1\">page1 link</p>";
        String newContent = urlConverter.getHtmlContentConverted(htmlContent);
        //System.out.println(newContent);
        assert(newContent.contains(UrlConverter.WIKILINKURL+"page1"));
    }

    /**
     * Test the html conversion to local html, sub folder un url
     */
    @Test
    public void UrlConverter_htmlConversion_folderniceurl(){
        UrlConverter urlConverter =  new UrlConverter("/cache/dir/", "default_css.css");
        String htmlContent = "<a href=\"/my/wiki/subfolder/doku.php/page1\">page1 link</p>";
        String newContent = urlConverter.getHtmlContentConverted(htmlContent);
        //System.out.println(newContent);
        assert(newContent.contains(UrlConverter.WIKILINKURL+"page1"));
    }

    /**
     * Test the html conversion to local html, adding nice url only
     */
    @Test

    public void UrlConverter_htmlConversion_folderniceurl_onSubdomain(){
        UrlConverter urlConverter =  new UrlConverter("/cache/dir/", "default_css.css");
        String htmlContent = "<a href=\"/somepage1\">page1 link</p>";
        String newContent = urlConverter.getHtmlContentConverted(htmlContent);
        assert(newContent.contains(UrlConverter.WIKILINKURL+"somepage1"));
    }

    /**
     * Test the html conversion to local html, adding nice url with namespaces
     */
    @Test

    public void UrlConverter_htmlConversion_folderniceurl_onSubdomain_withnamespace(){
        UrlConverter urlConverter =  new UrlConverter("/cache/dir/", "default_css.css");
        String htmlContent = "<a href=\"/some:page1:start\">page1 link</p>";
        String newContent = urlConverter.getHtmlContentConverted(htmlContent);
        assert(newContent.contains(UrlConverter.WIKILINKURL+"some:page1:start"));
    }

    /**
     * Test the html conversion to local html, image url
     */
    @Test
    public void UrlConverter_htmlConversion_basicimage(){
        UrlConverter urlConverter =  new UrlConverter("/cache/dir/", "default_css.css");
        String htmlContent = "<img src=\"/lib/exe/fetch.php?media=image.png\" />";
        String newContent = urlConverter.getHtmlContentConverted(htmlContent);
        //System.out.println(newContent);
        assert(newContent.contains("<img src=\"/cache/dir//image.png\" />"));
    }

    /**
     * Test the html conversion to local html, image url with server's folder
     */
    @Test
    public void UrlConverter_htmlConversion_folderimage(){
        UrlConverter urlConverter =  new UrlConverter("/cache/dir/", "default_css.css");
        String htmlContent = "<img src=\"/my/server/lib/exe/fetch.php?media=image2.jpg\" />";
        String newContent = urlConverter.getHtmlContentConverted(htmlContent);
        //System.out.println(newContent);
        assert(newContent.contains("<img src=\"/cache/dir//image2.jpg\" />"));
    }


    /**
     * Test the html conversion to local html, image url in namespace
     */
    @Test
    public void UrlConverter_htmlConversion_imagenamespace(){
        UrlConverter urlConverter =  new UrlConverter("/cache/dir/", "default_css.css");
        String htmlContent = "<img src=\"/my/server/lib/exe/fetch.php?media=wiki:private:image3.png\" />";
        String newContent = urlConverter.getHtmlContentConverted(htmlContent);
        //System.out.println(newContent);
        assert(newContent.contains("<img src=\"/cache/dir//wiki/private/image3.png\" />"));
    }

    /**
     * Test the html conversion to local html, image url with width details
     */
    @Test
    public void UrlConverter_htmlConversion_imagedetailsw(){
        UrlConverter urlConverter =  new UrlConverter("/cache/dir/", "default_css.css");
        String htmlContent = "<img src=\"/my/server/lib/exe/fetch.php?w=200&amp;tok=8712f5&amp;media=wiki:private:image3.png\" />";
        String newContent = urlConverter.getHtmlContentConverted(htmlContent);
        //System.out.println(newContent);
        assert(newContent.contains("<img src=\"/cache/dir//wiki/private/image3.png_200_\" />"));
    }

    /**
     * Test the html conversion to local html, image url with width details
     */
    @Test
    public void UrlConverter_htmlConversion_imagedetailsh(){
        UrlConverter urlConverter =  new UrlConverter("/cache/dir/", "default_css.css");
        String htmlContent = "<img src=\"/my/server/lib/exe/fetch.php?h=300&amp;tok=8712f5&amp;media=wiki:private:image3.png\" />";
        String newContent = urlConverter.getHtmlContentConverted(htmlContent);
        //System.out.println(newContent);
        assert(newContent.contains("<img src=\"/cache/dir//wiki/private/image3.png__300\" />"));
    }

    /**
     * Test the html conversion to local html, image url with width and height details
     */
    @Test
    public void UrlConverter_htmlConversion_imagedetailswh(){
        UrlConverter urlConverter =  new UrlConverter("/cache/dir/", "default_css.css");
        String htmlContent = "<img src=\"/my/server/lib/exe/fetch.php?w=250&amp;tok=8712f5&amp;h=300&amp;media=wiki:private:image3.png\" />";
        String newContent = urlConverter.getHtmlContentConverted(htmlContent);
        //System.out.println(newContent);
        assert(newContent.contains("<img src=\"/cache/dir//wiki/private/image3.png_250_300\" />"));
    }

    /**
     * Test the html conversion to local html, image url
     */
    @Test
    public void UrlConverter_htmlConversion_basicimage_niceurl(){
        UrlConverter urlConverter =  new UrlConverter("/cache/dir/", "default_css.css");
        String htmlContent = "<img src=\"/lib/exe/fetch.php/image.png\" />";
        String newContent = urlConverter.getHtmlContentConverted(htmlContent);
        //System.out.println(newContent);
        assert(newContent.contains("<img src=\"/cache/dir//image.png\" />"));
    }

    /**
     * Test the html conversion to local html, image url with server's folder
     */
    @Test
    public void UrlConverter_htmlConversion_folderimage_niceurl(){
        UrlConverter urlConverter =  new UrlConverter("/cache/dir/", "default_css.css");
        String htmlContent = "<img src=\"/my/server/lib/exe/fetch.php/image2.jpg\" />";
        String newContent = urlConverter.getHtmlContentConverted(htmlContent);
        //System.out.println(newContent);
        assert(newContent.contains("<img src=\"/cache/dir//image2.jpg\" />"));
    }


    /**
     * Test the html conversion to local html, image url in namespace
     */
    @Test
    public void UrlConverter_htmlConversion_imagenamespace_niceurl(){
        UrlConverter urlConverter =  new UrlConverter("/cache/dir/", "default_css.css");
        String htmlContent = "<img src=\"/my/server/lib/exe/fetch.php/wiki:private:image3.png\" />";
        String newContent = urlConverter.getHtmlContentConverted(htmlContent);
        //System.out.println(newContent);
        assert(newContent.contains("<img src=\"/cache/dir//wiki/private/image3.png\" />"));
    }

    /**
     * Test the html conversion to local html, image url with width details
     */
    @Test
    public void UrlConverter_htmlConversion_imagedetailsw_niceurl(){
        UrlConverter urlConverter =  new UrlConverter("/cache/dir/", "default_css.css");
        String htmlContent = "<img src=\"/my/server/lib/exe/fetch.php/wiki:private:image3.png?w=200&amp;tok=8712f5\" />";
        String newContent = urlConverter.getHtmlContentConverted(htmlContent);
        //System.out.println(newContent);
        assert(newContent.contains("<img src=\"/cache/dir//wiki/private/image3.png_200_\" />"));
    }

    /**
     * Test the html conversion to local html, image url with width details
     */
    @Test
    public void UrlConverter_htmlConversion_imagedetailsh_niceurl(){
        UrlConverter urlConverter =  new UrlConverter("/cache/dir/", "default_css.css");
        String htmlContent = "<img src=\"/my/server/lib/exe/fetch.php/wiki:private:image3.png?h=300&amp;tok=8712f5\" />";
        String newContent = urlConverter.getHtmlContentConverted(htmlContent);
        //System.out.println(newContent);
        assert(newContent.contains("<img src=\"/cache/dir//wiki/private/image3.png__300\" />"));
    }

    /**
     * Test the html conversion to local html, image url with width and height details
     */
    @Test
    public void UrlConverter_htmlConversion_imagedetailswh_niceurl(){
        UrlConverter urlConverter =  new UrlConverter("/cache/dir/", "default_css.css");
        String htmlContent = "<img src=\"/my/server/lib/exe/fetch.php/wiki:private:image3.png?w=250&amp;tok=8712f5&amp;h=300\" />";
        String newContent = urlConverter.getHtmlContentConverted(htmlContent);
        //System.out.println(newContent);
        assert(newContent.contains("<img src=\"/cache/dir//wiki/private/image3.png_250_300\" />"));
    }

    /**
     * Test the html conversion to local html, image url rewrite from https://www.dokuwiki.org/rewrite
     */
    @Test
    public void UrlConverter_htmlConversion_imagenamespace_niceurl_rewrite(){
        UrlConverter urlConverter =  new UrlConverter("/cache/dir/", "default_css.css");
        String htmlContent = "<img src=\"/my/server/_media/wiki:private:image3.png\" />";
        String newContent = urlConverter.getHtmlContentConverted(htmlContent);
        System.out.println(newContent);
        assert(newContent.contains("<img src=\"/cache/dir//wiki/private/image3.png\" />"));
    }


    /**
     * Test the html conversion to local html, file dowload link with server's folder
     */
    @Test
    public void UrlConverter_htmlConversion_pdfFile(){
        UrlConverter urlConverter =  new UrlConverter("/cache/dir/", "default_css.css");
        String htmlContent = "<a href=\"/my/server/lib/exe/fetch.php?media=notes:test.pdf\" />";
        String newContent = urlConverter.getHtmlContentConverted(htmlContent);
        //System.out.println("output: "+newContent);
        assert(newContent.contains("<a href=\"file:///cache/dir//notes/test.pdf\" />"));
        //System.out.println(urlConverter._imageList);
        assert(urlConverter._imageList.size() == 1);
        assert(urlConverter._imageList.get(0).imageFilePath.equals("notes/test.pdf"));
    }

    /**
     * Test the html conversion to local html, file dowload link with server's folder and nice urls
     */
    @Test
    public void UrlConverter_htmlConversion_pdfFile_niceurls(){
        UrlConverter urlConverter =  new UrlConverter("/cache/dir/", "default_css.css");
        String htmlContent = "<a href=\"//my/server/lib/exe/fetch.php/notes/test.pdf\" />";
        String newContent = urlConverter.getHtmlContentConverted(htmlContent);
        System.out.println("output: "+newContent);
        assert(newContent.contains("<a href=\"file:///cache/dir//notes/test.pdf\" />"));
    }

    private static final String NICEURL_SERVER = "https://my.server/wiki/lib/exe/xmlrpc.php";

    private static String convertNiceUrl(String serverUrl, String htmlContent){
        UrlConverter urlConverter = new UrlConverter("/cache/dir/", "default_css.css", serverUrl);
        return urlConverter.getHtmlContentConverted(htmlContent);
    }

    /**
     * Nice URLs with the "useslash" option: namespaces are kept and not taken as the wiki path
     */
    @Test
    public void UrlConverter_niceUrl_useslash_keepsNamespaces(){
        String newContent = convertNiceUrl(NICEURL_SERVER, "<a href=\"/wiki/topic/home\" class=\"wikilink1\">home</a>");
        assert(newContent.contains("href=\"" + UrlConverter.WIKILINKURL + "topic:home\""));
    }

    @Test
    public void UrlConverter_niceUrl_colonSeparator(){
        String newContent = convertNiceUrl(NICEURL_SERVER, "<a href=\"/wiki/topic:sub:home\">home</a>");
        assert(newContent.contains("href=\"" + UrlConverter.WIKILINKURL + "topic:sub:home\""));
    }

    @Test
    public void UrlConverter_niceUrl_wikiAtServerRoot(){
        String newContent = convertNiceUrl("https://wiki.xyz.com/lib/exe/xmlrpc.php", "<a href=\"/topic/sub/home\">home</a>");
        assert(newContent.contains("href=\"" + UrlConverter.WIKILINKURL + "topic:sub:home\""));
    }

    /**
     * Rewrite done by DokuWiki itself (userewrite=2): /doku.php/ns/page
     */
    @Test
    public void UrlConverter_niceUrl_dokuPhpRewrite_keepsAnchor(){
        String newContent = convertNiceUrl(NICEURL_SERVER, "<a href=\"/wiki/doku.php/topic/home#section\">home</a>");
        assert(newContent.contains("href=\"" + UrlConverter.WIKILINKURL + "topic:home#section\""));
        assertPageName(UrlConverter.WIKILINKURL + "topic:home#section", "topic:home");
    }

    @Test
    public void UrlConverter_niceUrl_absoluteUrl(){
        String newContent = convertNiceUrl(NICEURL_SERVER, "<a href=\"https://my.server/wiki/topic/home\">home</a>");
        assert(newContent.contains("href=\"" + UrlConverter.WIKILINKURL + "topic:home\""));
    }

    @Test
    public void UrlConverter_niceUrl_externalLinkUnchanged(){
        String newContent = convertNiceUrl(NICEURL_SERVER, "<a href=\"https://other.server/wiki/topic/home\">home</a>");
        assert(newContent.contains("href=\"https://other.server/wiki/topic/home\""));
    }

    @Test
    public void UrlConverter_niceUrl_defaultLinksStillWork(){
        String newContent = convertNiceUrl(NICEURL_SERVER, "<a href=\"/wiki/doku.php?id=topic:home\">home</a>");
        assert(newContent.contains("href=\"" + UrlConverter.WIKILINKURL + "topic:home\""));
    }

    @Test
    public void UrlConverter_niceUrl_useslash_media(){
        UrlConverter urlConverter = new UrlConverter("/cache/dir/", "default_css.css", NICEURL_SERVER);
        String newContent = urlConverter.getHtmlContentConverted("<img src=\"/wiki/_media/topic/image3.png?w=200&amp;tok=8712f5\" />");
        assert(newContent.contains("<img src=\"/cache/dir//topic/image3.png_200_\" />"));
        assert(urlConverter._imageList.size() == 1);
        assert(urlConverter._imageList.get(0).id.equals("topic:image3.png"));
    }

    @Test
    public void UrlConverter_niceUrl_useslash_mediaLink(){
        UrlConverter urlConverter = new UrlConverter("/cache/dir/", "default_css.css", NICEURL_SERVER);
        String newContent = urlConverter.getHtmlContentConverted("<a href=\"/wiki/_media/notes/test.pdf\" class=\"media\">doc</a>");
        assert(newContent.contains("<a href=\"file:///cache/dir//notes/test.pdf\""));
        assert(urlConverter._imageList.size() == 1);
        assert(urlConverter._imageList.get(0).id.equals("notes:test.pdf"));
        assert(urlConverter._imageList.get(0).imageFilePath.equals("notes/test.pdf"));
    }

    @Test
    public void UrlConverter_niceUrl_absoluteRevisionLinkUnchanged(){
        String newContent = convertNiceUrl(NICEURL_SERVER, "<a href=\"https://my.server/wiki/topic/home?rev=123\">old</a>");
        assert(newContent.contains("href=\"https://my.server/wiki/topic/home?rev=123\""));
    }

    @Test
    public void UrlConverter_niceUrl_relativeLinkWithQuery(){
        String newContent = convertNiceUrl(NICEURL_SERVER, "<a href=\"/wiki/topic/home?rev=123\">old</a>");
        assert(newContent.contains("href=\"" + UrlConverter.WIKILINKURL + "topic:home?rev=123\""));
        assertPageName(UrlConverter.WIKILINKURL + "topic:home?rev=123", "topic:home");
    }

    @Test
    public void UrlConverter_niceUrl_mediaLinkOutsideCacheNotDownloaded(){
        UrlConverter urlConverter = new UrlConverter("/cache/dir/", "default_css.css", NICEURL_SERVER);
        urlConverter.getHtmlContentConverted("<a href=\"/wiki/_media/../../files/evil.so\">doc</a>");
        assert(urlConverter._imageList.isEmpty());
    }

    private static void assertPageName(String url, String expected){
        assert(UrlConverter.getPageName(url).equals(expected));
    }
}
