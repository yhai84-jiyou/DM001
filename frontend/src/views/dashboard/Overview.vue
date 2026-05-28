<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart, BarChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import request from '../../api/request'

use([CanvasRenderer, LineChart, PieChart, BarChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

const router = useRouter()
const overview = ref<any>({})
const platformData = ref<any[]>([])
const trendData = ref<any[]>([])
const staleIssues = ref<any[]>([])
const loading = ref(true)

async function loadData() {
  loading.value = true
  try {
    const [ovRes, platRes, trendRes, staleRes] = await Promise.all([
      request.get('/reports/overview'),
      request.get('/reports/by-platform'),
      request.get('/reports/trend', { params: { days: 30 } }),
      request.get('/reports/stale-issues'),
    ]) as any[]
    overview.value = ovRes.data
    platformData.value = platRes.data
    trendData.value = trendRes.data
    staleIssues.value = staleRes.data
  } finally {
    loading.value = false
  }
}

const trendOption = ref({})
const platformPieOption = ref({})

function buildCharts() {
  trendOption.value = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['新增', '关闭'] },
    grid: { left: 40, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: trendData.value.map((d: any) => d.date.slice(5)) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      { name: '新增', type: 'line', data: trendData.value.map((d: any) => d.new), smooth: true, itemStyle: { color: '#F56C6C' } },
      { name: '关闭', type: 'line', data: trendData.value.map((d: any) => d.closed), smooth: true, itemStyle: { color: '#67C23A' } },
    ],
  }

  platformPieOption.value = {
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie', radius: ['40%', '70%'],
      data: platformData.value.map((p: any) => ({ name: p.name, value: p.total, itemStyle: { color: p.color } })),
      label: { formatter: '{b}: {c}' },
    }],
  }
}

onMounted(async () => {
  await loadData()
  buildCharts()
})

const cards = [
  { key: 'today_new', label: '今日新增', color: '#F56C6C', icon: 'Plus' },
  { key: 'today_closed', label: '今日关闭', color: '#67C23A', icon: 'Check' },
  { key: 'today_status_changed', label: '今日变化', color: '#409EFF', icon: 'Refresh' },
  { key: 'total_open', label: '待处理总数', color: '#E6A23C', icon: 'Warning' },
  { key: 'total_stale', label: '停滞问题', color: '#F56C6C', icon: 'Timer' },
]
</script>

<template>
  <div v-loading="loading">
    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col :span="4" :xs="12" v-for="card in cards" :key="card.key">
        <el-card shadow="hover" :body-style="{ padding: '16px' }">
          <div style="display: flex; justify-content: space-between; align-items: center">
            <div>
              <div style="font-size: 12px; color: #999">{{ card.label }}</div>
              <div :style="{ fontSize: '28px', fontWeight: 'bold', color: card.color, marginTop: '4px' }">
                {{ overview[card.key] ?? '-' }}
              </div>
            </div>
            <el-icon :size="32" :style="{ color: card.color }"><component :is="card.icon" /></el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col :span="16">
        <el-card>
          <template #header><span style="font-weight: bold">近30天问题趋势</span></template>
          <v-chart :option="trendOption" style="height: 300px" autoresize />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header><span style="font-weight: bold">分端问题分布</span></template>
          <v-chart :option="platformPieOption" style="height: 300px" autoresize />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="12">
        <el-card>
          <template #header><span style="font-weight: bold">各端概况</span></template>
          <vxe-table :data="platformData" stripe border>
            <vxe-column field="name" title="平台" width="140">
              <template #default="{ row }">
                <el-tag :color="row.color" style="color: #fff" size="small" effect="dark">{{ row.name }}</el-tag>
              </template>
            </vxe-column>
            <vxe-column field="total" title="总数" width="70" align="center" />
            <vxe-column field="open" title="未关闭" width="70" align="center" />
            <vxe-column field="week_new" title="本周新增" width="80" align="center" />
            <vxe-column field="week_closed" title="本周关闭" width="80" align="center" />
          </vxe-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center">
              <span style="font-weight: bold; color: #F56C6C">停滞问题（超2天无变更）</span>
              <el-tag type="danger" size="small">{{ staleIssues.length }}</el-tag>
            </div>
          </template>
          <vxe-table :data="staleIssues.slice(0, 10)" stripe border :row-config="{ isHover: true }" max-height="280">
            <vxe-column field="issue_no" title="编号" width="160">
              <template #default="{ row }">
                <a style="color: #409eff; cursor: pointer" @click="router.push(`/issues/${row.id}`)">{{ row.issue_no }}</a>
              </template>
            </vxe-column>
            <vxe-column field="title" title="标题" min-width="150" show-overflow />
            <vxe-column field="platform" title="平台" width="100" />
            <vxe-column field="status" title="状态" width="100" />
          </vxe-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>
