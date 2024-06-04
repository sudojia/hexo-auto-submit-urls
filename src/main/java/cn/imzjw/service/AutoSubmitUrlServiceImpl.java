package cn.imzjw.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author sudojia
 * @version 1.0
 * @description 提交网站到各大搜索引擎
 * @create 2024-05-11 19:25
 * @github https://github.com/sudojia
 * @website https://blog.imzjw.cn
 */
public class AutoSubmitUrlServiceImpl {
    /**
     * 日志
     */
    private static final Logger LOGGER = Logger.getLogger(AutoSubmitUrlServiceImpl.class.getName());
    /**
     * Bing 新出的 IndexNow API 服务
     */
    private static final String BING_INDEX_NOW_SUBMISSION_API = "https://api.indexnow.org/IndexNow";
    /**
     * Bing 的 URL 提交 API
     */
    private static final String BING_URL_SUBMISSION_API = "https://ssl.bing.com/webmaster/api.svc/json/SubmitUrlbatch";
    /**
     * 百度 的 URL 提交 API
     */
    private static final String BAIDU_URL_SUBMISSION_API = "http://data.zz.baidu.com/urls";
    /**
     * Google Indexing API
     */
    private static final String SCOPES = "https://www.googleapis.com/auth/indexing";
    /**
     * Google Indexing API
     */
    private static final String END_POINT = "https://indexing.googleapis.com/v3/urlNotifications:publish";
    /**
     * POST 请求
     */
    private static final String POST = "POST";
    /**
     * 请求头
     */
    private static final String CONTENT_TYPE = "Content-Type";
    /**
     * 请求体
     */
    private static final String APPLICATION_JSON_UTF_8 = "application/json; charset=utf-8";
    /**
     * google service json 文件
     */
    private static final String GOOGLE_SERVICE_JSON = "google_service.json";
    /**
     * 推送消息
     */
    private static final StringBuffer MSG = new StringBuffer("****************网站提交详情****************\n");

    /**
     * 提交 Bing Index Now 索引。
     *
     * @param urlList       需要提交索引的URL字符串列表，不能为空。
     * @param host          提交索引请求的主机名
     * @param key           用于认证的密钥
     * @param keyLocation   密钥在请求中的位置信息
     * @param indexNowCount 请求中希望提交的URL数量，如果为null，则默认提交全部。该数量不能超过urlList的实际大小。
     */
    public static void pushIndexNowUrl(List<String> urlList, String host, String key, String keyLocation, Integer indexNowCount) {
        if (Objects.equals(key, "")) {
            LOGGER.log(Level.WARNING, "未填入 INDEX_NOW_KEY 变量, 取消提交");
            return;
        }
        int countToSubmit = (indexNowCount == null) ? urlList.size() : Math.min(indexNowCount, urlList.size());
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("host", host);
        jsonObject.put("key", key);
        jsonObject.put("keyLocation", keyLocation);
        sendPost(BING_INDEX_NOW_SUBMISSION_API, forUrlAddJson(jsonObject, new JSONArray(), countToSubmit, urlList).toString(), countToSubmit);
    }

    /**
     * 向Bing提交URL列表进行索引。
     *
     * @param urlList   需要提交给Bing索引的URL字符串列表。
     * @param siteUrl   网站的根URL，提交时会作为站点标识。
     * @param bing_key  BING_KEY，用于API调用的身份验证。
     * @param bingCount 向Bing提交的URL数量上限。如果为null，则提交urlList的全部内容。
     */
    public static void pushBingUrl(List<String> urlList, String siteUrl, String bing_key, Integer bingCount) {
        if (Objects.equals(bing_key, "")) {
            LOGGER.log(Level.WARNING, "未填入 BINE_KEY 变量, 取消提交");
            return;
        }
        // 如果 bingCount 为 null，则使用 urlList 的长度，否则使用 bingCount 的值
        int countToSubmit = (bingCount == null) ? urlList.size() : Math.min(bingCount, urlList.size());
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("siteUrl", siteUrl);
        sendPost(BING_URL_SUBMISSION_API + "?apikey=" + bing_key, forUrlAddJson(jsonObject, new JSONArray(), countToSubmit, urlList).toString(), countToSubmit);
    }

    /**
     * 向百度推送网址的函数。
     *
     * @param urlList    需要推送的网址列表。
     * @param siteUrl    站点的URL，是百度API需要的参数。
     * @param token      用于百度API认证的令牌。
     * @param baiDuCount 向百度推送的URL数量限制。如果为null提交全部，默认为10。
     */
    public static void pushBaiduUrl(List<String> urlList, String siteUrl, String token, Integer baiDuCount) {
        if (Objects.equals(token, "")) {
            LOGGER.log(Level.WARNING, "未填入 BAIDU_KEY 变量, 取消提交");
            return;
        }
        // 百度配额有点特殊，所以这里限制了每次提交不超过 10 条
        // 如果配额有很多的，可以自行添加 Secrets 变量：BAIDU_COUNT
        int countToSubmit = (baiDuCount == null) ? 10 : Math.min(baiDuCount, urlList.size());
        StringBuilder postData = new StringBuilder();
        // 构建提交数据的字符串
        for (int i = 0; i < countToSubmit; i++) {
            postData.append(urlList.get(i)).append("\n");
        }
        sendPost(BAIDU_URL_SUBMISSION_API + "?site=" + siteUrl + "&token=" + token, postData.toString(), countToSubmit);
    }

    /**
     * 将一组URL推送到Google索引API。
     *
     * @param urlList 需要推送的URL列表。
     */
    public static void pushGoogleUrl(List<String> urlList) {
        try {
            // 判断文件是否存在
            if (!new File(GOOGLE_SERVICE_JSON).exists()) {
                LOGGER.log(Level.WARNING, "未找到 google_service.json 文件, 取消提交");
                return;
            }
            // 设置HTTP传输和JSON工厂
            HttpTransport httpTransport = new NetHttpTransport();
            JsonFactory jsonFactory = new JacksonFactory();
            // 读取服务账户的认证文件
            FileInputStream in = new FileInputStream(GOOGLE_SERVICE_JSON);
            GoogleCredential credentials = GoogleCredential.fromStream(in, httpTransport, jsonFactory).createScoped(Collections.singleton(SCOPES));
            // 设置API的请求地址
            GenericUrl genericUrl = new GenericUrl(END_POINT);
            // 创建请求工厂
            HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
            // 构建URL更新的请求体
            String content = null;
            for (String url : urlList) {
                content = "{"
                        + "\"url\": \"" + url + "\","
                        + "\"type\": \"URL_UPDATED\","
                        + "}";
            }
            // 创建POST请求，并设置请求体和认证信息
            HttpRequest request = requestFactory.buildPostRequest(genericUrl, ByteArrayContent.fromString(APPLICATION_JSON_UTF_8.split(";")[0], content));
            credentials.initialize(request);
            // 执行请求并获取响应
            HttpResponse response = request.execute();
            int statusCode = response.getStatusCode();
            // 根据响应状态码记录日志
            if (HttpURLConnection.HTTP_OK == statusCode) {
                LOGGER.log(Level.INFO, "Google Index Api 提交成功");
                MSG.append("Google Index Api 提交成功✅\n");
            } else {
                LOGGER.log(Level.SEVERE, "Google Index Api 提交失败，状态码：" + statusCode);
            }
        } catch (Exception e) {
            // 记录异常信息
            LOGGER.log(Level.SEVERE, "Google Index Api 请求失败", e.getMessage());
        }
    }

    /**
     * 向指定API发送POST请求。
     *
     * @param apiUrl        API的URL地址。
     * @param payload       请求体载荷，以JSON格式提供。
     * @param countToSubmit 需要提交的网站数量。
     */
    private static void sendPost(String apiUrl, String payload, Integer countToSubmit) {
        HttpURLConnection connection = null;
        try {
            // 创建URL对象并打开连接
            URL url = new URL(apiUrl);
            connection = (HttpURLConnection) url.openConnection();
            // 设置请求方法为POST，并指定请求头信息
            connection.setRequestMethod(POST);
            connection.setRequestProperty(CONTENT_TYPE, APPLICATION_JSON_UTF_8);
            // 允许输出请求体
            connection.setDoOutput(true);
            // 写入请求体内容
            try (OutputStream os = connection.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }
            // 获取并处理响应
            int responseCode = connection.getResponseCode();
            if (HttpURLConnection.HTTP_OK == responseCode) {
                // 请求成功
                if (url.getHost().equals("api.telegram.org")) {
                    LOGGER.log(Level.INFO, "Telegram 推送成功");
                } else {
                    MSG.append(url.getHost()).append(" 提交成功, 共提交 ").append(countToSubmit).append(" 条✅\n");
                    LOGGER.log(Level.INFO, url.getHost() + " 提交成功, 共提交 " + countToSubmit + " 条");
                }
            } else if (HttpURLConnection.HTTP_BAD_REQUEST == responseCode) {
                // 请求失败：配额已上限
                MSG.append(url.getHost()).append(" 提交配额已上限❌\n");
                LOGGER.log(Level.WARNING, url.getHost() + " 提交配额已上限");
            } else {
                // 其他错误情况
                LOGGER.log(Level.WARNING, url.getHost() + " 提交失败，可能未填变量, 状态码: " + responseCode);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "请求失败，异常信息：" + e.getMessage());
        } finally {
            // 断开连接，确保资源被释放
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 发送 Telegram 消息
     *
     * @param botToken 机器人 token
     * @param chatId   用户 id
     */
    public static void sendTelegramMsg(String botToken, String chatId) {
        try {
            String text = URLEncoder.encode(MSG.toString(), StandardCharsets.UTF_8.toString());
            String url = "https://api.telegram.org/bot" + botToken + "/sendMessage?chat_id=" + chatId + "&text=" + text;
            sendPost(url, "", null);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Telegram 推送失败", e.getMessage());
        }
    }

    /**
     * 将指定数量的URL添加到JSON数组中，并将更新后的数组添加到JSON对象中。
     *
     * @param payload       原始的JSON对象，将在此对象中添加一个新的"urlList"字段。
     * @param jsonArray     用于存储URL的JSON数组。
     * @param countToSubmit 需要提交的URL数量。
     * @param urlList       包含URL字符串的列表。
     * @return 更新后的JSON对象，包含了一个新的名为"urlList"的字段，该字段存储了所有提交的URL。
     */
    private static JSONObject forUrlAddJson(JSONObject payload, JSONArray jsonArray, int countToSubmit, List<String> urlList) {
        // 循环将指定数量的URL添加到JSON数组中
        for (int i = 0; i < countToSubmit; i++) {
            jsonArray.put(urlList.get(i));
        }
        // 将更新后的JSON数组添加到JSON对象中，并返回更新后的JSON对象
        return payload.put("urlList", jsonArray);
    }
}
