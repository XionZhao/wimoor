package com.wimoor.common.service.util;

import cn.hutool.core.util.StrUtil;
import com.wimoor.common.service.ObjectHandler;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Part;
import lombok.Setter;
import okhttp3.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
@ConfigurationProperties(prefix = "large.minio")
public class MinIOLargeApiUtil {
    @Setter
    private String minio_endpoint;
    @Setter
    private String accessKeyId;
    @Setter
    private String accessKeySecret;
    @Setter
    public String bucketName;
    @Setter
    public String bucketPath;

    /** 预签名 URL 过期秒数 */
    private static final int PRESIGN_EXPIRY = 3600;

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    public boolean isRun() {
        return StrUtil.isNotBlank(minio_endpoint)
            && StrUtil.isNotBlank(accessKeyId)
            && StrUtil.isNotBlank(accessKeySecret);
    }

    public MinioClient getClient() {
        return MinioClient.builder()
                .endpoint(minio_endpoint)
                .credentials(accessKeyId, accessKeySecret)
                .build();
    }

    public boolean putObject(String bucketName, String objectName, InputStream inputStream) {
        try {
            ByteArrayOutputStream outputstream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputstream.write(buffer, 0, bytesRead);
            }
            outputstream.flush();
            int length = outputstream.size();
            inputStream = new ByteArrayInputStream(outputstream.toByteArray());
            MinioClient client = getClient();
            client.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(inputStream, Long.valueOf(length), Long.valueOf(-1))
                    .build()
            );
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void removeObject(String bucketName, String objectName) {
        try {
            MinioClient client = getClient();
            client.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build()
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getObject(String bucketName, String objectName, ObjectHandler handler, Map<String, Object> param) {
        try {
            MinioClient client = getClient();
            InputStream inputStream = client.getObject(
                GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build()
            );
            handler.treatReader(inputStream, param);
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getBucketName() { return bucketName; }
    public String getBucketPath() { return bucketPath; }

    // ==================== 分片上传（SDK 预签名 URL + OkHttp 调用） ====================

    /**
     * 生成预签名 URL，签名由 SDK 计算，Signature 放在 query 中
     */
    private String presigned(Method method, String bucketName, String objectName, Map<String, String> queryParams) throws Exception {
        MinioClient client = getClient();
        return client.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(method)
                .bucket(bucketName)
                .object(objectName)
                .extraQueryParams(queryParams)
                .expiry(PRESIGN_EXPIRY, TimeUnit.SECONDS)
                .build()
        );
    }

    /**
     * 初始化分片上传，返回 uploadId
     */
    public String initiateMultipartUpload(String bucketName, String objectName) throws Exception {
        // 关键：在 initiate 阶段设置对象 Content-Type，否则合并后对象会被标记为 application/xml，
        // 导致浏览器按 XML 解析二进制文件报错
        String contentType = guessContentType(objectName);
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("uploads", "");
        Map<String, String> extraHeaders = new HashMap<>();
        extraHeaders.put("Content-Type", contentType);

        MinioClient client = getClient();
        String presignedUrl = client.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(Method.POST)
                .bucket(bucketName)
                .object(objectName)
                .extraQueryParams(queryParams)
                .extraHeaders(extraHeaders)
                .expiry(PRESIGN_EXPIRY, TimeUnit.SECONDS)
                .build()
        );

        RequestBody emptyBody = RequestBody.create(new byte[0], MediaType.parse(contentType));
        Request request = new Request.Builder().url(presignedUrl).post(emptyBody).build();
        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            String respBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IOException("initiateMultipartUpload failed: " + response.code() + " " + respBody);
            }
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new ByteArrayInputStream(respBody.getBytes(StandardCharsets.UTF_8)));
            return doc.getElementsByTagName("UploadId").item(0).getTextContent();
        }
    }

    /**
     * 根据对象名扩展名推断 Content-Type
     */
    private String guessContentType(String objectName) {
        if (objectName == null) return "application/octet-stream";
        String name = objectName.toLowerCase();
        int dot = name.lastIndexOf('.');
        String ext = dot >= 0 ? name.substring(dot) : "";
        switch (ext) {
            case ".mp4": return "video/mp4";
            case ".avi": return "video/x-msvideo";
            case ".mov": return "video/quicktime";
            case ".mkv": return "video/x-matroska";
            case ".flv": return "video/x-flv";
            case ".jpg": case ".jpeg": return "image/jpeg";
            case ".png": return "image/png";
            case ".gif": return "image/gif";
            case ".webp": return "image/webp";
            case ".bmp": return "image/bmp";
            case ".svg": return "image/svg+xml";
            case ".ico": return "image/x-icon";
            case ".pdf": return "application/pdf";
            case ".xlsx": return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case ".xls": return "application/vnd.ms-excel";
            case ".docx": return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case ".doc": return "application/msword";
            case ".pptx": return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case ".ppt": return "application/vnd.ms-powerpoint";
            case ".txt": return "text/plain";
            case ".csv": return "text/csv";
            case ".zip": return "application/zip";
            case ".rar": return "application/x-rar-compressed";
            case ".7z": return "application/x-7z-compressed";
            case ".json": return "application/json";
            case ".xml": return "application/xml";
            case ".md": return "text/markdown";
            default: return "application/octet-stream";
        }
    }

    /**
     * 上传单个分片（服务端）
     * @return Part
     */
    public Part uploadPart(String bucketName, String objectName, String uploadId,
                           int partNumber, InputStream data, long partSize) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = data.read(buf)) != -1) baos.write(buf, 0, n);
        byte[] body = baos.toByteArray();

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("uploadId", uploadId);
        queryParams.put("partNumber", String.valueOf(partNumber));
        String presignedUrl = presigned(Method.PUT, bucketName, objectName, queryParams);

        RequestBody requestBody = RequestBody.create(body, MediaType.parse("application/octet-stream"));
        Request request = new Request.Builder().url(presignedUrl).put(requestBody).build();

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("uploadPart failed: " + response.code());
            String etag = response.header("ETag");
            return new Part(partNumber, etag);
        }
    }

    /**
     * 合并分片
     */
    public void completeMultipartUpload(String bucketName, String objectName,
                                        String uploadId, List<Part> parts) throws Exception {
        StringBuilder xml = new StringBuilder("<CompleteMultipartUpload>");
        for (Part part : parts) {
            xml.append("<Part><PartNumber>").append(part.partNumber())
               .append("</PartNumber><ETag>").append(part.etag())
               .append("</ETag></Part>");
        }
        xml.append("</CompleteMultipartUpload>");
        byte[] body = xml.toString().getBytes(StandardCharsets.UTF_8);

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("uploadId", uploadId);
        String presignedUrl = presigned(Method.POST, bucketName, objectName, queryParams);

        RequestBody requestBody = RequestBody.create(body, MediaType.parse("application/xml"));
        Request request = new Request.Builder().url(presignedUrl).post(requestBody).build();

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errBody = response.body() != null ? response.body().string() : "";
                throw new IOException("completeMultipartUpload failed: " + response.code() + " " + errBody);
            }
        }
    }

    /**
     * 查询已上传的分片编号列表
     */
    public List<Integer> listUploadedParts(String bucketName, String objectName,
                                           String uploadId) throws Exception {
        List<Part> parts = listParts(bucketName, objectName, uploadId);
        List<Integer> partNumbers = new ArrayList<>();
        for (Part part : parts) {
            partNumbers.add(part.partNumber());
        }
        return partNumbers;
    }

    /**
     * 查询已上传的分片列表，按 partNumber 升序
     */
    public List<Part> listParts(String bucketName, String objectName, String uploadId) throws Exception {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("uploadId", uploadId);
        String presignedUrl = presigned(Method.GET, bucketName, objectName, queryParams);

        Request request = new Request.Builder().url(presignedUrl).get().build();
        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errBody = response.body() != null ? response.body().string() : "";
                throw new IOException("listParts failed: " + response.code() + " " + errBody);
            }
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new ByteArrayInputStream(response.body().bytes()));
            List<Part> parts = new ArrayList<>();
            NodeList partNodes = doc.getElementsByTagName("Part");
            for (int i = 0; i < partNodes.getLength(); i++) {
                Element el = (Element) partNodes.item(i);
                int num = Integer.parseInt(el.getElementsByTagName("PartNumber").item(0).getTextContent());
                String etag = el.getElementsByTagName("ETag").item(0).getTextContent();
                parts.add(new Part(num, etag));
            }
            parts.sort(Comparator.comparingInt(Part::partNumber));
            return parts;
        }
    }

    /**
     * 生成分片上传的预签名 URL，前端可直接 PUT 到 MinIO
     */
    public String getPresignedUploadUrl(String bucketName, String objectName,
                                        String uploadId, int partNumber,
                                        int expirySeconds) throws Exception {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("uploadId", uploadId);
        queryParams.put("partNumber", String.valueOf(partNumber));
        return presigned(Method.PUT, bucketName, objectName, queryParams);
    }

    /**
     * 取消分片上传
     */
    public void abortMultipartUpload(String bucketName, String objectName, String uploadId) throws Exception {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("uploadId", uploadId);
        String presignedUrl = presigned(Method.DELETE, bucketName, objectName, queryParams);

        Request request = new Request.Builder().url(presignedUrl).delete().build();
        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            // 204 表示成功
        }
    }
}