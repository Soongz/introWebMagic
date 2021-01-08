package com.yjyyun.processor.processor;

import com.jayway.jsonpath.JsonPath;
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
public class EastMoneyProcessor implements PageProcessor {

    /**
     * the detail page url prefix
     */
    private static final String detailUrlPrefix = "http://data.eastmoney.com/report/zw_industry.jshtml?infocode=";

    public static final String URL_LIST = "http://reportapi\\.eastmoney\\.com/report/list";

    public static final String homePage = "http://data\\.eastmoney\\.com/report/industry\\.jshtml";

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

        if (page.getUrl().regex(URL_LIST).match()) {
            String originalResponse = page.getRawText();
            String orJson = originalResponse.substring(17, originalResponse.length() - 1);
            int totalPageNo = JsonPath.parse(orJson).read("$.TotalPage");
            if (firstTime) {
                addListPages(page, totalPageNo);
            }

            List<String> infoCodes = JsonPath.parse(orJson).read("$.data[*].infoCode");
            List<String> targetUrls = infoCodes.stream().map(e -> detailUrlPrefix + e).collect(Collectors.toList());
            //push to the queue
            page.addTargetRequests(targetUrls);
        } else {
            String title = page.getHtml().xpath("//div[@class='c-title']/h1/text()").toString();
            String downloadLink = page.getHtml().xpath("//div[@class='c-infos']//span[@class='to-link']/a[@class='pdf-link']/@href").toString();
            page.putField("title", title);
            page.putField("downloadLink", downloadLink);
        }
        //$.data[*].infoCode
    }

    @Override
    public Site getSite() {
        return site;
    }

    private void addListPages(Page page, int totalPageNo) {
        for (int i = 1; i < totalPageNo + 1; i++) {
            String url = "http://reportapi.eastmoney.com/report/list?cb=datatable6071889&industryCode=451&pageSize=50&industry=*&rating=*&ratingChange=*&beginTime=2019-01-07&endTime=2021-01-07&pageNo="
                    + String.valueOf(i) + "&fields=&qType=1&orgCode=&rcode=&p=" + String.valueOf(i) + "&pageNum=" + String.valueOf(i) + "&_=1610002226861";
            page.addTargetRequest(url);
        }
        firstTime = false;
    }

    public static void main(String[] args) {
//        PageProcessor pageProcessor = new EastMoneyProcessor();
//        Spider spider = Spider.create(pageProcessor);
//        for (int i = 1; i < 3; i++) {
//            String url = "http://reportapi.eastmoney.com/report/list?cb=datatable6071889&industryCode=451&pageSize=50&industry=*&rating=*&ratingChange=*&beginTime=2019-01-07&endTime=2021-01-07&pageNo="
//                    + String.valueOf(i) + "&fields=&qType=1&orgCode=&rcode=&p=" + String.valueOf(i) + "&pageNum=" + String.valueOf(i) + "&_=1610002226861";
//            Request request = new Request(url);
//            request.setMethod(HttpConstant.Method.GET);
//            spider.addRequest(request);
//        }
//        spider.thread(1).run();

        Spider.create(new EastMoneyProcessor())
                .addPipeline(new CommonPipeline())
                .addUrl("http://reportapi.eastmoney.com/report/list?cb=datatable6071889&industryCode=451&pageSize=50&industry=*&rating=*&ratingChange=*&beginTime=2019-01-07&endTime=2021-01-07&pageNo=1&fields=&qType=1&orgCode=&rcode=&p=1&pageNum=1&_=1610002226861")
                .thread(5)
                .run();
    }
}
