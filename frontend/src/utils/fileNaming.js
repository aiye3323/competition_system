/**
 * 科研竞赛管理系统 — 文件命名规范工具
 *
 * ## 命名规则
 *
 * ### 学科竞赛
 *   格式：学科竞赛-{竞赛名称}-{参赛选手}[-{材料类型}].ext
 *   示例：学科竞赛-全国大学生数学竞赛-张三等-获奖证书.png
 *
 * ### 创新项目
 *   格式：创新项目-{项目名称}-{立项人员}[-{材料类型}].ext
 *   示例：创新项目-智能机器人研发-李四-立项申报书.pdf
 *
 * ### 学术论文
 *   格式：学术论文-{期刊名称}-{作者列表}[-{材料类型}].ext
 *   示例：学术论文-Knowledge-Based Systems-王五等-投稿初稿.pdf
 *
 * ### 软件著作
 *   格式：软件著作-{软件名称}-{著作权人}[-{材料类型}].ext
 *   示例：软件著作-图书管理系统-赵六-申报材料.zip
 *
 * ## 特殊规则
 * - 多人：取第一位 + "等"字
 * - 文件名过长：truncate 30字符
 * - 非法字符：替换为下划线
 * - 重名：自动加 (1)(2) 序号
 */

// ============================================================
// 类型映射 & 材料关键词
// ============================================================

const TYPE_LABEL = {
  COMPETITION: '学科竞赛',
  PROJECT: '创新项目',
  PAPER: '学术论文',
  SOFTWARE: '软件著作'
}

/** 每个成果类型对应的"名称字段"提取规则 */
const NAME_FIELDS = {
  COMPETITION: ['competitionName'],
  PROJECT: ['projectName'],
  PAPER: ['journalName'],
  SOFTWARE: ['softwareName']
}

/** 每个成果类型对应的"人员字段"提取规则 */
const PERSON_FIELDS = {
  COMPETITION: ['contestants', 'contestant', 'participants'],
  PROJECT: ['members', 'projectMember', 'member'],
  PAPER: ['authors', 'author'],
  SOFTWARE: ['copyrightOwner', 'copyrightHolder']
}

/** 材料类型关键词 → 标签 */
const MATERIAL_MAP = [
  { keys: ['获奖证书', '证书', 'cert', 'award'], label: '获奖证书' },
  { keys: ['现场合影', '合影', 'photo', '照片'], label: '现场合影' },
  { keys: ['立项申报书', '申报书', 'proposal'], label: '立项申报书' },
  { keys: ['结题材料', '结题报告', '结题', 'conclusion'], label: '结题材料' },
  { keys: ['结题证书', '完成证书'], label: '结题证书' },
  { keys: ['投稿初稿', '初稿', 'draft'], label: '投稿初稿' },
  { keys: ['录用终稿', '终稿', 'final'], label: '录用终稿' },
  { keys: ['审稿意见', '审稿', 'review'], label: '审稿意见' },
  { keys: ['申报材料', '源代码', '代码', 'material'], label: '申报材料' },
  { keys: ['证书扫描件', '扫描件', 'scan'], label: '证书扫描件' }
]

// ============================================================
// 工具函数
// ============================================================

/**
 * 取第一人，多人加"等"
 * @param {string} nameList - 逗号/分号/顿号分隔的姓名
 * @returns {string} 如 "张三等" 或 "张三"
 */
export function getFirstPerson(nameList) {
  if (!nameList || !nameList.trim()) return ''
  const parts = nameList.split(/[,;，；、\s]+/).filter(Boolean)
  if (parts.length === 0) return ''
  if (parts.length === 1) return parts[0]
  return parts[0] + '等'
}

/** 清理非法字符 */
export function sanitize(text) {
  return (text || '')
    .replace(/[\\/:*?"<>|]/g, '_')
    .replace(/\s+/g, '')
    .replace(/-+/g, '-')
    .replace(/^[-_]+|[-_]+$/g, '')
}

/** 截断过长文本 */
export function truncate(text, maxLen = 30) {
  if (!text) return ''
  if (text.length <= maxLen) return text
  return text.substring(0, maxLen)
}

/** 从文件名提取扩展名（含点） */
export function getExt(name) {
  if (!name) return ''
  const i = name.lastIndexOf('.')
  return i >= 0 ? name.substring(i) : ''
}

// ============================================================
// 核心 API
// ============================================================

/**
 * 从原始文件名推断材料类型
 * @param {string} originalName
 * @returns {string}
 */
export function inferMaterialType(originalName) {
  if (!originalName) return '其他材料'
  const name = originalName.toLowerCase()
  for (const entry of MATERIAL_MAP) {
    if (entry.keys.some(k => name.includes(k))) {
      return entry.label
    }
  }
  return '其他材料'
}

/**
 * 生成规范文件名
 *
 * @param {Object} params
 * @param {string} params.achievementType - COMPETITION | PROJECT | PAPER | SOFTWARE 或中文
 * @param {string} [params.competitionName]  - 竞赛名称（学科竞赛用）
 * @param {string} [params.projectName]      - 项目名称（创新项目用）
 * @param {string} [params.journalName]      - 期刊名称（学术论文用）
 * @param {string} [params.softwareName]     - 软件名称（软件著作用）
 * @param {string} [params.contestants]      - 参赛选手
 * @param {string} [params.members]          - 立项人员
 * @param {string} [params.authors]          - 作者列表
 * @param {string} [params.copyrightOwner]   - 著作权人
 * @param {string} [params.projectName]      - 通用名称 fallback
 * @param {string} [params.userName]         - 通用人员 fallback
 * @param {string} [params.materialType]     - 材料类型（不传则自动推断）
 * @param {string} [params.originalName]     - 原始文件名
 * @param {string} [params.ext]              - 扩展名
 * @returns {string}
 */
export function generateFileName(params) {
  const type = params.achievementType || ''
  const typeName = TYPE_LABEL[type] || type || '成果'

  // 1. 提取名称字段
  let achievementName = ''
  if (type && NAME_FIELDS[type]) {
    for (const field of NAME_FIELDS[type]) {
      if (params[field]) { achievementName = params[field]; break }
    }
  }
  // fallback
  if (!achievementName) {
    achievementName = params.competitionName || params.projectName
      || params.journalName || params.softwareName
      || params.achievementName || ''
  }

  // 2. 提取人员字段
  let person = ''
  if (type && PERSON_FIELDS[type]) {
    for (const field of PERSON_FIELDS[type]) {
      if (params[field]) { person = getFirstPerson(params[field]); break }
    }
  }
  if (!person) {
    person = getFirstPerson(
      params.contestants || params.members || params.authors
      || params.copyrightOwner || params.userName || params.applicantName || ''
    )
  }

  // 3. 材料类型
  let material = params.materialType
  if (!material && params.originalName) {
    material = inferMaterialType(params.originalName)
  }
  if (!material) material = '其他材料'

  // 4. 扩展名
  let ext = params.ext || getExt(params.originalName || '')
  if (!ext.startsWith('.')) ext = '.' + ext

  // 5. 拼接
  const parts = [typeName, sanitize(truncate(achievementName)), sanitize(person), sanitize(material)]
  const base = parts.filter(Boolean).join('_')
  const clean = base.replace(/_+/g, '_').replace(/^[-_]+|[-_]+$/g, '')

  return clean + ext
}

/**
 * 批量下载 ZIP 名
 * 格式：{成果类型}-{项目名称}-{申报人}-附件材料.zip
 */
export function generateBatchZipName(params) {
  return generateFileName({
    ...params,
    materialType: '附件材料',
    ext: '.zip',
    originalName: ''
  })
}

/**
 * 批量下载 ZIP 名（全部材料）
 */
export function generateAllZipName(params) {
  return generateFileName({
    ...params,
    materialType: '全部材料',
    ext: '.zip',
    originalName: ''
  })
}

/**
 * 处理重名
 * @param {string} filename
 * @param {number} index 从1开始
 */
export function dedupFileName(filename, index) {
  const dotIdx = filename.lastIndexOf('.')
  if (dotIdx >= 0) {
    return filename.substring(0, dotIdx) + `(${index})` + filename.substring(dotIdx)
  }
  return filename + `(${index})`
}

/**
 * 为一组文件分配不重名的下载名
 * @param {Array} files
 * @param {Object} baseParams
 * @returns {Array}
 */
export function assignDownloadNames(files, baseParams) {
  const used = new Map()
  return files.map(f => {
    const params = { ...baseParams, originalName: f.originalName || '' }
    let name = generateFileName(params)
    const count = used.get(name) || 0
    if (count > 0) name = dedupFileName(name, count)
    used.set(name, count + 1)
    return { ...f, downloadName: name }
  })
}

/**
 * blob 下载
 * @param {Blob} blob
 * @param {string} fileName
 */
export function downloadBlob(blob, fileName) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = fileName
  a.style.display = 'none'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}
