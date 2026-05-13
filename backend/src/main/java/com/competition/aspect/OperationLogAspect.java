package com.competition.aspect;

import com.competition.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.security.Principal;

@Aspect
@Component
public class OperationLogAspect {

    private final OperationLogService operationLogService;

    public OperationLogAspect(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @AfterReturning(pointcut = "execution(* com.competition.controller.AuthController.login(..))", returning = "result")
    public void logLogin(JoinPoint joinPoint, Object result) {
        try {
            HttpServletRequest request = getRequest();
            String ip = getClientIp(request);
            // login success: extract userId from result
            operationLogService.log(0L, request.getParameter("username"), "LOGIN", null, null, "用户登录", ip);
        } catch (Exception ignored) {}
    }

    @AfterReturning("execution(* com.competition.controller.AuthController.register(..))")
    public void logRegister(JoinPoint joinPoint) {
        try {
            HttpServletRequest request = getRequest();
            String ip = getClientIp(request);
            operationLogService.log(0L, "GUEST", "REGISTER", null, null, "新用户注册", ip);
        } catch (Exception ignored) {}
    }

    @AfterReturning("execution(* com.competition.controller.CompetitionController.submit(..))")
    public void logCompetitionSubmit(JoinPoint joinPoint) {
        logWithAuth("SUBMIT", "COMPETITION", null, "提交竞赛成果");
    }

    @AfterReturning("execution(* com.competition.controller.CompetitionController.update(..))")
    public void logCompetitionUpdate(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        Long id = args.length > 0 ? (Long) args[0] : null;
        logWithAuth("UPDATE", "COMPETITION", id, "修改竞赛成果");
    }

    @AfterReturning("execution(* com.competition.controller.CompetitionController.delete(..))")
    public void logCompetitionDelete(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        Long id = args.length > 0 ? (Long) args[0] : null;
        logWithAuth("DELETE", "COMPETITION", id, "删除竞赛成果");
    }

    @AfterReturning("execution(* com.competition.controller.ProjectController.submit(..))")
    public void logProjectSubmit(JoinPoint joinPoint) {
        logWithAuth("SUBMIT", "PROJECT", null, "提交创新项目");
    }

    @AfterReturning("execution(* com.competition.controller.ProjectController.update(..))")
    public void logProjectUpdate(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        Long id = args.length > 0 ? (Long) args[0] : null;
        logWithAuth("UPDATE", "PROJECT", id, "修改创新项目");
    }

    @AfterReturning("execution(* com.competition.controller.ProjectController.delete(..))")
    public void logProjectDelete(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        Long id = args.length > 0 ? (Long) args[0] : null;
        logWithAuth("DELETE", "PROJECT", id, "删除创新项目");
    }

    @AfterReturning("execution(* com.competition.controller.PaperController.submit(..))")
    public void logPaperSubmit(JoinPoint joinPoint) {
        logWithAuth("SUBMIT", "PAPER", null, "提交论文成果");
    }

    @AfterReturning("execution(* com.competition.controller.PaperController.update(..))")
    public void logPaperUpdate(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        Long id = args.length > 0 ? (Long) args[0] : null;
        logWithAuth("UPDATE", "PAPER", id, "修改论文成果");
    }

    @AfterReturning("execution(* com.competition.controller.PaperController.delete(..))")
    public void logPaperDelete(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        Long id = args.length > 0 ? (Long) args[0] : null;
        logWithAuth("DELETE", "PAPER", id, "删除论文成果");
    }

    @AfterReturning("execution(* com.competition.controller.AuditController.secretaryApprove(..)) || execution(* com.competition.controller.AuditController.secretaryApproveProject(..)) || execution(* com.competition.controller.AuditController.secretaryApprovePaper(..))")
    public void logSecretaryApprove(JoinPoint joinPoint) {
        String name = joinPoint.getSignature().getName();
        String type = name.contains("Project") ? "PROJECT" : name.contains("Paper") ? "PAPER" : "COMPETITION";
        Object[] args = joinPoint.getArgs();
        Long id = args.length > 0 ? (Long) args[0] : null;
        logWithAuth("AUDIT", type, id, "秘书审核通过");
    }

    @AfterReturning("execution(* com.competition.controller.AuditController.secretaryReject(..)) || execution(* com.competition.controller.AuditController.secretaryRejectProject(..)) || execution(* com.competition.controller.AuditController.secretaryRejectPaper(..))")
    public void logSecretaryReject(JoinPoint joinPoint) {
        String name = joinPoint.getSignature().getName();
        String type = name.contains("Project") ? "PROJECT" : name.contains("Paper") ? "PAPER" : "COMPETITION";
        Object[] args = joinPoint.getArgs();
        Long id = args.length > 0 ? (Long) args[0] : null;
        logWithAuth("AUDIT", type, id, "秘书审核退回");
    }

    @AfterReturning("execution(* com.competition.controller.AuditController.leaderApprove(..)) || execution(* com.competition.controller.AuditController.leaderApproveProject(..)) || execution(* com.competition.controller.AuditController.leaderApprovePaper(..))")
    public void logLeaderApprove(JoinPoint joinPoint) {
        String name = joinPoint.getSignature().getName();
        String type = name.contains("Project") ? "PROJECT" : name.contains("Paper") ? "PAPER" : "COMPETITION";
        Object[] args = joinPoint.getArgs();
        Long id = args.length > 0 ? (Long) args[0] : null;
        logWithAuth("AUDIT", type, id, "领导审核通过并归档");
    }

    @AfterReturning("execution(* com.competition.controller.AuditController.leaderReject(..)) || execution(* com.competition.controller.AuditController.leaderRejectProject(..)) || execution(* com.competition.controller.AuditController.leaderRejectPaper(..))")
    public void logLeaderReject(JoinPoint joinPoint) {
        String name = joinPoint.getSignature().getName();
        String type = name.contains("Project") ? "PROJECT" : name.contains("Paper") ? "PAPER" : "COMPETITION";
        Object[] args = joinPoint.getArgs();
        Long id = args.length > 0 ? (Long) args[0] : null;
        logWithAuth("AUDIT", type, id, "领导审核退回");
    }

    @AfterReturning("execution(* com.competition.controller.AuthController.updateProfile(..))")
    public void logProfileUpdate(JoinPoint joinPoint) {
        logWithAuth("UPDATE", "PROFILE", null, "修改个人信息");
    }

    private void logWithAuth(String operationType, String targetType, Long targetId, String description) {
        try {
            HttpServletRequest request = getRequest();
            Long userId = 0L;
            String username = "SYSTEM";
            Object principal = request.getUserPrincipal();
            if (principal instanceof Principal) {
                // In Spring Security, principal is set by JwtAuthenticationFilter
                // We can get userId from SecurityContextHolder
                try {
                    org.springframework.security.core.Authentication auth =
                            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                    if (auth != null && auth.getPrincipal() instanceof Long) {
                        userId = (Long) auth.getPrincipal();
                    }
                } catch (Exception ignored) {}
            }
            String ip = getClientIp(request);
            operationLogService.log(userId, username, operationType, targetType, targetId, description, ip);
        } catch (Exception ignored) {}
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
