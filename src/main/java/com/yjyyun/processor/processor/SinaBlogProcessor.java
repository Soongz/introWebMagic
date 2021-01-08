package com.yjyyun.processor.processor;

import com.yjyyun.processor.pipeline.CommonPipeline;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Description: introWebMagic
 *
 * @author Soong
 */
public class SinaBlogProcessor implements PageProcessor {

    private class ArticalContent {
        private String content;
        private String date;
        private String url;

        public ArticalContent() {
        }

        public ArticalContent(String content, String date, String url) {
            this.content = content;
            this.date = date;
            this.url = url;
        }
    }

    public static final String URL_LIST = "http://blog\\.sina\\.com\\.cn/s/articlelist_1487828712_0_\\d+\\.html";

    public static final String URL_POST = "http://blog\\.sina\\.com\\.cn/s/blog_\\w+\\.html";

    public ConcurrentHashMap<String, ArticalContent> articles = new ConcurrentHashMap<>(16);

    private Site site = Site
            .me()
            .setDomain("blog.sina.com.cn")
            .setSleepTime(3000)
            .setUserAgent(
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_2) AppleWebKit/537.31 (KHTML, like Gecko) Chrome/26.0.1410.65 Safari/537.31");

    @Override
    public void process(Page page) {
        //列表页
        if (page.getUrl().regex(URL_LIST).match()) {
            page.addTargetRequests(page.getHtml().xpath("//div[@class=\"articleList\"]").links().regex(URL_POST).all());
            page.addTargetRequests(page.getHtml().links().regex(URL_LIST).all());
            //文章页
        } else {
            String title = page.getHtml().xpath("//div[@class='articalTitle']/h2").toString();
            String content = page.getHtml().xpath("//div[@id='articlebody']//div[@class='articalContent']").toString();
            String date = page.getHtml().xpath("//div[@id='articlebody']//span[@class='time SG_txtc']").regex("\\((.*)\\)").toString();
            page.putField("title", title);
            page.putField("content", content);
            page.putField("date", date);

            ArticalContent articalContent = new ArticalContent(content, date, page.getUrl().toString());

            articles.put(title, articalContent);
        }
    }

    @Override
    public Site getSite() {
        return site;
    }

    public static void main(String[] args) {
        Spider.create(new SinaBlogProcessor())
                .addPipeline(new CommonPipeline())
                .addUrl("http://blog.sina.com.cn/s/articlelist_1487828712_0_1.html")
                .run();
    }
}