package cn.imzjw;


import cn.imzjw.service.AutoSubmitUrlServiceImpl;
import cn.imzjw.utils.ReptileRssTools;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 重构代码
 *
 * @author sudojia
 * @version 1.0
 * @description 为推送网站到 bing 的 IndexNow 服务
 * @create 2024-05-11 12:55
 * @github https://github.com/sudojia
 * @website https://blog.imzjw.cn
 */
public class AutoSubmitUrlApplications {
    /**
     * 日志
     */
    private static final Logger LOGGER = Logger.getLogger(AutoSubmitUrlApplications.class.getName());
    /**
     * API KEY
     */
    private static final String API_KEY = "apiKey";
    /**
     * 提交数量
     */
    private static final String COUNT = "count";
    /**
     * 环境变量默认值
     */
    private static final String DEFAULT_VALUE = "";

    /**
     * 主函数
     *
     * @param args 参数
     */
    public static void main(String[] args) {
        // 获取环境变量中定义的 Secrets 参数，不填默认为空字符串
        String rssUrl = getEnvOrDefault("RSS_URL");
        String getIndexNowkey = getEnvOrDefault("INDEX_NOW_KEY");
        String getBingApiKey = getEnvOrDefault("BING_KEY");
        String getBaiduApiKey = getEnvOrDefault("BAIDU_KEY");
        String botToken = getEnvOrDefault("BOT_TOKEN");
        String chatId = getEnvOrDefault("CHAT_ID");

        // 解析 Secrets 参数为 Map 对象
        Map<String, Object> indexNowMap = parseSecrets(getIndexNowkey);
        Map<String, Object> bingMap = parseSecrets(getBingApiKey);
        Map<String, Object> baiduMap = parseSecrets(getBaiduApiKey);
        // 从解析后的 Map 中提取 API Key
        String indexNowkey = (String) indexNowMap.get(API_KEY);
        String bingApiKey = (String) bingMap.get(API_KEY);
        String baiduApiKey = (String) baiduMap.get(API_KEY);
        // 验证 RSS URL 的有效性
        if (!isValidRssUrl(rssUrl)) {
            LOGGER.log(Level.SEVERE, "输入的 rssUrl 不合法！");
            return;
        }
        try {
            URL url = new URL(rssUrl);
            String protocol = url.getProtocol();
            String host = url.getHost();
            // 构造站点基础 URL
            String siteUrl = protocol + "://" + host;
            String keyLocation = protocol + "://" + host + "/" + indexNowkey + ".txt";
            // 通过 rss 链接获取文章 id
            ReptileRssTools.getRss(rssUrl);
            // 休眠 2 秒，等待后续处理
            TimeUnit.SECONDS.sleep(2);
            // 提交构造的 urls，包括站点 URL、密钥位置、各 API Key 及提交数量等
            submitUrls(host, siteUrl, indexNowkey, keyLocation, bingApiKey, baiduApiKey, (Integer) indexNowMap.get(COUNT), (Integer) bingMap.get(COUNT), (Integer) baiduMap.get(COUNT));
            // 推送消息
            AutoSubmitUrlServiceImpl.sendTelegramMsg(botToken, chatId);
        } catch (MalformedURLException e) {
            LOGGER.log(Level.SEVERE, "请求 rss 失败", e.getMessage());
        } catch (InterruptedException e) {
            // 恢复中断状态
            Thread.currentThread().interrupt();
            LOGGER.log(Level.WARNING, "线程被中断", e.getMessage());
        }
    }

    /**
     * 向多个搜索引擎提交 urls
     *
     * @param host          主机地址
     * @param siteUrl       网站的URL
     * @param indexNowkey   IndexNow的密钥
     * @param keyLocation   密钥存储位置
     * @param bingApiKey    Bing搜索引擎的API密钥
     * @param baiduApiKey   百度搜索引擎的API密钥
     * @param indexNowCount 向IndexNow提交的URL数量
     * @param bingCount     向Bing提交的URL数量
     * @param baiDuCount    向百度提交的URL数量
     */
    private static void submitUrls(String host, String siteUrl, String indexNowkey, String keyLocation, String bingApiKey, String baiduApiKey, Integer indexNowCount, Integer bingCount, Integer baiDuCount) {
        List<String> urlList;
        try {
            // 从文件中读取 urls 列表
            urlList = ReptileRssTools.readUrlsFromFile();
            if (urlList.isEmpty()) {
                // 如果URL列表为空或读取失败, 取消提交
                LOGGER.log(Level.SEVERE, "URL 列表为空或读取 rss 失败, 取消提交！");
                return;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "读取 URL 列表时发生异常" + e.getMessage());
            return;
        }
        // 创建一个固定大小为 5 的线程池，用于并发提交URL到不同的搜索引擎
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        // 向IndexNow提交URL任务
        executorService.submit(() -> {
            try {
                AutoSubmitUrlServiceImpl.pushIndexNowUrl(urlList, host, indexNowkey, keyLocation, indexNowCount);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "提交 URL 到 IndexNow 时发生异常", e.getMessage());
            }
        });
        // 向Bing提交URL任务
        executorService.submit(() -> {
            try {
                AutoSubmitUrlServiceImpl.pushBingUrl(urlList, siteUrl, bingApiKey, bingCount);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "提交 URL 到 Bing 时发生异常", e.getMessage());
            }
        });
        // 向百度提交URL任务
        executorService.submit(() -> {
            try {
                AutoSubmitUrlServiceImpl.pushBaiduUrl(urlList, siteUrl, baiduApiKey, baiDuCount);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "提交 URL 到 Baidu 时发生异常", e.getMessage());
            }
        });
        // 向Google提交URL任务
        executorService.submit(() -> {
            try {
                AutoSubmitUrlServiceImpl.pushGoogleUrl(urlList);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "提交 URL 到 Google 时发生异常", e.getMessage());
            }
        });
        // 关闭线程池，并等待所有任务完成
        executorService.shutdown();
        try {
            // 长时间等待所有任务完成，以确保所有提交任务执行完毕
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            // 如果在等待线程池关闭时线程被中断时，尝试恢复中断状态
            LOGGER.log(Level.SEVERE, "线程池等待终止时发生异常", e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 解析包含API密钥和可选的提交数量的字符串。
     *
     * @param apiKeyWithCount 一个字符串，格式为 "apiKey,count"
     * @return 一个包含API密钥和数量的 Map 集合
     */
    public static Map<String, Object> parseSecrets(String apiKeyWithCount) {
        Map<String, Object> result = new HashMap<>();
        String[] secretsSplit = apiKeyWithCount.split(",");
        if (secretsSplit.length > 0) {
            // API 密钥
            result.put(API_KEY, secretsSplit[0]);
        }
        if (secretsSplit.length == 2) {
            // 判断是否是数字
            if (secretsSplit[1].matches("\\d+")) {
                try {
                    result.put(COUNT, Integer.parseInt(secretsSplit[1]));
                } catch (NumberFormatException e) {
                    LOGGER.log(Level.WARNING, "转换失败: ", e.getMessage());
                }
            }
        }
        return result;
    }

    /**
     * 从环境变量中获取指定的变量值，如果不存在则返回默认值。
     *
     * @param varName 环境变量名
     * @return 环境变量的值或默认值
     */
    private static String getEnvOrDefault(String varName) {
        String value = System.getenv(varName);
        if (value == null) return DEFAULT_VALUE;
        return value;
    }

    /**
     * 检查提供的RSS URL是否有效。
     * 通过尝试建立与URL的连接并发送HEAD请求来检验URL的有效性。如果连接成功建立且服务器返回HTTP OK（200）响应码，则认为URL有效。
     *
     * @param rssUrl 需要验证的RSS URL字符串。
     * @return 布尔值，如果URL有效则返回true，否则返回false。
     */
    private static boolean isValidRssUrl(String rssUrl) {
        try {
            // 创建URL对象并打开连接
            URL url = new URL(rssUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            // 设置请求方法为HEAD，只获取响应头信息，以减少数据传输量
            connection.setRequestMethod("HEAD");
            // 设置连接超时时间为10秒
            connection.setConnectTimeout(10000);
            // 连接到服务器
            connection.connect();
            // 获取服务器响应码
            int responseCode = connection.getResponseCode();
            // 判断响应码是否为HTTP OK（200），是则认为URL有效
            return (responseCode == HttpURLConnection.HTTP_OK);
        } catch (IOException e) {
            // 若发生异常，则认为URL无效
            return false;
        }
    }
}