export const RISK_COLORS = {
  HIGH: '#d64545',
  MEDIUM: '#e38b2c',
  LOW: '#2f7d32',
  OPEN: '#d64545',
  CLOSED: '#2f7d32'
}

export const CHART_PALETTE = [
  '#2f7d32',
  '#2a6f97',
  '#e38b2c',
  '#d64545',
  '#6f63c2',
  '#2f9e89'
]

export const CHART_SPLIT_LINE = {
  lineStyle: {
    color: 'rgba(15, 23, 42, 0.1)',
    type: 'dashed'
  }
}

export const INDICATOR_LABEL_MAP = {
  BLOOD_PRESSURE: '血压',
  BLOOD_SUGAR: '血糖',
  WEIGHT: '体重',
  MEDICATION: '服药',
  HEART_RATE: '心率',
  TEMPERATURE: '体温',
  OXYGEN_SATURATION: '血氧'
}

export const toIndicatorLabel = (value) => {
  const key = String(value || '').trim()
  return INDICATOR_LABEL_MAP[key] || key
}
