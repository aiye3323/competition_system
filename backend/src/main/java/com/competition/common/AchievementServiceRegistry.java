package com.competition.common;

import com.competition.repository.*;
import org.springframework.stereotype.Component;

/**
 * 成果类型服务注册中心
 *
 * 统一管理各类型对应的 Repository，未来新增类型只需在此注册。
 * 用于 FileController 等需要按类型查找成果的场景。
 */
@Component
public class AchievementServiceRegistry {

    private final CompetitionRepository competitionRepository;
    private final ProjectRepository projectRepository;
    private final PaperRepository paperRepository;
    private final SoftwareRepository softwareRepository;

    public AchievementServiceRegistry(CompetitionRepository cr, ProjectRepository pr,
                                       PaperRepository par, SoftwareRepository sr) {
        this.competitionRepository = cr;
        this.projectRepository = pr;
        this.paperRepository = par;
        this.softwareRepository = sr;
    }

    /**
     * 根据类型获取对应 Repository
     */
    public Object getRepository(AchievementType type) {
        return switch (type) {
            case COMPETITION -> competitionRepository;
            case PROJECT -> projectRepository;
            case PAPER -> paperRepository;
            case SOFTWARE -> softwareRepository;
        };
    }

    /**
     * 根据类型查询成果的申报人姓名
     * @return 申报人姓名，找不到返回 null
     */
    public String getApplicantName(AchievementType type, Long achievementId) {
        return switch (type) {
            case COMPETITION -> {
                var entity = competitionRepository.findById(achievementId).orElse(null);
                yield entity != null && entity.getApplicant() != null
                        ? entity.getApplicant().getRealName() : null;
            }
            case PROJECT -> {
                var entity = projectRepository.findById(achievementId).orElse(null);
                yield entity != null && entity.getApplicant() != null
                        ? entity.getApplicant().getRealName() : null;
            }
            case PAPER -> {
                var entity = paperRepository.findById(achievementId).orElse(null);
                yield entity != null && entity.getApplicant() != null
                        ? entity.getApplicant().getRealName() : null;
            }
            case SOFTWARE -> {
                var entity = softwareRepository.findById(achievementId).orElse(null);
                yield entity != null && entity.getApplicant() != null
                        ? entity.getApplicant().getRealName() : null;
            }
        };
    }
}
