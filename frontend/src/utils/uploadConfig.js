/**
 * 科研竞赛管理系统 — 上传区域配置
 *
 * 规则：上传到哪个区域，文件就叫什么名字
 * 示例：上传到"获奖证书"区域 → 显示"获奖证书.png"
 */

/**
 * @typedef {Object} UploadAreaConfig
 * @property {string}  key         - 唯一标识
 * @property {string}  label       - 显示标签（即 materialType，也是显示名）
 * @property {string}  accept      - MIME 类型，逗号分隔
 * @property {string}  acceptExt   - 文件扩展名，用于前端校验
 * @property {number}  [maxCount]  - 最大文件数（不设 = 无限制）
 * @property {number}  [maxSize]   - 单文件最大 MB
 * @property {string}  [hint]      - 上传提示文字
 */

// ============================================================
// 一、学科竞赛
// ============================================================
export const competitionAreas = [
  {
    key: 'cert',
    label: '获奖证书',
    accept: 'image/jpeg,image/png',
    acceptExt: '.jpg,.jpeg,.png',
    maxCount: 5,
    maxSize: 10,
    hint: '支持 JPG、PNG 格式，单个文件不超过10MB（最多上传5张）'
  },
  {
    key: 'photo',
    label: '现场合影',
    accept: 'image/jpeg,image/png',
    acceptExt: '.jpg,.jpeg,.png',
    maxCount: 5,
    maxSize: 10,
    hint: '支持 JPG、PNG 格式，单个文件不超过10MB（最多上传5张）'
  },
  {
    key: 'other',
    label: '其他材料',
    accept: 'image/jpeg,image/png,application/pdf,application/zip,application/x-zip-compressed',
    acceptExt: '.jpg,.jpeg,.png,.pdf,.zip',
    maxSize: 10,
    hint: '支持 JPG、PNG、PDF、ZIP 格式，单个文件不超过10MB'
  }
]

// ============================================================
// 二、大学生创新训练计划项目
// ============================================================
export const projectAreas = [
  {
    key: 'proposal',
    label: '立项申报书',
    accept: 'application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/pdf',
    acceptExt: '.doc,.docx,.pdf',
    maxCount: 1,
    maxSize: 10,
    hint: '支持 DOC、DOCX、PDF 格式，单个文件不超过10MB'
  },
  {
    key: 'conclusion',
    label: '结题材料',
    accept: 'application/zip,application/x-zip-compressed',
    acceptExt: '.zip',
    maxCount: 1,
    maxSize: 20,
    hint: '支持 ZIP 格式（包含结题报告、成果材料等），不超过20MB'
  },
  {
    key: 'projCert',
    label: '结题证书',
    accept: 'image/jpeg,image/png,application/pdf',
    acceptExt: '.jpg,.jpeg,.png,.pdf',
    maxCount: 1,
    maxSize: 10,
    hint: '支持 JPG、PNG、PDF 格式，单个文件不超过10MB'
  }
]

// ============================================================
// 三、学术论文
// ============================================================
export const paperAreas = [
  {
    key: 'draft',
    label: '投稿初稿',
    accept: 'application/pdf',
    acceptExt: '.pdf',
    maxCount: 1,
    maxSize: 10,
    hint: '支持 PDF 格式，单个文件不超过10MB'
  },
  {
    key: 'final',
    label: '录用终稿',
    accept: 'application/pdf',
    acceptExt: '.pdf',
    maxCount: 1,
    maxSize: 10,
    hint: '支持 PDF 格式，单个文件不超过10MB'
  },
  {
    key: 'review',
    label: '审稿意见及回复',
    accept: 'application/pdf',
    acceptExt: '.pdf',
    maxCount: 1,
    maxSize: 10,
    hint: '支持 PDF 格式，单个文件不超过10MB'
  }
]

// ============================================================
// 四、软件著作
// ============================================================
export const softwareAreas = [
  {
    key: 'material',
    label: '申报材料',
    accept: 'application/zip,application/x-zip-compressed',
    acceptExt: '.zip',
    maxCount: 1,
    maxSize: 20,
    hint: '支持 ZIP 格式（包含源代码、用户手册等），不超过20MB'
  },
  {
    key: 'swCert',
    label: '证书扫描件',
    accept: 'image/png',
    acceptExt: '.png',
    maxCount: 1,
    maxSize: 10,
    hint: '支持 PNG 格式，单个文件不超过10MB'
  }
]

// ============================================================
// 工具函数
// ============================================================

/**
 * 根据 MIME type 列表生成 accept 字符串
 */
export function buildAccept(mimeTypes) {
  return mimeTypes.join(',')
}

/**
 * 校验文件是否符合区域限制
 * @returns {{ valid: boolean, message?: string }}
 */
export function validateFile(file, areaConfig) {
  // 大小检查
  const maxBytes = (areaConfig.maxSize || 10) * 1024 * 1024
  if (file.size > maxBytes) {
    return { valid: false, message: `文件大小不能超过 ${areaConfig.maxSize || 10}MB` }
  }

  // 类型检查
  const allowedTypes = areaConfig.accept.split(',').map(s => s.trim())
  if (allowedTypes.length > 0 && !allowedTypes.includes(file.type)) {
    // MIME 不匹配时检查扩展名
    const ext = '.' + (file.name.split('.').pop() || '').toLowerCase()
    const allowedExts = (areaConfig.acceptExt || '').split(',').map(s => s.trim().toLowerCase())
    if (allowedExts.length > 0 && !allowedExts.includes(ext)) {
      return { valid: false, message: `不支持的文件格式，支持：${areaConfig.acceptExt}` }
    }
  }

  return { valid: true }
}
