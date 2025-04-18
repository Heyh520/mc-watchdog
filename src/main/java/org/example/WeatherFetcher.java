package org.example;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

public class WeatherFetcher {
    private static final String API_KEY = "506c2e5713fd460ebe6ea7d087a7089a"; // 👈 替换成你自己的 KEY
    private static final String API_HOST = "https://na487qfrmv.re.qweatherapi.com"; // 👈 控制台中查看

    // 获取支持 GZIP 的 InputStreamReader
    private static BufferedReader getReader(HttpURLConnection conn) throws IOException {
        InputStream inputStream = conn.getInputStream();
        String encoding = conn.getContentEncoding();
        if ("gzip".equalsIgnoreCase(encoding)) {
            inputStream = new GZIPInputStream(inputStream);
        }
        return new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
    }

    /**
     * 根据城市名查询城市 ID
     */
    public static String getCityId(String cityName) {
        try {
            String encodedCity = URLEncoder.encode(cityName, "UTF-8");
            String apiUrl = API_HOST + "/geo/v2/city/lookup?location=" + encodedCity;

            HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept-Encoding", "gzip");
            conn.setRequestProperty("X-QW-Api-Key", API_KEY); // 认证方式

            try (BufferedReader reader = getReader(conn)) {
                String json = reader.lines().collect(Collectors.joining());
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();

                if (!obj.get("code").getAsString().equals("200")) {
                    System.err.println("[天气] 城市查询失败: " + obj.get("code").getAsString());
                    return null;
                }

                return obj.getAsJsonArray("location")
                        .get(0).getAsJsonObject()
                        .get("id").getAsString();
            }
        } catch (Exception e) {
            System.err.println("[天气] 城市ID获取失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 根据城市 ID 获取实时天气信息
     */
    public static String getWeatherInfo(String cityId) {
        try {
            String apiUrl = API_HOST + "/v7/weather/now?location=" + cityId + "&lang=zh";

            HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept-Encoding", "gzip");
            conn.setRequestProperty("X-QW-Api-Key", API_KEY); // 认证方式

            try (BufferedReader reader = getReader(conn)) {
                String json = reader.lines().collect(Collectors.joining());
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();

                if (!obj.get("code").getAsString().equals("200")) {
                    System.err.println("[天气] 天气查询失败: " + obj.get("code").getAsString());
                    return "获取天气失败";
                }

                JsonObject now = obj.getAsJsonObject("now");
                return String.format("天气：%s，温度：%s℃，风向：%s，风力：%s级",
                        now.get("text").getAsString(),
                        now.get("temp").getAsString(),
                        now.get("windDir").getAsString(),
                        now.get("windScale").getAsString());
            }

        } catch (Exception e) {
            System.err.println("[天气] 天气信息获取异常: " + e.getMessage());
            e.printStackTrace();
            return "获取天气失败";
        }
    }
}
