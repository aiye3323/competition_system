/**
 * 统一审核状态映射
 * 所有页面使用同一份状态→颜色/标签转换，确保视觉一致性
 */

const STATUS_TYPE_MAP = {
  DRAFT: 'info',
  PENDING: 'warning',
  PENDING_LEADER: 'warning',
  FIRST_REVIEW: 'info',
  FINAL_REVIEW: 'primary',
  APPROVED: 'success',
  REJECTED: 'danger',
  ARCHIVED: 'success'
}

const STATUS_LABEL_MAP = {
  DRAFT: '草稿',
  PENDING: '待审核',
  PENDING_LEADER: '待领导审核',
  FIRST_REVIEW: '一审中',
  FINAL_REVIEW: '终审中',
  APPROVED: '已通过',
  REJECTED: '已退回',
  ARCHIVED: '已归档'
}

export function statusType(status) {
  return STATUS_TYPE_MAP[status] || 'info'
}

export function statusLabel(status) {
  return STATUS_LABEL_MAP[status] || status
}

/**
 * 成果类型标签颜色映射
 */
const TYPE_TAG_MAP = {
  COMPETITION: '',       // 默认色
  PROJECT: 'success',
  PAPER: 'warning',
  SOFTWARE: 'info'
}

const TYPE_LABEL_MAP = {
  COMPETITION: '学科竞赛',
  PROJECT: '创新项目',
  PAPER: '学术论文',
  SOFTWARE: '软件著作'
}

export function typeTag(type) {
  return TYPE_TAG_MAP[type] || 'info'
}

const TYPE_COLOR_MAP = {
  COMPETITION: '#4C51BF',
  PROJECT: '#059669',
  PAPER: '#D97706',
  SOFTWARE: '#C2415C'
}

export function typeColor(type) {
  return TYPE_COLOR_MAP[type] || '#6B7280'
}

export function typeLabel(type) {
  return TYPE_LABEL_MAP[type] || type
}
