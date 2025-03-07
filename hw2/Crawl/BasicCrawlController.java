package org.example;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;

public class BasicCrawlController {

    public static void main(String[] args) throws Exception {
        String crawlStorageFolder = "/tmp/crawler4j/";
        String newsWebsite = "nytimes";
        String rootUrl = "https://www.nytimes.com";

        CrawlConfig config = new CrawlConfig();

        // 设置存储文件夹
        config.setCrawlStorageFolder(crawlStorageFolder);

        // 设置礼貌延迟 - 每次请求之间等待时间
        config.setPolitenessDelay(1000);

        // 设置最大爬取深度为16
        config.setMaxDepthOfCrawling(16);

        // 设置最大爬取页面数为20,000
        config.setMaxPagesToFetch(20000);

        // 启用二进制内容爬取（用于PDF和图片等）
        config.setIncludeBinaryContentInCrawling(true);

        // 设置爬虫超时时间
        config.setConnectionTimeout(10000);

        // 设置socket超时时间
        config.setSocketTimeout(10000);

        // 设置是否遵循重定向
        config.setFollowRedirects(true);

        // 设置是否包含HTTPS页面
        config.setIncludeHttpsPages(true);

        // 设置不可恢复爬取
        config.setResumableCrawling(false);

        // 实例化爬虫控制器
        PageFetcher pageFetcher = new PageFetcher(config);
        RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
        RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);
        CrawlController controller = new CrawlController(config, pageFetcher, robotstxtServer);

        // 添加种子URL
        controller.addSeed(rootUrl);

        // 设置爬虫线程数为7
        int numberOfCrawlers = 7;

        // 创建爬虫工厂
        CrawlController.WebCrawlerFactory<BasicCrawler> factory = () -> new BasicCrawler("https://www.nytimes.com");

        // 开始爬取
        controller.start(factory, numberOfCrawlers);

        // 爬取完成后，获取收集的数据
        ConcurrentHashMap<String, Integer> fetchedUrls = BasicCrawler.getFetchedUrls();
        ConcurrentHashMap<String, String[]> visitedUrls = BasicCrawler.getVisitedUrls();
        ConcurrentHashMap<String, Boolean> discoveredUrls = BasicCrawler.getDiscoveredUrls();

        // 写入CSV文件
        writeFetchCsv(fetchedUrls, newsWebsite);
        writeVisitCsv(visitedUrls, newsWebsite);
        writeUrlsCsv(discoveredUrls, newsWebsite);

        // 生成统计报告
        generateReport(fetchedUrls, visitedUrls, discoveredUrls, newsWebsite);
    }

    // 写入fetch_nytimes.csv
    private static void writeFetchCsv(ConcurrentHashMap<String, Integer> fetchedUrls, String newsWebsite) throws IOException {
        try (FileWriter writer = new FileWriter("fetch_" + newsWebsite + ".csv")) {
            writer.write("URL,Status\n");
            for (Map.Entry<String, Integer> entry : fetchedUrls.entrySet()) {
                writer.write(entry.getKey() + "," + entry.getValue() + "\n");
            }
        }
    }

    // 写入visit_nytimes.csv
    private static void writeVisitCsv(ConcurrentHashMap<String, String[]> visitedUrls, String newsWebsite) throws IOException {
        try (FileWriter writer = new FileWriter("visit_" + newsWebsite + ".csv")) {
            writer.write("URL,Size (bytes),Outlinks,Content-Type\n");
            for (Map.Entry<String, String[]> entry : visitedUrls.entrySet()) {
                String[] data = entry.getValue();
                writer.write(entry.getKey() + "," + data[0] + "," + data[1] + "," + data[2] + "\n");
            }
        }
    }

    // 写入urls_nytimes.csv
    private static void writeUrlsCsv(ConcurrentHashMap<String, Boolean> discoveredUrls, String newsWebsite) throws IOException {
        try (FileWriter writer = new FileWriter("urls_" + newsWebsite + ".csv")) {
            writer.write("URL,Location\n");
            for (Map.Entry<String, Boolean> entry : discoveredUrls.entrySet()) {
                writer.write(entry.getKey() + "," + (entry.getValue() ? "OK" : "N_OK") + "\n");
            }
        }
    }

    // 生成统计报告
    private static void generateReport(
            ConcurrentHashMap<String, Integer> fetchedUrls,
            ConcurrentHashMap<String, String[]> visitedUrls,
            ConcurrentHashMap<String, Boolean> discoveredUrls,
            String newsWebsite) throws IOException {

        try (FileWriter writer = new FileWriter("CrawlReport_" + newsWebsite + ".txt")) {
            writer.write("Name: Jianchen Liu\n");
            writer.write("USC ID: 8300382104\n");
            writer.write("News site crawled: nytimes.com\n");
            writer.write("Number of threads: 7\n\n");

            // Fetch 统计
            int fetchesAttempted = fetchedUrls.size();
            int fetchesSucceeded = 0;
            int fetchesFailed = 0;

            // 状态码统计
            Map<Integer, Integer> statusCodes = new HashMap<>();

            for (int statusCode : fetchedUrls.values()) {
                statusCodes.put(statusCode, statusCodes.getOrDefault(statusCode, 0) + 1);

                if (statusCode >= 200 && statusCode < 300) {
                    fetchesSucceeded++;
                } else {
                    fetchesFailed++;
                }
            }

            writer.write("Fetch Statistics\n");
            writer.write("================\n");
            writer.write("# fetches attempted: " + fetchesAttempted + "\n");
            writer.write("# fetches succeeded: " + fetchesSucceeded + "\n");
            writer.write("# fetches failed or aborted: " + fetchesFailed + "\n\n");

            // Outgoing URLs 统计
            int totalUrlsExtracted = 0;
            for (String[] data : visitedUrls.values()) {
                totalUrlsExtracted += Integer.parseInt(data[1]); // 外链数量
            }

            Set<String> uniqueUrls = new HashSet<>(discoveredUrls.keySet());
            int uniqueUrlsCount = uniqueUrls.size();

            int uniqueUrlsWithin = 0;
            int uniqueUrlsOutside = 0;

            for (Map.Entry<String, Boolean> entry : discoveredUrls.entrySet()) {
                if (entry.getValue()) {
                    uniqueUrlsWithin++;
                } else {
                    uniqueUrlsOutside++;
                }
            }

            writer.write("Outgoing URLs:\n");
            writer.write("==============\n");
            writer.write("Total URLs extracted: " + totalUrlsExtracted + "\n");
            writer.write("# unique URLs extracted: " + uniqueUrlsCount + "\n");
            writer.write("# unique URLs within News Site: " + uniqueUrlsWithin + "\n");
            writer.write("# unique URLs outside News Site: " + uniqueUrlsOutside + "\n\n");

            // 状态码分布
            writer.write("Status Codes:\n");
            writer.write("=============\n");
            for (Map.Entry<Integer, Integer> entry : statusCodes.entrySet()) {
                String statusText = "";
                int code = entry.getKey();

                if (code == 200) statusText = "OK";
                else if (code == 301) statusText = "Moved Permanently";
                else if (code == 302) statusText = "Found";
                else if (code == 401) statusText = "Unauthorized";
                else if (code == 403) statusText = "Forbidden";
                else if (code == 404) statusText = "Not Found";
                else if (code == 500) statusText = "Internal Server Error";

                if (!statusText.isEmpty()) {
                    writer.write(code + " " + statusText + ": " + entry.getValue() + "\n");
                } else {
                    writer.write(code + ": " + entry.getValue() + "\n");
                }
            }
            writer.write("\n");

            // 文件大小统计
            int less1K = 0;
            int less10K = 0;
            int less100K = 0;
            int less1M = 0;
            int more1M = 0;

            for (String[] data : visitedUrls.values()) {
                int size = Integer.parseInt(data[0]);

                if (size < 1024) {
                    less1K++;
                } else if (size < 10 * 1024) {
                    less10K++;
                } else if (size < 100 * 1024) {
                    less100K++;
                } else if (size < 1024 * 1024) {
                    less1M++;
                } else {
                    more1M++;
                }
            }

            writer.write("File Sizes:\n");
            writer.write("===========\n");
            writer.write("< 1KB: " + less1K + "\n");
            writer.write("1KB ~ <10KB: " + less10K + "\n");
            writer.write("10KB ~ <100KB: " + less100K + "\n");
            writer.write("100KB ~ <1MB: " + less1M + "\n");
            writer.write(">= 1MB: " + more1M + "\n\n");

            // 内容类型统计
            Map<String, Integer> contentTypes = new HashMap<>();

            for (String[] data : visitedUrls.values()) {
                String contentType = data[2];
                contentTypes.put(contentType, contentTypes.getOrDefault(contentType, 0) + 1);
            }

            writer.write("Content Types:\n");
            writer.write("==============\n");
            for (Map.Entry<String, Integer> entry : contentTypes.entrySet()) {
                writer.write(entry.getKey() + ": " + entry.getValue() + "\n");
            }
        }
    }
}
