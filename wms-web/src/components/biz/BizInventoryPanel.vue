<template>
  <div class="inv-panel-wrap">
    <div class="inv-panel">
      <div class="inv-eq">
        <span class="eq-icon">Σ</span>
        <span>库存恒等式：<b>在库数量 = 已分配 + 锁定 + 可用数量</b></span>
        <span class="eq-sub">分配 / 锁定只改占用，不改在库；只有上架、拣货、盘点调整才会动在库量，且每笔都写流水。</span>
      </div>
      <div class="inv-actions">
        <el-button size="small" icon="el-icon-refresh" @click="$emit('refresh')">刷新</el-button>
        <el-button size="small" icon="el-icon-tickets" @click="goTxn">查看库存流水</el-button>
      </div>
    </div>

    <!-- V1.1 F-02：并发验收结果。数据来自 e2e 写出的报告文件，页面不自行推导 -->
    <div class="inv-verify" v-if="report">
      <span class="verify-badge" :class="report.passed ? 'ok' : 'bad'">
        {{ report.passed ? '并发验收通过' : '并发验收未通过' }}
      </span>
      <span v-if="report.concurrent.scenario100">
        100 并发：成功 {{ report.concurrent.scenario100.succeeded }} /
        拒绝 {{ report.concurrent.scenario100.rejected }} /
        流水 {{ report.concurrent.scenario100.txns }}
      </span>
      <span v-if="report.concurrent.scenario150">
        150 并发：成功 {{ report.concurrent.scenario150.succeeded }} /
        拒绝 {{ report.concurrent.scenario150.rejected }} /
        流水 {{ report.concurrent.scenario150.txns }} / 无负数
      </span>
      <span class="verify-time">{{ report.generatedAt }}</span>
    </div>
    <div class="inv-verify muted" v-else>
      并发验收报告尚未生成 —— 在 cloud 工程执行 <code>bash scripts/verify.sh</code>，本页读取真实结果（不自行推导）
    </div>
  </div>
</template>

<script>
export default {
  name: 'BizInventoryPanel',
  data() {
    return { report: null }
  },
  created() {
    this.loadReport()
  },
  methods: {
    goTxn() {
      this.$router.push('/wms_warehouse/stock/wms_inventory_txn/list')
    },
    async loadReport() {
      // public/wms-verify-report.json 由 verify.sh 调用 e2e 时生成；
      // 本地开发没跑过门禁时 404 属正常，静默降级为提示文案，不弹错误。
      try {
        const res = await fetch('/wms-verify-report.json?ts=' + Date.now())
        if (!res.ok) return
        const data = await res.json()
        if (data && data.concurrent) this.report = data
      } catch (e) { /* 未生成报告 */ }
    }
  }
}
</script>

<style scoped>
.inv-panel-wrap { margin-bottom: 16px; }
.inv-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 16px;
  background: #fff;
  border: 1px solid #f0f2f7;
  border-left: 3px solid #e1251b;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(16, 24, 40, .04);
}
.inv-eq { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; font-size: 13px; color: #374151; }
.inv-eq b { color: #e1251b; font-weight: 600; }
.eq-icon {
  width: 22px; height: 22px; border-radius: 50%; background: #fdecea; color: #e1251b;
  display: inline-flex; align-items: center; justify-content: center; font-weight: 600; font-size: 13px;
}
.eq-sub { color: #9aa3b5; font-size: 12px; }
.inv-actions { flex: 0 0 auto; }
.inv-verify {
  display: flex; align-items: center; flex-wrap: wrap; gap: 12px;
  margin-top: 8px; padding: 8px 14px;
  background: #fff; border: 1px solid #eef1f6; border-radius: 8px;
  font-size: 12px; color: #4b5563;
}
.inv-verify.muted { color: #9aa3b5; }
.inv-verify code { background: #f3f4f6; padding: 1px 5px; border-radius: 4px; }
.verify-badge { padding: 2px 8px; border-radius: 10px; font-weight: 600; }
.verify-badge.ok { background: #e8f5e9; color: #1b7f3a; }
.verify-badge.bad { background: #fdecea; color: #c62828; }
.verify-time { margin-left: auto; color: #9aa3b5; }
</style>
