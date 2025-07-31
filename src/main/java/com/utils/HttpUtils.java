package com.utils;

import org.springframework.util.StringUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author xushi
 * @version 1.0
 * @project zsglpt_cloud
 * @description http 工具类
 * @date 2025/2/6 19:02:13
 */
public class HttpUtils {

    /**
     * @param urlStr  请求的 URL
     * @param headers 自定义请求头
     * @return java.lang.String
     * @description 发送 HTTP GET 请求，支持自定义请求头
     * @author xushi
     * @date 2025/2/6 19:07:50
     */
    public static String sendGet(String urlStr, Map<String, String> headers) {
        try {
            // 创建 URL 对象
            URL url = new URL(urlStr);
            // 打开连接
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            // 设置请求方法为 GET
            connection.setRequestMethod("GET");
            // 设置连接超时和读取超时时间
            connection.setConnectTimeout(10000);
            // 设置读取超时时间
            connection.setReadTimeout(10000);

            // 设置自定义请求头
            if (headers != null) {
                // 遍历请求头并设置
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    // 设置请求头
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            // 获取响应码
            int responseCode = connection.getResponseCode();
            // 如果响应码为 200，表示请求成功
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // 创建 BufferedReader 读取响应内容
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                // 创建 StringBuilder 存储响应内容
                StringBuilder response = new StringBuilder();
                // 逐行读取响应内容并追加到 StringBuilder 中
                String line;
                // 循环读取响应内容，直到读取完毕
                while ((line = reader.readLine()) != null) {
                    // 将读取到的一行内容追加到 StringBuilder 中
                    response.append(line);
                }
                // 关闭 BufferedReader
                reader.close();
                // 返回响应内容
                return response.toString();
            } else {
                throw new IOException("HTTP GET request failed with response code: " + responseCode);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @param urlStr  请求的 URL
     * @param headers 自定义请求头
     * @return java.lang.String
     * @description 发送 HTTP GET 请求，支持自定义请求头
     * @author xushi
     * @date 2025/2/6 19:07:50
     */
    public static String sendDelete(String urlStr, Map<String, String> headers) {
        try {
            // 创建 URL 对象
            URL url = new URL(urlStr);
            // 打开连接
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            // 设置请求方法为 GET
            connection.setRequestMethod("DELETE");
            // 设置连接超时和读取超时时间
            connection.setConnectTimeout(10000);
            // 设置读取超时时间
            connection.setReadTimeout(10000);

            // 设置自定义请求头
            if (headers != null) {
                // 遍历请求头并设置
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    // 设置请求头
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            // 获取响应码
            int responseCode = connection.getResponseCode();
            // 如果响应码为 200，表示请求成功
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // 创建 BufferedReader 读取响应内容
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                // 创建 StringBuilder 存储响应内容
                StringBuilder response = new StringBuilder();
                // 逐行读取响应内容并追加到 StringBuilder 中
                String line;
                // 循环读取响应内容，直到读取完毕
                while ((line = reader.readLine()) != null) {
                    // 将读取到的一行内容追加到 StringBuilder 中
                    response.append(line);
                }
                // 关闭 BufferedReader
                reader.close();
                // 返回响应内容
                return response.toString();
            } else {
                throw new IOException("HTTP GET request failed with response code: " + responseCode);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @param urlStr  请求的 URL
     * @param headers 自定义请求头
     * @param params  请求参数
     * @return java.lang.String
     * @description 发送 HTTP POST 请求，支持自定义请求头和请求参数
     * @author xushi
     * @date 2025/2/6 19:08:06
     */
    public static String sendPost(String urlStr, Map<String, String> headers, String params) {
        try {
            // 创建 URL 对象
            URL url = new URL(urlStr);
            // 打开连接
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            // 设置请求方法为 POST
            connection.setRequestMethod("POST");
            // 设置连接超时和读取超时时间
            connection.setConnectTimeout(5000);
            // 设置读取超时时间
            connection.setReadTimeout(5000);
            // 设置请求头
            connection.setDoOutput(true);

            // 设置自定义请求头
            if (headers != null) {
                // 遍历请求头并设置
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    // 设置请求头
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

//            // 构建请求参数
//            StringBuilder postData = new StringBuilder();
//            // 遍历请求参数并构建请求体
//            if (params != null) {
//                // 遍历请求参数并构建请求体
//                for (Map.Entry<String, Object> param : params.entrySet()) {
//                    // 如果请求参数不为空，则在请求体中添加分隔符 &
//                    if (postData.length() != 0) {
//                        // 在请求体中添加分隔符 &
//                        postData.append('&');
//                    }
//                    // 在请求体中添加请求参数
//                    postData.append(param.getKey());
//                    // 在请求体中添加分隔符 =
//                    postData.append('=');
//                    // 在请求体中添加请求参数值
//                    postData.append(param.getValue());
//                }
//            }
            // 将请求体转换为字节数组
            byte[] postDataBytes = (StringUtils.isEmpty(params) ? "" : params).getBytes(StandardCharsets.UTF_8);

            // 发送请求参数
            DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
            // 写入请求参数
            outputStream.write(postDataBytes);
            // 刷新输出流
            outputStream.flush();
            // 关闭输出流
            outputStream.close();

            // 获取响应码
            int responseCode = connection.getResponseCode();
            // 如果响应码为 200，表示请求成功
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // 创建 BufferedReader 读取响应内容
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                // 创建 StringBuilder 存储响应内容
                StringBuilder response = new StringBuilder();
                // 逐行读取响应内容并追加到 StringBuilder 中
                String line;
                // 循环读取响应内容，直到读取完毕
                while ((line = reader.readLine()) != null) {
                    // 将读取到的一行内容追加到 StringBuilder 中
                    response.append(line);
                }
                // 关闭 BufferedReader
                reader.close();
                // 返回响应内容
                return response.toString();
            } else {
                throw new IOException("HTTP POST request failed with response code: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @param urlStr      请求的 URL
     * @param storagePath 本地存储地址
     * @return java.lang.String
     * @description 发送 HTTP GET 请求，获取文件，并将文件存储到指定路径下
     * @author xushi
     * @date 2025/2/6 19:08:06
     */
    public static void sendGetAndStorageFile(String urlStr, String storagePath) {
        try (// 创建 URL 对象
             InputStream inputStream = new URL(urlStr).openStream();
             // 创建一个文件输入流，用于将文件写入到文件夹中
             FileOutputStream fileOutputStream = new FileOutputStream(storagePath)) {
            // 创建缓冲区
            byte[] buffer = new byte[4096];

            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, length);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
