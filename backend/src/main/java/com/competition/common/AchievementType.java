package com.competition.common;

/**
 * 科研成果类型枚举 — 未来新增类型只需在此枚举中添加一项
 *
 * 扩展步骤：
 * 1. 在此枚举添加类型常量
 * 2. 创建对应的 Entity (extends BaseEntity)
 * 3. 创建 Repository、Service、Controller
 * 4. 在 FileStorageService 中注册文件关联
 * 5. 前端在 uploadConfig.js 添加上传区域配置
 * 6. 前端在 router 添加路由
 */
public enum AchievementType {

    COMPETITION("学科竞赛", "t_competition", "/competition"),
    PROJECT("创新项目", "t_project", "/project"),
    PAPER("学术论文", "t_paper", "/paper"),
    SOFTWARE("软件著作", "t_software", "/software");

    private final String displayName;
    private final String tableName;
    private final String routePath;

    AchievementType(String displayName, String tableName, String routePath) {
        this.displayName = displayName;
        this.tableName = tableName;
        this.routePath = routePath;
    }

    public String getDisplayName() { return displayName; }
    public String getTableName() { return tableName; }
    public String getRoutePath() { return routePath; }

    public static AchievementType fromString(String type) {
        if (type == null) return null;
        for (AchievementType t : values()) {
            if (t.name().equalsIgnoreCase(type) || t.displayName.equals(type)) {
                return t;
            }
        }
        return null;
    }
}
