<template>
  <div class="work-home-block">
    <template v-if="role === 'ADMIN'">
      <div class="overview-grid overview-grid-admin" v-loading="loading">
        <button v-for="card in cards" :key="card.key" type="button"
          :class="['overview-card', { 'overview-card--highlight': card.highlight }]"
          @click="$router.push(card.path)">
          <div class="overview-head"><h4>{{ card.title }}</h4></div>
          <p class="overview-main">{{ card.main }}</p>
          <p class="overview-sub">{{ card.sub }}</p>
        </button>
      </div>
    </template>

    <template v-else>
      <div class="overview-grid overview-grid-role" v-loading="loading">
        <button v-for="card in cards" :key="card.key" type="button"
          :class="['overview-card', { 'overview-card--highlight': card.highlight }]"
          @click="$router.push(card.path)">
          <div class="overview-head"><h4>{{ card.title }}</h4></div>
          <p class="overview-main">{{ card.main }}</p>
          <p class="overview-sub">{{ card.sub }}</p>
        </button>
      </div>

      <div v-if="role === 'PATIENT'" class="patient-rich-grid">
        <div class="guide-card">
          <h4>今日三步</h4>
          <ul class="focus-list">
            <li v-for="text in steps" :key="text">{{ text }}</li>
          </ul>
        </div>
        <div class="guide-card">
          <h4>健康提醒</h4>
          <ul class="focus-list">
            <li v-for="text in hints" :key="text">{{ text }}</li>
          </ul>
        </div>
        <div class="guide-card patient-metric-card">
          <div class="patient-metric-item">
            <span>累计上报</span><strong>{{ metrics.totalReports }}</strong>
          </div>
          <div class="patient-metric-item">
            <span>未处理预警</span><strong>{{ metrics.openAlerts }}</strong>
          </div>
          <div class="patient-metric-item">
            <span>未读反馈</span><strong>{{ metrics.unreadFeedback }}</strong>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
defineProps({
  role: { type: String, required: true },
  loading: { type: Boolean, default: false },
  cards: { type: Array, default: () => [] },
  steps: { type: Array, default: () => [] },
  hints: { type: Array, default: () => [] },
  metrics: { type: Object, default: () => ({ totalReports: 0, openAlerts: 0, unreadFeedback: 0 }) }
})
</script>

<style scoped>
.work-home-block {
  border: 1px solid rgba(31, 143, 114, 0.16); border-radius: 14px; padding: 10px;
  background: rgba(255, 255, 255, 0.36);
}
.overview-grid { display: grid; gap: 8px; align-items: stretch; }
.overview-grid-admin { grid-template-columns: repeat(4, minmax(0, 1fr)); }
.overview-grid-role { grid-template-columns: repeat(3, minmax(0, 1fr)); }
.overview-card {
  text-align: left; padding: 12px; border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.74); background: rgba(255, 255, 255, 0.5);
  cursor: pointer; min-height: 118px; display: flex; flex-direction: column; justify-content: flex-start;
}
.overview-card:hover { background: rgba(255, 255, 255, 0.64); }
.overview-card--highlight {
  border: 2px solid rgba(var(--brand-rgb), 0.8);
  background: linear-gradient(150deg, rgba(255, 255, 255, 0.84), rgba(255, 255, 255, 0.66)),
              radial-gradient(circle at 8% 10%, rgba(var(--brand-rgb), 0.28), transparent 42%);
  box-shadow: 0 0 0 2px rgba(var(--brand-rgb), 0.2);
}
.overview-head { display: flex; justify-content: space-between; gap: 8px; align-items: baseline; }
.overview-head h4 { margin: 0; font-size: 15px; color: #1f424c; }
.overview-main { margin: 8px 0 0; font-size: 13px; font-weight: 700; color: #244a53; }
.overview-sub { margin: 6px 0 0; font-size: 12px; color: #567078; line-height: 1.6; }
.guide-card {
  border-radius: 12px; padding: 10px 12px; border: 1px solid rgba(255, 255, 255, 0.74);
  background: rgba(255, 255, 255, 0.5);
}
.guide-card h4 { margin: 0; font-size: 14px; color: #204f57; }
.focus-list { margin: 6px 0 0; padding-left: 16px; display: grid; gap: 4px; font-size: 13px; line-height: 1.6; color: #48656c; }
.patient-rich-grid { display: grid; grid-template-columns: minmax(0, 6fr) minmax(0, 6fr); gap: 8px; }
.patient-metric-card { grid-column: 1 / -1; display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 8px; }
.patient-metric-item {
  border-radius: 10px; padding: 10px; border: 1px solid rgba(255, 255, 255, 0.72);
  background: rgba(255, 255, 255, 0.52); display: flex; flex-direction: column; gap: 6px;
  color: #42666f; font-size: 12px;
}
.patient-metric-item strong { font-size: 20px; color: #224a55; }

@media (max-width: 900px) {
  .overview-grid-admin, .overview-grid-role { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .patient-rich-grid { grid-template-columns: 1fr; }
  .patient-metric-card { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}
@media (max-width: 680px) {
  .overview-grid-admin, .overview-grid-role, .patient-rich-grid, .patient-metric-card { grid-template-columns: 1fr; }
}
</style>
