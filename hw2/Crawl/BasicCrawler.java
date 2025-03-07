package org.example;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.apache.http.Header;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.BinaryParseData;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;

public class BasicCrawler extends WebCrawler {

    // 允许的文件类型：HTML, doc, pdf 和各种图片格式
    private static final Pattern ALLOWED_EXTENSIONS =
            Pattern.compile(".*(\\.(html|doc|pdf|bmp|gif|jpe?g|png))$");

    // 存储爬取的URL和状态码
    private static ConcurrentHashMap<String, Integer> fetchedUrls = new ConcurrentHashMap<>();

    // 存储成功访问的URL及其元数据
    private static ConcurrentHashMap<String, String[]> visitedUrls = new ConcurrentHashMap<>();

    // 存储所有遇到的URL及其是否属于nytimes域
    private static ConcurrentHashMap<String, Boolean> discoveredUrls = new ConcurrentHashMap<>();

    private final AtomicInteger numSeenImages;
    private final String rootUrl;

    public BasicCrawler(AtomicInteger numSeenImages) {
        this.numSeenImages = numSeenImages;
        this.rootUrl = "https://www.nytimes.com";

    }
    public BasicCrawler(String rootUrl) {
        this.numSeenImages = new AtomicInteger(0);
        this.rootUrl = rootUrl;
    }


    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        String href = url.getURL().toLowerCase();

        // 记录所有发现的URL
        boolean isInDomain = href.startsWith(rootUrl.toLowerCase());
        discoveredUrls.put(url.getURL(), isInDomain);

        // 如果是图片URL，增加计数
        if (ALLOWED_EXTENSIONS.matcher(href).matches() &&
                (href.endsWith(".bmp") || href.endsWith(".gif") ||
                        href.endsWith(".jpg") || href.endsWith(".jpeg") ||
                        href.endsWith(".png"))) {
            numSeenImages.incrementAndGet();
        }

        // 只访问指定域名下的URL
        if (!isInDomain) {
            return false;
        }

        // 检查内容类型
        // 如果URL没有明确的扩展名，我们也允许访问，因为它可能返回我们需要的内容类型
        if (!ALLOWED_EXTENSIONS.matcher(href).matches() && href.contains(".")) {
            return false;
        }

        return true;
    }

    /**
     * 处理已获取的页面
     */
    @Override
    public void visit(Page page) {
        String url = page.getWebURL().getURL();

        // 获取内容类型，去掉编码部分
        String contentType = page.getContentType();
        if (contentType != null && contentType.contains(";")) {
            contentType = contentType.substring(0, contentType.indexOf(";"));
        }

        // 获取页面大小（字节）
        int pageSize = page.getContentData().length;

        // 获取外链数量
        int outLinks = 0;
        if (page.getParseData() instanceof HtmlParseData) {
            HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
            Set<WebURL> links = htmlParseData.getOutgoingUrls();
            outLinks = links.size();
        }

        // 存储访问信息
        visitedUrls.put(url, new String[]{String.valueOf(pageSize), String.valueOf(outLinks), contentType});

        logger.info("Visited: {}", url);
        logger.debug("Content Type: {}", contentType);
        logger.debug("Size: {} bytes", pageSize);
        logger.debug("Outlinks: {}", outLinks);
    }

    /**
     * 当爬虫尝试获取页面时调用
     */
    @Override
    protected void handlePageStatusCode(WebURL webUrl, int statusCode, String statusDescription) {
        String url = webUrl.getURL();

        // 记录所有尝试获取的URL及其状态码
        fetchedUrls.put(url, statusCode);

        logger.debug("Fetched: {} - Status: {}", url, statusCode);
    }


    @Override
    public void onBeforeExit() {
    }

    /**
     * 获取已爬取的URL和状态码
     */
    public static ConcurrentHashMap<String, Integer> getFetchedUrls() {
        return fetchedUrls;
    }

    /**
     * 获取已访问的URL及其元数据
     */
    public static ConcurrentHashMap<String, String[]> getVisitedUrls() {
        return visitedUrls;
    }

    /**
     * 获取所有发现的URL
     */
    public static ConcurrentHashMap<String, Boolean> getDiscoveredUrls() {
        return discoveredUrls;
    }
}
