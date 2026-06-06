package com.matrix.cloud.service;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class SpeechToTextService {

    private static final Logger log = LoggerFactory.getLogger(SpeechToTextService.class);

    @Value("${siliconflow.api.key:sk-fpvxyqorlftgtkupcswbeethmkeqgcbushpduxrnwcsovedc}")
    private String apiKey;

    @Value("${siliconflow.api.url:https://api.siliconflow.cn/v1/audio/transcriptions}")
    private String apiUrl;

    @Value("${siliconflow.model.name:FunAudioLLM/SenseVoiceSmall}")
    private String modelName;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String transcribe(MultipartFile audioFile) {
        HttpURLConnection conn = null;
        try {
            byte[] fileBytes = audioFile.getBytes();
            String filename = audioFile.getOriginalFilename() != null
                    ? audioFile.getOriginalFilename()
                    : "audio.wav";

            String boundary = "----MatrixCloud" + System.currentTimeMillis();
            String CRLF = "\r\n";

            URI uri = new URI(apiUrl);
            conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(180000);
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            log.info("Sending audio to SiliconFlow: file={}, size={} bytes, model={}, boundary={}",
                    filename, fileBytes.length, modelName, boundary);

            try (OutputStream out = conn.getOutputStream()) {
                // -- model field
                writePart(out, boundary, CRLF, "model", modelName, null);

                // -- file field
                writePart(out, boundary, CRLF, "file", filename, fileBytes);

                // -- closing boundary
                out.write(("--" + boundary + "--" + CRLF).getBytes(StandardCharsets.UTF_8));
                out.flush();
            }

            int status = conn.getResponseCode();
            InputStream is = (status >= 200 && status < 300)
                    ? conn.getInputStream()
                    : conn.getErrorStream();
            String responseBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            log.info("SiliconFlow response: status={}, body={}", status, responseBody);

            if (status >= 200 && status < 300) {
                @SuppressWarnings("unchecked")
                Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
                if (responseMap.get("text") != null) {
                    String text = responseMap.get("text").toString();
                    log.info("Transcription result: {}", text);
                    return text;
                }
                throw new RuntimeException("语音转文本返回为空");
            } else {
                throw new RuntimeException("语音转文本失败: " + status + " " + responseBody);
            }
        } catch (Exception e) {
            log.error("Speech-to-text failed: {}", e.getMessage());
            throw new RuntimeException("语音转文本失败: " + e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private void writePart(OutputStream out, String boundary, String CRLF,
                           String name, String value, byte[] fileContent) throws Exception {
        StringBuilder header = new StringBuilder();
        header.append("--").append(boundary).append(CRLF);
        if (fileContent != null) {
            // File part
            header.append("Content-Disposition: form-data; name=\"").append(name)
                    .append("\"; filename=\"").append(value).append("\"").append(CRLF);
            header.append("Content-Type: application/octet-stream").append(CRLF);
            header.append(CRLF);
            out.write(header.toString().getBytes(StandardCharsets.UTF_8));
            out.write(fileContent);
            out.write(CRLF.getBytes(StandardCharsets.UTF_8));
        } else {
            // Text field part
            header.append("Content-Disposition: form-data; name=\"").append(name).append("\"").append(CRLF);
            header.append(CRLF);
            header.append(value).append(CRLF);
            out.write(header.toString().getBytes(StandardCharsets.UTF_8));
        }
    }
}
