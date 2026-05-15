package com.competition.service;

import com.baidu.aip.ocr.AipOcr;
import com.competition.dto.OcrResultDTO;
import com.competition.entity.FileEntity;
import com.competition.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.*;

@Service
@RequiredArgsConstructor
public class OcrService {

    private final AipOcr aipOcr;
    private final FileRepository fileRepository;

    @Value("${app.upload.path}")
    private String uploadPath;

    public OcrResultDTO recognizeByFileId(Long fileId, String type) {
        FileEntity fileEntity = fileRepository.findById(fileId).orElse(null);
        if (fileEntity == null) return OcrResultDTO.fail("文件记录不存在");
        Path basePath = Paths.get(uploadPath).toAbsolutePath().normalize();
        Path filePath = basePath.resolve(fileEntity.getStoragePath()).normalize();
        if (!filePath.startsWith(basePath)) return OcrResultDTO.fail("非法文件路径");
        try {
            return doRecognize(Files.readAllBytes(filePath), type);
        } catch (Exception e) {
            return OcrResultDTO.fail("读取文件失败: " + e.getMessage());
        }
    }

    public OcrResultDTO recognize(MultipartFile file, String type) {
        try {
            return doRecognize(file.getBytes(), type);
        } catch (Exception e) {
            return OcrResultDTO.fail("OCR 识别异常: " + e.getMessage());
        }
    }

    private OcrResultDTO doRecognize(byte[] imageBytes, String type) {
        JSONObject response = aipOcr.basicAccurateGeneral(imageBytes, new HashMap<>());
        if (response.has("error_code")) {
            return OcrResultDTO.fail("OCR 识别失败: " + response.optString("error_msg"));
        }

        JSONArray wordsResult = response.getJSONArray("words_result");
        List<String> lines = new ArrayList<>();
        StringBuilder rawText = new StringBuilder();
        for (int i = 0; i < wordsResult.length(); i++) {
            String line = wordsResult.getJSONObject(i).getString("words").trim();
            if (!line.isEmpty()) {
                lines.add(line);
                rawText.append(line).append("\n");
            }
        }
        String text = rawText.toString();

        OcrResultDTO result = OcrResultDTO.ok(text);
        result.setCategory(type);

        switch (type) {
            case "competition" -> parseCompetition(lines, text, result);
            case "project"     -> parseProject(lines, text, result);
            case "software"    -> parseSoftware(lines, text, result);
            case "paper"       -> parsePaper(lines, text, result);
        }

        return result;
    }

    // ==================== 竞赛 ====================

    private void parseCompetition(List<String> lines, String text, OcrResultDTO r) {
        r.setName(matchLabel(lines, "竞赛名称", "获奖项目"));
        if (r.getName() == null) {
            r.setName(matchFirst(text, "([\\u4e00-\\u9fa5]{3,}(?:大赛|竞赛|比赛|挑战杯|杯|奖))"));
        }

        // 级别 + 等级：逐行扫描
        for (String line : lines) {
            if (r.getAwardLevel() == null) r.setAwardLevel(detectLevel(line));
            if (r.getAwardRank() == null) {
                r.setAwardRank(matchFirst(line, "(特等奖|一等奖|二等奖|三等奖|优秀奖|金奖|银奖|铜奖|[一二三]等)"));
            }
        }
        if (r.getAwardLevel() == null) {
            r.setAwardLevel(normalizeLevel(matchFirst(text, "(国家级|省级|市级|校级|院级)")));
        }
        if (r.getAwardRank() == null) {
            r.setAwardRank(matchFirst(text, "(特等奖|一等奖|二等奖|三等奖|优秀奖|金奖|银奖|铜奖|[一二三]等)"));
        }

        r.setCompetitionDate(extractDate(text));
        r.setHostUnit(matchLabel(lines, "主办单位", "主办"));
        if (r.getHostUnit() == null) {
            r.setHostUnit(matchFirst(text, "([\\u4e00-\\u9fa5]{3,}(?:大学|学院|学会|协会|委员会|教育部|教育厅|组委会))"));
        }

        // 指导教师
        r.setAdvisor(matchLabel(lines, "指导教师", "指导老师", "导师"));
        if (r.getAdvisor() == null) {
            r.setAdvisor(matchFirst(text, "(?:指导教师|指导老师|导师)[：:]\\s*([\\u4e00-\\u9fa5]{2,6})"));
        }

        // 参赛选手
        r.setSubmitter(matchLabel(lines, "参赛选手", "参赛人员", "选手", "获奖者", "获奖人"));
        if (r.getSubmitter() == null) {
            r.setSubmitter(matchFirst(text, "(?:参赛选手|参赛人员|选手|获奖者)[：:]\\s*(.+?)(?:\\n|$)"));
        }
    }

    // ==================== 项目 ====================

    private void parseProject(List<String> lines, String text, OcrResultDTO r) {
        r.setName(matchLabel(lines, "项目名称", "课题名称"));
        if (r.getName() == null) {
            r.setName(matchFirst(text, "([\\u4e00-\\u9fa5]{3,}(?:系统|平台|机器人|装置|算法|方法|研究|项目))"));
        }

        for (String line : lines) {
            if (r.getProjectLevel() == null) r.setProjectLevel(detectLevel(line));
        }
        if (r.getProjectLevel() == null) {
            r.setProjectLevel(normalizeLevel(matchFirst(text, "(国家级|省级|校级|院级)")));
        }

        r.setProjectType(matchFirst(text, "(创新训练|创业训练|创业实践)"));

        // 指导教师
        r.setAdvisor(matchLabel(lines, "指导教师", "指导老师", "导师"));
        if (r.getAdvisor() == null) {
            r.setAdvisor(matchFirst(text, "(?:指导教师|指导老师|导师)[：:]\\s*([\\u4e00-\\u9fa5]{2,6})"));
        }

        // 立项人员 / 项目成员
        r.setSubmitter(matchLabel(lines, "立项人员", "项目成员", "成员", "参与人员"));
        if (r.getSubmitter() == null) {
            r.setSubmitter(matchFirst(text, "(?:立项人员|项目成员|成员)[：:]\\s*(.+?)(?:\\n|$)"));
        }

        r.setEstablishTime(extractDate(text));
    }

    // ==================== 软著 ====================

    private void parseSoftware(List<String> lines, String text, OcrResultDTO r) {
        r.setSoftwareName(matchLabel(lines, "软件名称", "软件全称"));
        if (r.getSoftwareName() == null) {
            r.setSoftwareName(matchFirst(text, "([\\u4e00-\\u9fa5a-zA-Z]{3,}(?:系统|平台|软件|管理系统|工具|助手))"));
        }

        r.setCopyrightHolder(matchLabel(lines, "著作权人", "申请人", "权利人"));
        if (r.getCopyrightHolder() == null) {
            r.setCopyrightHolder(matchFirst(text, "(?:著作权人|申请人)[：:]\\s*([\\u4e00-\\u9fa5a-zA-Z]{2,})"));
        }

        r.setRegistrationNo(matchLabel(lines, "登记号", "登记证号", "证书号"));
        if (r.getRegistrationNo() == null) {
            r.setRegistrationNo(matchFirst(text, "(\\d{4}\\s*SR\\s*\\d{5,})"));
        }
        if (r.getRegistrationNo() == null) {
            r.setRegistrationNo(matchFirst(text, "(\\d{4}\\s*5R\\s*\\d{5,})"));
        }
        // 逐行扫描包含"登记"的行，提取长数字序列
        if (r.getRegistrationNo() == null) {
            for (String line : lines) {
                if (line.contains("登记") || line.contains("证书") || line.contains("号")) {
                    String num = matchFirst(line, "(\\d{10,})");
                    if (num != null) { r.setRegistrationNo(num); break; }
                }
            }
        }
        // 全文搜索长数字序列（至少10位）
        if (r.getRegistrationNo() == null) {
            r.setRegistrationNo(matchFirst(text, "(\\d{4}[A-Za-z]{1,3}\\d{5,})"));
        }
        // 清理空格
        if (r.getRegistrationNo() != null) {
            r.setRegistrationNo(r.getRegistrationNo().replaceAll("\\s+", ""));
        }

        r.setRegistrationDate(extractDate(text));
    }

    // ==================== 论文 ====================

    private void parsePaper(List<String> lines, String text, OcrResultDTO r) {
        r.setPaperTitle(matchLabel(lines, "论文题目", "论文标题", "题目", "标题"));
        if (r.getPaperTitle() == null) {
            // 最长中文行兜底
            String longest = "";
            for (String line : lines) {
                String cn = line.replaceAll("[^\\u4e00-\\u9fa5]", "");
                if (cn.length() > longest.length()) longest = cn;
            }
            if (longest.length() >= 6) r.setPaperTitle(longest);
        }

        r.setJournalName(matchLabel(lines, "期刊名称", "期刊", "发表期刊", "刊物名称"));
        if (r.getJournalName() == null) {
            r.setJournalName(matchFirst(text, "([\\u4e00-\\u9fa5]{3,}(?:学报|期刊))"));
        }
        if (r.getJournalName() == null) {
            r.setJournalName(matchFirst(text, "([A-Z][a-z]+\\s*(?:Journal|Transactions|Letters|Review))"));
        }

        r.setJournalLevel(matchFirst(text, "(SCI[一二三四]区|EI[期刊会议]*|CCF\\s*[ABC]类|北大核心|北大核心期刊|省级期刊|核心期刊)"));

        r.setAcceptanceDate(extractDate(text));
    }

    // ==================== 逐行标签匹配（核心方法）====================

    /**
     * 在多行 OCR 文本中查找标签，返回对应的值。
     * 支持两种格式：
     *   1. 同行: "指导教师：张三"
     *   2. 分离: 上一行 "指导教师"，下一行 "张三"
     */
    private String matchLabel(List<String> lines, String... labels) {
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            // 去掉空格便于模糊匹配（OCR 可能把 "登记号" 识别为 "登记 号"）
            String lineNoSpace = line.replaceAll("\\s+", "");
            for (String label : labels) {
                String labelNoSpace = label.replaceAll("\\s+", "");
                // 格式1: 标签：值 在同一行
                String v = matchFirst(lineNoSpace, Pattern.quote(labelNoSpace) + "[：:]\\s*(.+)");
                if (v != null && !v.isEmpty()) return cleanValue(v);

                // 格式2: 该行就是标签名本身，值在下一行
                String after = lineNoSpace.replaceFirst("^" + Pattern.quote(labelNoSpace) + "[：:]?\\s*", "");
                if (lineNoSpace.contains(labelNoSpace) && after.isEmpty()) {
                    if (i + 1 < lines.size()) {
                        String next = lines.get(i + 1).trim();
                        // 确保下一行不是另一个标签
                        if (!next.isEmpty() && !isLikelyLabel(next)) {
                            return cleanValue(next);
                        }
                    }
                }
                // 格式3: 标签后直接跟值但无冒号（如 "指导教师张三"）
                if (line.startsWith(label) && after.length() > 0 && after.length() < 20) {
                    return cleanValue(after);
                }
            }
        }
        return null;
    }

    /** 判断一行文本是否像标签而非值 */
    private boolean isLikelyLabel(String line) {
        return line.matches(".*[：:].*") || line.length() < 3
            || line.contains("单位") || line.contains("名称") || line.contains("级别")
            || line.contains("时间") || line.contains("日期");
    }

    /** 清理值文本中的多余标记 */
    private String cleanValue(String v) {
        if (v == null) return null;
        // 去掉行末可能残留的冒号
        return v.replaceAll("[：:]$", "").trim();
    }

    // ==================== 通用工具 ====================

    private String matchFirst(String text, String regex) {
        Matcher m = Pattern.compile(regex).matcher(text);
        return m.find() ? m.group(1).trim() : null;
    }

    private String extractDate(String text) {
        Matcher m1 = Pattern.compile("(\\d{4})[-/](\\d{1,2})[-/](\\d{1,2})").matcher(text);
        if (m1.find()) {
            return String.format("%s-%02d-%02d", m1.group(1),
                Integer.parseInt(m1.group(2)), Integer.parseInt(m1.group(3)));
        }
        Matcher m2 = Pattern.compile("(\\d{4})\\s*年\\s*(\\d{1,2})\\s*月\\s*(\\d{1,2})\\s*日").matcher(text);
        if (m2.find()) {
            return String.format("%s-%02d-%02d", m2.group(1),
                Integer.parseInt(m2.group(2)), Integer.parseInt(m2.group(3)));
        }
        Matcher m3 = Pattern.compile("(\\d{4})\\.(\\d{1,2})\\.(\\d{1,2})").matcher(text);
        if (m3.find()) {
            return String.format("%s-%02d-%02d", m3.group(1),
                Integer.parseInt(m3.group(2)), Integer.parseInt(m3.group(3)));
        }
        return null;
    }

    private String detectLevel(String line) {
        if (line.contains("国家级") || (line.contains("国家") && !line.contains("家级"))) return "国家级";
        if (line.contains("省级") || line.contains("省"))   return "省级";
        if (line.contains("市级") || line.contains("市"))   return "市级";
        if (line.contains("校级") || line.contains("校"))   return "校级";
        if (line.contains("院级") || line.contains("院"))   return "院级";
        return null;
    }

    private String normalizeLevel(String raw) {
        if (raw == null) return null;
        if (raw.contains("国家")) return "国家级";
        if (raw.contains("省")) return "省级";
        if (raw.contains("市")) return "市级";
        if (raw.contains("校")) return "校级";
        if (raw.contains("院")) return "院级";
        return raw;
    }
}
