package cn.imzjw.utils;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author sudojia
 * @version 1.0
 * @description 解析获取 rss 内容
 * @create 2024-05-11 14:12
 * @github https://github.com/sudojia
 * @website https://blog.imzjw.cn
 */
public class ReptileRssTools {
    /**
     * 日志
     */
    private static final Logger LOGGER = Logger.getLogger(ReptileRssTools.class.getName());
    /**
     * 待提交的 URL 文件
     * 里面包含文章链接, 每行一个
     */
    private static final String TXT_FILE_PATH = "urls.txt";

    /**
     * 从指定的 RSS URL 获取内容，并提取其中的 ID，最后将这些 ID 写入文件。
     *
     * @param rssUrl RSS 的 URL 地址，用于获取 RSS 内容。
     */
    public static void getRss(String rssUrl) {
        // 初始化存储ID的列表
        List<String> ids = new ArrayList<>();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new URL(rssUrl).openStream(), StandardCharsets.UTF_8))) {
            // 读取RSS内容并存储到StringBuilder中
            StringBuilder content = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            // 从内容中提取ID并存储到ids列表中
            extractIds(content.toString(), ids);
            // 将提取到的ID写入文件
            writeIdsToFile(ids);
        } catch (Exception e) {
            // 记录获取RSS feed失败的异常信息
            LOGGER.log(Level.WARNING, "获取 RSS feed 失败", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 从文件中读取 URLs。
     * 读取预先定义的文本文件路径（TXT_FILE_PATH）。
     * 文件中每行的非空内容都将被解析为一个 URL，并存储在一个列表中返回。
     *
     * @return List<String> 包含从文件中读取的所有非空 URL 字符串的列表。
     */
    public static List<String> readUrlsFromFile() {
        List<String> urls = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(TXT_FILE_PATH))) {
            String line;
            // 逐行读取文件内容，忽略空行，并将非空行添加到 urls 列表中
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    urls.add(line);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "urls 文件读取失败：", e.getMessage());
        }
        return urls;
    }

    /**
     * 从XML内容中提取id值。
     * 首先解析XML内容，然后使用XPath表达式定位到id元素的文本内容，并将这些内容收集到一个列表中。
     *
     * @param xmlContent 要解析的XML内容，作为字符串提供。
     * @param ids        用于收集提取到的id值的列表。
     * @throws Exception 如果解析XML或执行XPath表达式时发生错误，则抛出异常。
     */
    private static void extractIds(String xmlContent, List<String> ids) throws Exception {
        // 创建一个文档工厂实例，用于构建XML文档解析器
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        // 使用文档工厂创建一个文档构建器
        DocumentBuilder db = dbf.newDocumentBuilder();
        // 将XML内容解析为DOM文档
        Document doc = db.parse(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));

        // 创建XPath工厂，用于生成XPath实例
        XPathFactory xPathFactory = XPathFactory.newInstance();
        // 创建XPath实例，用于评估XPath表达式
        XPath xpath = xPathFactory.newXPath();
        // 编译XPath表达式，用于定位id元素的文本内容
        XPathExpression expr = xpath.compile("//feed/entry/id/text()");
        // 执行XPath表达式并获取匹配的节点列表
        NodeList nodeList = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
        // 遍历节点列表，将id文本添加到收集列表中
        for (int i = 0; i < nodeList.getLength(); i++) {
            ids.add(nodeList.item(i).getNodeValue());
        }
    }

    /**
     * 将一串ID写入到指定的文本文件中。
     * 每个ID占一行。
     *
     * @param ids 包含需要写入文件的所有ID的列表。
     * @throws IOException 在写入文件过程中发生IO异常。
     */
    private static void writeIdsToFile(List<String> ids) throws IOException {
        // 使用BufferedWriter来提高文件写入性能，通过FileWriter指定写入的文件路径
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(TXT_FILE_PATH))) {
            // 遍历ids列表，将每个ID写入文件，如果ID不是最后一个，则在后面添加换行符
            for (int i = 0; i < ids.size(); i++) {
                // 写入当前ID
                writer.write(ids.get(i));
                if (i < ids.size() - 1) {
                    // 如果当前ID不是最后一个，则写入换行符
                    writer.newLine();
                }
            }
        }
    }
}
