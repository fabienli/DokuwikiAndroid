package com.fabienli.dokuwiki.usecase;

import org.junit.Test;

public class UrlConverterUnitTest {
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
        UrlConverter urlConverter =  new UrlConverter("/cache/dir/");
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
        UrlConverter urlConverter =  new UrlConverter("/cache/dir/");
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
        UrlConverter urlConverter =  new UrlConverter("/cache/dir/");
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
        UrlConverter urlConverter =  new UrlConverter("/cache/dir/");
        String htmlContent = "<a href=\"/my/wiki/subfolder/doku.php/page1\">page1 link</p>";
        String newContent = urlConverter.getHtmlContentConverted(htmlContent);
        //System.out.println(newContent);
        assert(newContent.contains(UrlConverter.WIKILINKURL+"page1"));
    }

    /**
     * Test the html conversion to local html, image url
     */
    @Test
    public void UrlConverter_htmlConversion_basicimage(){
        UrlConverter urlConverter =  new UrlConverter("/cache/dir/");
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
        UrlConverter urlConverter =  new UrlConverter("/cache/dir/");
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
        UrlConverter urlConverter =  new UrlConverter("/cache/dir/");
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
        UrlConverter urlConverter =  new UrlConverter("/cache/dir/");
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
        UrlConverter urlConverter =  new UrlConverter("/cache/dir/");
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
        UrlConverter urlConverter =  new UrlConverter("/cache/dir/");
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
        UrlConverter urlConverter =  new UrlConverter("/cache/dir/");
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
        UrlConverter urlConverter =  new UrlConverter("/cache/dir/");
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
        UrlConverter urlConverter =  new UrlConverter("/cache/dir/");
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
        UrlConverter urlConverter =  new UrlConverter("/cache/dir/");
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
        UrlConverter urlConverter =  new UrlConverter("/cache/dir/");
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
        UrlConverter urlConverter =  new UrlConverter("/cache/dir/");
        String htmlContent = "<img src=\"/my/server/lib/exe/fetch.php/wiki:private:image3.png?w=250&amp;tok=8712f5&amp;h=300\" />";
        String newContent = urlConverter.getHtmlContentConverted(htmlContent);
        //System.out.println(newContent);
        assert(newContent.contains("<img src=\"/cache/dir//wiki/private/image3.png_250_300\" />"));
    }
}
