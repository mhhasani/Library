package com.library.exception;

import com.library.audit.AuditEntry;
import com.library.audit.AuditService;
import com.library.dto.ApiResponse;
import com.library.entity.enums.AuditAction;
import com.library.keycloak.KeycloakAdminException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String AUTH_PATH_PREFIX = "/v1/auth/";

    private final AuditService auditService;

    public GlobalExceptionHandler(AuditService auditService) {
        this.auditService = auditService;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFound(ResourceNotFoundException ex, WebRequest request) {
        log.error("Resource not found: {}", ex.getMessage());
        return new ResponseEntity<>(
                ApiResponse.error("یافت نشد", ex.getMessage()),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadRequest(BadRequestException ex, WebRequest request) {
        log.error("Bad request: {}", ex.getMessage());
        return new ResponseEntity<>(
                ApiResponse.error("درخواست نامعتبر", ex.getMessage()),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnauthorized(UnauthorizedException ex, WebRequest request) {
        log.error("Unauthorized: {}", ex.getMessage());
        auditAccessDenied(request);
        return new ResponseEntity<>(
                ApiResponse.error("دسترسی غیرمجاز", ex.getMessage()),
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        log.error("Access denied: {}", ex.getMessage());
        auditAccessDenied(request);
        return new ResponseEntity<>(
                ApiResponse.error("دسترسی رد شد", "شما اجازه‌ی دسترسی به این بخش را ندارید"),
                HttpStatus.FORBIDDEN
        );
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadCredentials(BadCredentialsException ex, WebRequest request) {
        log.error("Bad credentials: {}", ex.getMessage());
        return new ResponseEntity<>(
                ApiResponse.error("ورود ناموفق", "ایمیل یا رمز عبور نادرست است"),
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        log.error("Validation error: {}", errors);
        // Show only the human-readable (Persian) messages, joined cleanly
        String detail = errors.values().stream()
                .distinct()
                .collect(Collectors.joining("، "));
        return new ResponseEntity<>(
                ApiResponse.error("اطلاعات واردشده نامعتبر است", detail),
                HttpStatus.BAD_REQUEST
        );
    }

    /** Malformed / missing / wrongly-typed client input: 400 with a generic message (no parser internals). */
    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class,
            MissingRequestHeaderException.class,
            MethodArgumentTypeMismatchException.class,
            ConstraintViolationException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ApiResponse<Object>> handleMalformedInput(Exception ex, WebRequest request) {
        log.warn("Malformed request input: {}: {}", ex.getClass().getSimpleName(), ex.getMessage());
        return new ResponseEntity<>(
                ApiResponse.error("درخواست نامعتبر", "اطلاعات ارسالی نامعتبر یا ناقص است"),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("Method not supported: {}", ex.getMessage());
        return new ResponseEntity<>(
                ApiResponse.error("درخواست نامعتبر", "این نوع درخواست پشتیبانی نمی‌شود"),
                HttpStatus.METHOD_NOT_ALLOWED
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleMediaType(HttpMediaTypeNotSupportedException ex) {
        log.warn("Unsupported media type: {}", ex.getMessage());
        return new ResponseEntity<>(
                ApiResponse.error("درخواست نامعتبر", "قالب داده‌ی ارسالی پشتیبانی نمی‌شود"),
                HttpStatus.UNSUPPORTED_MEDIA_TYPE
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Object>> handleMaxUpload(MaxUploadSizeExceededException ex) {
        log.warn("Upload too large: {}", ex.getMessage());
        return new ResponseEntity<>(
                ApiResponse.error("درخواست نامعتبر", "حجم فایل بیش از حد مجاز است"),
                HttpStatus.PAYLOAD_TOO_LARGE
        );
    }

    /**
     * Identity-provider failures: a rejected value (e.g. a password that violates the policy)
     * is the caller's error and Keycloak's reason is shown; anything else is reported as the
     * identity service being unavailable.
     */
    @ExceptionHandler(KeycloakAdminException.class)
    public ResponseEntity<ApiResponse<Object>> handleIdentityProvider(KeycloakAdminException ex) {
        if (ex.getStatus() == HttpStatus.BAD_REQUEST.value() || ex.getStatus() == HttpStatus.CONFLICT.value()) {
            log.warn("Identity provider rejected the request: {}", ex.getMessage());
            return new ResponseEntity<>(
                    ApiResponse.error("درخواست نامعتبر", "سامانه‌ی احراز هویت این مقدار را نپذیرفت: " + ex.getMessage()),
                    HttpStatus.BAD_REQUEST);
        }
        log.error("Identity provider unavailable: {}", ex.getMessage(), ex);
        return new ResponseEntity<>(
                ApiResponse.error("خطای سامانه‌ی احراز هویت", "سامانه‌ی احراز هویت در دسترس نیست؛ دوباره تلاش کنید"),
                HttpStatus.SERVICE_UNAVAILABLE);
    }

    /** Unknown paths: plain 404, never a directory listing or framework error page. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoResource(NoResourceFoundException ex) {
        log.warn("No resource: {}", ex.getResourcePath());
        return new ResponseEntity<>(
                ApiResponse.error("یافت نشد", "مسیر درخواستی وجود ندارد"),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception ex, WebRequest request) {
        // Unexpected failure: full detail goes to the log only, the client gets a generic message
        log.error("خطای داخلی سرور", ex);
        return new ResponseEntity<>(
                ApiResponse.error("خطای داخلی سرور", "خطای غیرمنتظره‌ای رخ داد"),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    /** Login failures are audited by AuthenticationAuditListener; everything else here is a denied access. */
    private void auditAccessDenied(WebRequest request) {
        if (!(request instanceof ServletWebRequest servletRequest)) return;
        HttpServletRequest http = servletRequest.getRequest();
        String path = http.getRequestURI().substring(http.getContextPath().length());
        if (path.startsWith(AUTH_PATH_PREFIX)) return;
        auditService.record(AuditEntry.failure(AuditAction.ACCESS_DENIED)
                .entityType("API").details(http.getMethod() + " " + http.getRequestURI()).build());
    }
}
