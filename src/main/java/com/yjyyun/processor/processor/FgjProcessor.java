package com.yjyyun.processor.processor;

import com.yjyyun.processor.pipeline.CommonPipeline;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Description: introWebMagic
 *
 * @author Soong
 */
public class FgjProcessor implements PageProcessor {

    private static final String HOME_PAGE_REGEX = "http://fgj\\.sh\\.gov\\.cn/gfxwj/index\\w*\\.html";
    private static final String LIST_PAGE_PREFIX = "http://fgj.sh.gov.cn/gfxwj/index_";
    private static final String DETAIL_PAGE_REGEX = "http://fgj.sh.gov.cn/gfxwj/\\d+/\\w+/\\.html";

    //detail page: http://fgj.sh.gov.cn/gfxwj/20201229/1d6e2e228b134bd98cb6502093edcf24.html
    private static final String DETAIL_PAGE_PREFIX = "http://fgj.sh.gov.cn";

    //  <a href="/gfxwj/20200331/2cd7e755c6f44e5f8ac3a29ab18b706e.html"
    private static final String DETAIL_PAGE_SUFFIX_REGEX = "/gfxwj/\\d+/\\w+\\.html";

    //PDF link /cmsres/c9/c9629cbce9ae4b168d28d0d9ee428263/2a40630efeb21a3225c0ac5d0b10d909.pdf
    private static final String PDF_LINK_SUFFIX_REGEX = "/cmsres/\\w+/\\w+/\\w+\\.pdf";
    private static final String PDF_LINK_PREFIX = "http://fgj.sh.gov.cn";

    private static final String DATE_REGEX = "\\d+年\\d+月\\d+日";

    //描述性文件解读页面: http://fgj.sh.gov.cn/zcjd/20201229/fa9c1aed1c2742e1a0713ac53ad2d6f2.html
    private static final String ANALYSIS_PAGE_REGEX = "http://fgj\\.sh\\.gov\\.cn/zcjd/\\d+/\\w+.html";

    //must not be singleton,
    private Boolean firstTime = true;

    private Site site = Site
            .me()
            .setDomain("blog.sina.com.cn")
            .setSleepTime(3000)
            .setUserAgent(
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_2) AppleWebKit/537.31 (KHTML, like Gecko) Chrome/26.0.1410.65 Safari/537.31");


    @Override
    public void process(Page page) {

        if (page.getUrl().regex(HOME_PAGE_REGEX).match()) {
            //step1. access to the list pages

            if (firstTime) {
                //"totalPage: 3,"
                String allPage = page.getHtml().regex("totalPage: \\d+,").get();
                int pageNo = Integer.parseInt(allPage.substring(11, allPage.length() - 1));
                for (int i = 2; i < pageNo + 1; i++) {
                    page.addTargetRequest(LIST_PAGE_PREFIX + i + ".html");
                }
                firstTime = false;
            }
            List<String> detailPagesSuffixList = page.getHtml().xpath("//table[@id='Datatable-1']/tbody/tr/td/a/@href").regex(DETAIL_PAGE_SUFFIX_REGEX).all();
            List<String> detailPagesList = detailPagesSuffixList.stream().map(e -> DETAIL_PAGE_PREFIX + e).collect(Collectors.toList());
            page.addTargetRequests(detailPagesList);


        } else if (page.getUrl().regex(ANALYSIS_PAGE_REGEX).match()) {
            //截取描述性文件解读内容页面 的内容
            List<String> analysisContent = page.getHtml().xpath("//div[@id='ivs_content']/p[@style='text-indent: 2em;']/text()").all();
            List<String> analysisContentTrim = analysisContent.stream().filter(e -> !"".equals(e)).collect(Collectors.toList());
            String content = String.join(",", analysisContentTrim);

            String title = page.getHtml().xpath("//div[@class='Article']//h2[@id='ivs_title']/text()").get();
            String date = page.getHtml().xpath("//div[@class='Article']//h2[@id='ivs_title']//small[@id='ivs_date']/text()").get();
            page.putField(title + "解读内容", content);
            page.putField(title + "发布日期", date);
        } else {
            //step2. access to detail pages
            String date = page.getHtml().xpath("//div[@id='ivs_content']/p[@style='text-indent: 2em; text-align: right;']/text()").regex(DATE_REGEX).get();
            List<String> fileName = page.getHtml().xpath("//div[@id='content']//div[@class='Article']//div[@id='ivs_content']/ol/li/a/text()").all();
            List<String> pdfLink = page.getHtml().xpath("//div[@id='content']//div[@class='Article']//div[@id='ivs_content']/ol/li/a/@href").all();

            if (fileName.size() == pdfLink.size()) {
                for (int i = 0; i < fileName.size(); i++) {
                    if (pdfLink.get(i).contains(".pdf") || pdfLink.get(i).contains(".doc")) {
                        String currentPdfLink = PDF_LINK_PREFIX + pdfLink.get(i);
                        page.putField(fileName.get(i), currentPdfLink);
                        page.putField(fileName.get(i) + "发布日期", date);
                    } else {
                        //规范性文件解读页面
                        page.addTargetRequest(DETAIL_PAGE_PREFIX + pdfLink.get(i));
                    }
                }
            }
        }

    }

    @Override
    public Site getSite() {
        return site;
    }

    public static void main(String[] args) {

        Spider.create(new FgjProcessor())
                .addPipeline(new CommonPipeline())
                .addUrl("http://fgj.sh.gov.cn/gfxwj/index.html")
                .thread(1)
                .run();
    }
}
