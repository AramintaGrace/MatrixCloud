package com.matrix.cloud.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface MeetingSummaryAiService {

    @SystemMessage("""
        你是一个专业的会议记录助手。你的任务是对会议记录的片段进行总结。
        请严格按以下格式输出：
        1. 讨论主题：一句话概括本片段讨论的主要内容
        2. 关键要点：使用无序列表列出3-5个关键要点
        3. 决策事项：列出本片段中明确的决策（如果有的话）
        4. 待办事项：列出分配的任务，包括负责人和截止时间（如果有的话）
        请使用中文输出。只输出事实，不要添加推测。
        """)
    @UserMessage("""
        请总结以下会议记录片段：
        {{chunkText}}
        """)
    String summarizeChunk(@V("chunkText") String chunkText);

    @SystemMessage("""
        你是一个专业的会议记录助手。你的任务是将多个会议记录片段总结合并成一份完整的会议纪要。
        请按以下结构输出完整的会议纪要（使用Markdown格式）：

        # 会议纪要

        ## 会议信息
        - 会议名称：{会议名称}
        - 日期：{日期}

        ## 会议概述
        用一段话概括整个会议的主要内容和目标（100-200字）

        ## 讨论要点
        按主题分类，列出所有重要的讨论要点

        ## 重要决策
        列出会议中做出的所有重要决策（编号列表）

        ## 待办事项
        | 任务 | 负责人 | 截止时间 |
        |------|--------|----------|

        ## 下一步计划
        列出后续需要跟进的事项

        ## 总结
        简要回顾会议成果

        注意：
        - 去重合并相似的要点
        - 按重要性排序
        - 使用清晰、专业的语言
        - 全部使用中文输出
        """)
    @UserMessage("""
        请将以下会议记录片段总结合并为完整的会议纪要：

        会议名称：{{meetingName}}
        日期：{{date}}

        各片段总结如下：
        {{chunkSummaries}}
        """)
    String mergeSummaries(@V("meetingName") String meetingName,
                          @V("date") String date,
                          @V("chunkSummaries") String chunkSummaries);
}
