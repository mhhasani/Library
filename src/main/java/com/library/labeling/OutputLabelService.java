package com.library.labeling;

import com.library.entity.enums.ClassificationLevel;
import com.library.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Builds the label every exported/printed output must carry: classification of the
 * content, who requested it, from which system (IP), and when.
 */
@Service
public class OutputLabelService {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ClassificationLevel systemClassification;

    public OutputLabelService(
            @Value("${app.classification.system-level:HIGHLY_CONFIDENTIAL}") ClassificationLevel systemClassification) {
        this.systemClassification = systemClassification;
    }

    /** Classification of system-wide reports (audit trail, statistics). */
    public ClassificationLevel systemClassification() {
        return systemClassification;
    }

    public OutputLabel labelFor(ClassificationLevel classification) {
        HttpServletRequest request = RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes a
                ? a.getRequest() : null;
        return new OutputLabel(
                classification,
                SecurityUtils.getCurrentUserId(),
                SecurityUtils.getCurrentUserEmail(),
                request != null ? request.getRemoteAddr() : null,
                LocalDateTime.now().format(TIME));
    }

    public record OutputLabel(ClassificationLevel classification, Long userId, String userEmail,
                              String clientIp, String requestedAt) {

        /** Single-line form used in file headers/footers and HTTP headers (ASCII-safe). */
        public String toAsciiLine() {
            return String.format("Classification: %s | User: %s (%s) | IP: %s | Time: %s",
                    classification, userId, userEmail, clientIp, requestedAt);
        }

        public String toPersianLine() {
            return String.format("طبقه‌بندی: %s | کاربر: %s (%s) | IP: %s | زمان: %s",
                    classification.getPersianLabel(), userId, userEmail, clientIp, requestedAt);
        }
    }
}
