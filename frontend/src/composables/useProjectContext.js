/**
 * useProjectContext — 从页面提取成果上下文用于文件命名
 *
 * 用法示例：
 *   const { namingParams, getFileName } = useProjectContext({
 *     achievementType: 'PROJECT',       // 或中文 "创新项目"
 *     projectName: '大学生创新创业大赛',
 *     submitter: '张三'
 *   })
 *
 *   const fileName = getFileName({ materialType: '获奖证书', originalName: 'cert.png' })
 *   // → "创新项目-大学生创新创业大赛-张三-获奖证书.png"
 */

import { computed } from 'vue'
import {
  generateFileName,
  generateBatchZipName,
  generateAllZipName,
  getFirstPerson,
  sanitize,
  truncate
} from '@/utils/fileNaming'

/** 标签文字 → 成果类型 */
const TAG_TYPE_MAP = {
  '项目': '创新项目',
  '学科竞赛': '学科竞赛',
  '论文': '学术论文',
  '软著': '软件著作'
}

/** el-tag type → 成果中文名 */
const EL_TAG_TYPE_MAP = {
  'success': '创新项目',
  '': '学科竞赛',
  'warning': '学术论文',
  'info': '软件著作'
}

/**
 * @param {Object} context
 * @param {string} [context.achievementType] - COMPETITION | PROJECT | PAPER | SOFTWARE 或中文
 * @param {string} [context.competitionName] - 竞赛名称
 * @param {string} [context.projectName]     - 项目名称
 * @param {string} [context.journalName]     - 期刊名称
 * @param {string} [context.softwareName]    - 软件名称
 * @param {string} [context.contestants]     - 参赛选手
 * @param {string} [context.members]         - 立项人员
 * @param {string} [context.authors]         - 作者列表
 * @param {string} [context.copyrightOwner]  - 著作权人
 * @param {string} [context.applicantName]   - 申报人 (通用)
 */
export function useProjectContext(context = {}) {
  const namingParams = computed(() => ({
    ...context,
    achievementType: mapTagToType(context.achievementType || '')
  }))

  /**
   * 为指定材料类型的文件生成规范名
   * @param {Object} opts
   * @param {string} opts.materialType - 获奖证书/现场合影/其他材料/立项申报书/结题材料/投稿初稿...
   * @param {string} opts.originalName - 原始文件名
   * @param {string} [opts.ext]        - 扩展名（可选）
   */
  function getFileName({ materialType, originalName, ext } = {}) {
    return generateFileName({
      ...namingParams.value,
      materialType,
      originalName: originalName || '',
      ext: ext || ''
    })
  }

  /** 批量下载 ZIP 名 */
  function getBatchZipName() {
    return generateBatchZipName(namingParams.value)
  }

  /** 全部材料 ZIP 名 */
  function getAllZipName() {
    return generateAllZipName(namingParams.value)
  }

  return {
    namingParams,
    getFileName,
    getBatchZipName,
    getAllZipName
  }
}

/**
 * 标签文字 → 成果类型中文名
 * @param {string} tagText - el-tag__content 的文字内容
 * @returns {string}
 */
export function mapTagToType(tagText) {
  return TAG_TYPE_MAP[tagText] || tagText || '成果'
}

/**
 * el-tag type 属性 → 成果类型
 * 用于从页面已有数据推断类型
 */
export function mapElTagTypeToType(tagType) {
  return EL_TAG_TYPE_MAP[tagType] || tagType || '成果'
}
