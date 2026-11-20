package com.example.search.service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

/**
 * 必应图片搜索聚合：请求必应图片搜索页，从 HTML 里提取图片信息。
 * 这就是原项目"外部数据源接入搜索中台"思路的演示。
 *
 * 注意：爬取方式对页面结构有依赖，必应改版或网络不通时此模块返回空列表，不影响其他搜索。
 */
@Service
public class ImageSearchService {

    private static final Pattern MURL = Pattern.compile("\"murl\":\"(.*?)\"");
    private static final Pattern TURL = Pattern.compile("\"turl\":\"(.*?)\"");
    private static final Pattern TITLE = Pattern.compile("\"t\":\"(.*?)\"");

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public List<Map<String, String>> search(String q, int limit) {
        List<Map<String, String>> result = new ArrayList<>();
        try {
            String url = "https://cn.bing.com/images/search?q="
                    + URLEncoder.encode(q, StandardCharsets.UTF_8) + "&count=40";
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent",
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/126.0 Safari/537.36")
                    .header("Accept-Language", "zh-CN,zh;q=0.9")
                    .GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String html = response.body();
            // 必应把图片元数据的 JSON 做了 HTML 实体转义（&quot;murl&quot;:&quot;...&quot;），
            // 先还原成裸引号/符号，后面的正则才能匹配。
            html = html.replace("&quot;", "\"")
                       .replace("&#34;", "\"")
                       .replace("&#39;", "'")
                       .replace("&amp;", "&");

            Matcher mUrls = MURL.matcher(html);
            Matcher tUrls = TURL.matcher(html);
            Matcher titles = TITLE.matcher(html);

            // 三组匹配逐个消费；同一张图的 m/t/turl 在 HTML 里顺序一致
            List<String> murlList = extractAll(mUrls);
            List<String> turlList = extractAll(tUrls);
            List<String> titleList = extractAll(titles);

            for (int i = 0; i < murlList.size() && result.size() < limit; i++) {
                String murl = unescape(murlList.get(i));
                String turl = i < turlList.size() ? unescape(turlList.get(i)) : murl;
                String title = i < titleList.size() ? unescape(titleList.get(i)) : "";
                if (murl.startsWith("http")) {
                    Map<String, String> item = new LinkedHashMap<>();
                    item.put("url", murl);
                    item.put("thumbnail", turl);
                    item.put("title", title);
                    item.put("source", "bing");
                    result.add(item);
                }
            }
        } catch (IOException | InterruptedException e) {
            // 爬取失败静默降级，不影响聚合搜索的其他数据源
        }
        return result;
    }

    private List<String> extractAll(Matcher m) {
        List<String> list = new ArrayList<>();
        while (m.find()) {
            list.add(m.group(1));
        }
        return list;
    }

    /** 处理 JSON 转义：\u0026 等 */
    private String unescape(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(i + 1);
                if (n == 'u' && i + 5 < s.length()) {
                    sb.append((char) Integer.parseInt(s.substring(i + 2, i + 6), 16));
                    i += 5;
                } else if (n == '/') {
                    sb.append('/');
                    i++;
                } else if (n == '"') {
                    sb.append('"');
                    i++;
                } else {
                    sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
