package com.matrix.cloud.service;

import com.matrix.cloud.entity.MeetingSummary;
import com.matrix.cloud.entity.VideoMeeting;
import com.matrix.cloud.exception.ResourceNotFoundException;
import com.matrix.cloud.repository.MeetingSummaryRepository;
import com.matrix.cloud.repository.VideoMeetingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Service
public class MeetingSummaryManager {

    @Autowired
    private MeetingSummaryAiService aiService;

    @Autowired
    private MeetingSummaryRepository summaryRepository;

    @Autowired
    private VideoMeetingRepository meetingRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    @Qualifier("meetingSummaryExecutor")
    private ExecutorService executor;

    @Value("${meeting.summary.chunk-size:2000}")
    private int chunkSize;

    @Value("${meeting.summary.chunk-overlap-ratio:0.15}")
    private double overlapRatio;

    @Value("${meeting.summary.max-chunks:20}")
    private int maxChunks;

    @Value("${langchain4j.open-ai.model-name:unknown}")
    private String modelName;

    public MeetingSummary generateSummary(Long meetingId, Long userId, String transcriptText) {
        if (transcriptText == null || transcriptText.trim().isEmpty()) {
            throw new RuntimeException("会议记录文本不能为空");
        }

        // Check if a completed summary already exists for this meeting (cache hit)
        Optional<MeetingSummary> existing = summaryRepository
                .findTopByMeetingIdAndStatusOrderByCreatedAtDesc(meetingId, MeetingSummary.Status.COMPLETED);
        if (existing.isPresent()) {
            return existing.get();
        }

        VideoMeeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("会议不存在"));

        MeetingSummary summary = new MeetingSummary();
        summary.setMeetingId(meetingId);
        summary.setCreatorId(userId);
        summary.setOriginalText(transcriptText);
        summary.setStatus(MeetingSummary.Status.PROCESSING);
        summary.setModelName(modelName);
        summary = summaryRepository.save(summary);

        try {
            List<String> chunks = splitIntoSpeakerAwareChunks(transcriptText);
            summary.setChunkCount(chunks.size());
            summaryRepository.save(summary);

            List<CompletableFuture<String>> mapFutures = chunks.stream()
                .map(chunk -> CompletableFuture.supplyAsync(
                    () -> aiService.summarizeChunk(chunk),
                    executor
                ))
                .collect(Collectors.toList());

            CompletableFuture<Void> allMaps = CompletableFuture.allOf(
                mapFutures.toArray(new CompletableFuture[0])
            );

            try {
                allMaps.get(120L * chunks.size(), TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                throw new RuntimeException("会议纪要生成超时，请稍后重试");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("会议纪要生成被中断");
            } catch (Exception e) {
                throw new RuntimeException("会议纪要生成失败: " + e.getMessage());
            }

            List<String> chunkSummaries = mapFutures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());

            String chunkSummariesText = buildChunkSummariesText(chunkSummaries);
            String finalSummary = aiService.mergeSummaries(
                meeting.getRoomName(),
                LocalDate.now().toString(),
                chunkSummariesText
            );

            summary.setSummaryText(finalSummary);
            summary.setStatus(MeetingSummary.Status.COMPLETED);

            // Save markdown to MinIO
            try {
                String objectName = "summaries/" + meetingId + "_" + UUID.randomUUID().toString().substring(0, 8) + ".md";
                byte[] mdBytes = finalSummary.getBytes(StandardCharsets.UTF_8);
                fileStorageService.uploadBytes(objectName, mdBytes, "text/markdown");
                summary.setMinioObjectName(objectName);
            } catch (Exception e) {
                // MinIO save failure is non-fatal — summary text is still in DB
            }

            summaryRepository.save(summary);
            return summary;

        } catch (RuntimeException e) {
            summary.setStatus(MeetingSummary.Status.FAILED);
            summary.setErrorMessage(e.getMessage());
            summaryRepository.save(summary);
            throw e;
        }
    }

    public List<Map<String, Object>> getSummaryHistory(List<Long> meetingIds) {
        List<MeetingSummary> summaries = summaryRepository
                .findByMeetingIdInAndStatusOrderByCreatedAtDesc(meetingIds, MeetingSummary.Status.COMPLETED);

        Map<Long, MeetingSummary> latestByMeeting = new java.util.LinkedHashMap<>();
        for (MeetingSummary s : summaries) {
            latestByMeeting.putIfAbsent(s.getMeetingId(), s);
        }

        return latestByMeeting.values().stream().map(s -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", s.getId());
            map.put("meetingId", s.getMeetingId());
            map.put("summaryText", s.getSummaryText());
            map.put("chunkCount", s.getChunkCount());
            map.put("modelName", s.getModelName());
            map.put("status", s.getStatus().name());
            map.put("creatorId", s.getCreatorId());
            map.put("createdAt", s.getCreatedAt().toString());
            return map;
        }).collect(Collectors.toList());
    }

    List<String> splitIntoSpeakerAwareChunks(String text) {
        String[] segments = text.split("(?=(?:说话人|Speaker)\\s*\\d+)");

        List<String> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        int overlapChars = (int) (chunkSize * overlapRatio);
        String previousChunkTail = "";

        for (String segment : segments) {
            if (segment.trim().isEmpty()) continue;

            int projectedLength = currentChunk.length() + segment.length();

            if (currentChunk.length() > 0 && projectedLength > chunkSize) {
                chunks.add(currentChunk.toString());
                if (chunks.size() >= maxChunks) break;

                String chunkText = currentChunk.toString();
                previousChunkTail = chunkText.length() > overlapChars
                    ? chunkText.substring(chunkText.length() - overlapChars)
                    : chunkText;
                currentChunk = new StringBuilder();

                if (!previousChunkTail.isEmpty()) {
                    currentChunk.append("【上文回顾】\n");
                    currentChunk.append(previousChunkTail);
                    currentChunk.append("\n【接上文继续】\n\n");
                }
            }

            currentChunk.append(segment);
        }

        if (currentChunk.length() > 0 && chunks.size() < maxChunks) {
            chunks.add(currentChunk.toString());
        }

        return chunks;
    }

    private String buildChunkSummariesText(List<String> chunkSummaries) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chunkSummaries.size(); i++) {
            sb.append("=== 片段 ").append(i + 1).append(" 总结 ===\n");
            sb.append(chunkSummaries.get(i)).append("\n\n");
        }
        return sb.toString();
    }

    public MeetingSummary getLatestSummary(Long meetingId) {
        return summaryRepository.findTopByMeetingIdOrderByCreatedAtDesc(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("该会议暂无纪要"));
    }
}
