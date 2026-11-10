<template>
  <el-steps :active="1" simple finish-status="success" class="biz-flow">
    <el-step v-for="(s, i) in steps" :key="i" :title="s" />
  </el-steps>
</template>

<script>
export default {
  name: 'BizFlowSteps',
  props: {
    steps: { type: Array, default: () => [] }
  }
}
</script>

<style scoped>
.biz-flow {
  margin-bottom: 16px;
  background: #fff;
  border: 1px solid #f0f2f7;
  border-radius: 8px;
  padding: 12px 16px;
}
.biz-flow >>> .el-step__title { font-size: 12px; }
.biz-flow >>> .is-process .el-step__title { color: #e1251b; font-weight: 600; }
.biz-flow >>> .is-process .el-step__head.is-text { background: #e1251b; border-color: #e1251b; color: #fff; }
.biz-flow >>> .is-finish .el-step__head.is-text { border-color: #e1251b; color: #e1251b; }
</style>
