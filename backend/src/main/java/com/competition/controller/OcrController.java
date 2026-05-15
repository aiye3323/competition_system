package com.competition.controller;

import com.competition.dto.OcrResultDTO;
import com.competition.dto.Result;
import com.competition.service.OcrService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ocr")
@RequiredArgsConstructor
public class OcrController {

    private final OcrService ocrService;

    @PostMapping("/recognize")
    public Result<OcrResultDTO> recognize(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String type) {
        return doRecognize(file, null, type);
    }

    @PostMapping("/recognize-by-id")
    public Result<OcrResultDTO> recognizeById(
            @RequestParam("fileId") Long fileId,
            @RequestParam("type") String type) {
        return doRecognize(null, fileId, type);
    }

    private Result<OcrResultDTO> doRecognize(MultipartFile file, Long fileId, String type) {
        if (file == null && fileId == null) {
            return Result.error(400, "请提供文件或文件ID");
        }
        String[] validTypes = {"competition", "project", "software", "paper"};
        boolean valid = false;
        for (String t : validTypes) {
            if (t.equals(type)) { valid = true; break; }
        }
        if (!valid) {
            return Result.error(400, "无效的成果类型: " + type);
        }

        OcrResultDTO result;
        if (fileId != null) {
            result = ocrService.recognizeByFileId(fileId, type);
        } else {
            result = ocrService.recognize(file, type);
        }
        return Result.success(result);
    }
}
