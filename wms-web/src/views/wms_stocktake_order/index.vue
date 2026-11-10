<template>
  <div class="app-container">
    <!-- <gen:begin key="vue-template"> -->
    <!-- 页头：一眼看清「我在哪个页面、这页能干什么」 -->
    <div class="page-head">
      <div class="page-head__main">
        <h2 class="page-title">盘点单表</h2>
        <p class="page-desc">维护盘点单表数据，可按条件筛选、编辑与导出</p>
      </div>
      <div class="page-head__meta">
        <span class="meta-pill">{{ total }} 条记录</span>
      </div>
    </div>
    <!-- 列表 -->
    <biz-flow-steps :steps="['创建', '已冻结', '录实盘', '已确认']" />
    <el-card shadow="never" class="table-card">
      <div slot="header" class="card-header">
        <div class="card-title">
          <span class="title">盘点单表</span>
          <span class="subtitle">共 {{ total }} 条</span>
        </div>
        <div class="card-actions">
          <!-- V1.1 F-04：创建入口收敛为一个——盘点单必须带库存快照，只能走业务端点 -->
          <biz-stocktake-actions mode="toolbar" @done="fetchList" />
          <el-button icon="el-icon-download" size="small" @click="handleExport">导出</el-button>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="list"
        class="data-table"
        stripe
        header-cell-class-name="table-head-cell"
        empty-text="暂无数据"
      >
        <el-table-column prop="id" label="ID" width="180" />
        <el-table-column prop="stocktakeNo" label="盘点单号" min-width="140" show-overflow-tooltip />
        <el-table-column prop="warehouseId" label="仓库" width="110" align="right" show-overflow-tooltip />
        <el-table-column prop="zoneId" label="盘点库区" width="194" align="right" show-overflow-tooltip />
        <el-table-column prop="stocktakeType" label="类型" min-width="222" show-overflow-tooltip
          :formatter="(row) => enumText('stocktakeType', row.stocktakeType)" />
        <el-table-column prop="status" label="状态" min-width="260" show-overflow-tooltip />
        <el-table-column prop="createdBy" label="创建人" width="110" align="right" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" width="170" align="center" show-overflow-tooltip />
        <el-table-column prop="confirmedAt" label="确认时间" width="170" align="center" show-overflow-tooltip />
        <el-table-column prop="updateTime" label="更新时间" width="170" align="center" />
        <el-table-column label="操作" width="320" fixed="right" align="center">
          <template slot-scope="{ row }">
            <el-button type="text" @click="openDetail(row)">详情</el-button>
            <biz-stocktake-actions :row="row" @done="fetchList" />
            <el-button v-hasPermi="['wms_warehouse_wms_stocktake_order:edit']" type="text" @click="openDialog(row)">编辑</el-button>
            <el-button v-hasPermi="['wms_warehouse_wms_stocktake_order:remove']" type="text" class="danger-btn" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        :page-sizes="[10, 20, 50]"
        :page-size="query.size"
        :current-page.sync="query.page"
        @size-change="handleSizeChange"
        @current-change="fetchList"
      />
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog :title="form.id ? '编辑盘点单表' : '新增盘点单表'" :visible.sync="dialogVisible" width="760px" top="6vh" class="form-dialog">
      <el-form ref="form" :model="form" :rules="rules" label-width="96px">
        <el-alert
          title="单据状态、创建人与确认时间由业务端点（创建 / 冻结 / 确认）写入，不可手工修改"
          type="info" :closable="false" show-icon style="margin-bottom: 12px" />
        <el-row :gutter="20">
          <el-col :span="12">
        <el-form-item label="盘点单号" prop="stocktakeNo">
          <el-input v-model="form.stocktakeNo" placeholder="请输入盘点单号" />
        </el-form-item>
          </el-col>
          <el-col :span="12">
        <el-form-item label="仓库" prop="warehouseId">
          <el-input-number v-model="form.warehouseId" controls-position="right" style="width: 100%" />
        </el-form-item>
          </el-col>
          <el-col :span="12">
        <el-form-item label="盘点库区" prop="zoneId">
          <el-input-number v-model="form.zoneId" controls-position="right" style="width: 100%" />
        </el-form-item>
          </el-col>
          <el-col :span="12">
        <el-form-item label="类型" prop="stocktakeType">
          <el-select v-model="form.stocktakeType" placeholder="请选择类型" clearable style="width: 100%">
            <el-option v-for="d in enumMap.stocktakeType || []" :key="d.value" :label="d.label" :value="d.value" />
          </el-select>
        </el-form-item>
          </el-col>
          <el-col :span="12">
        <el-form-item label="状态" prop="status">
          <el-input v-model="form.status" placeholder="由业务端点写入" disabled />
        </el-form-item>
          </el-col>
          <el-col :span="12">
        <el-form-item label="创建人" prop="createdBy">
          <el-input-number v-model="form.createdBy" controls-position="right" style="width: 100%" disabled placeholder="由业务端点写入" />
        </el-form-item>
          </el-col>
          <el-col :span="12">
        <el-form-item label="创建时间" prop="createdAt">
          <el-date-picker v-model="form.createdAt" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="由业务端点写入" style="width: 100%" disabled />
        </el-form-item>
          </el-col>
          <el-col :span="12">
        <el-form-item label="确认时间" prop="confirmedAt">
          <el-date-picker v-model="form.confirmedAt" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="由业务端点写入" style="width: 100%" disabled />
        </el-form-item>
          </el-col>
        </el-row>
      </el-form>


      <div slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
      </div>
    </el-dialog>

    <!-- 详情抽屉（只读）。主子表：宽抽屉 + el-tabs，tab1 主表字段，tab2..N 每张明细一张只读表；
         数据复用 /with-children 一次拉全，不逐 tab 发请求 -->
    <el-drawer title="盘点单表详情" :visible.sync="detailVisible" size="420px">
      <div class="detail-body">
        <div class="detail-row">
          <span class="detail-label">ID</span>
          <span class="detail-value">{{ detail.id }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">盘点单号</span>
          <span class="detail-value">{{ detail.stocktakeNo }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">仓库</span>
          <span class="detail-value">{{ detail.warehouseId }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">盘点库区</span>
          <span class="detail-value">{{ detail.zoneId }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">类型</span>
          <span class="detail-value">{{ enumText('stocktakeType', detail.stocktakeType) }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">状态</span>
          <span class="detail-value">{{ detail.status }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">创建人</span>
          <span class="detail-value">{{ detail.createdBy }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">创建时间</span>
          <span class="detail-value">{{ detail.createdAt }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">确认时间</span>
          <span class="detail-value">{{ detail.confirmedAt }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">创建时间</span>
          <span class="detail-value">{{ detail.createTime }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">更新时间</span>
          <span class="detail-value">{{ detail.updateTime }}</span>
        </div>
      </div>
    </el-drawer>

    <!-- <gen:end key="vue-template"> -->
  </div>
</template>

<script>
import {
  getWmsStocktakeOrderList,
  exportWmsStocktakeOrder,
  createWmsStocktakeOrder,
  updateWmsStocktakeOrder,
  deleteWmsStocktakeOrder
} from '@/api/wms_stocktake_order'

// V1.1 F-02：提交前剔除只读字段（后端 ProtectedFieldGuard 携带即拒绝）
import { stripReadonly } from '@/utils/readonly'

const defaultForm = () => ({
  id: undefined,
  stocktakeNo: '',
  warehouseId: undefined,
  zoneId: undefined,
  stocktakeType: '',
  status: '',
  createdBy: undefined,
  createdAt: undefined,
  confirmedAt: undefined,
})

import BizFlowSteps from '@/components/biz/BizFlowSteps.vue'
import BizStocktakeActions from '@/components/biz/BizStocktakeActions.vue'

export default {
  name: 'WmsStocktakeOrder',
  components: { BizFlowSteps, BizStocktakeActions },
  data() {
    return {
      // <gen:begin key="vue-data">
      loading: false,
      list: [],
      total: 0,
      query: { page: 1, size: 10 },
      dialogVisible: false,
      submitting: false,
      detailVisible: false,
      detail: {},
      // 注释枚举内联在页面里：不依赖后端字典接口，页面秒开即可回显
      enumMap: {
        stocktakeType: [{value: '0', label: ':全盘'}, {value: '1', label: '抽盘'}, {value: '2', label: '循环盘点'}]
      },
      rules: {
        stocktakeNo: [{ required: true, message: '请输入盘点单号', trigger: 'blur' }, { max: 64, message: '盘点单号长度不能超过64个字符', trigger: 'blur' }],
        warehouseId: [{ required: true, message: '请输入仓库ID', trigger: 'blur' }],
        stocktakeType: [{ max: 32, message: '类型长度不能超过32个字符', trigger: 'blur' }],
        // V1.1 F-02：status / createdAt / confirmedAt 只读，required 同步删除
      },
      form: defaultForm()
      // <gen:end key="vue-data">
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
    // <gen:begin key="vue-methods">
    // 枚举翻译：值可能是数字也可能是字符串，统一按字符串比，避免 1 与 '1' 对不上
    enumText(field, value) {
      const hit = (this.enumMap[field] || []).find(d => String(d.value) === String(value))
      return hit ? hit.label : (value === undefined || value === null || value === '' ? '' : value)
    },
    async fetchList() {
      this.loading = true
      try {
        const res = await getWmsStocktakeOrderList(this.query)
        this.list = res.data.list || []
        this.total = res.data.total || 0
      } catch (e) {
        this.list = []
        this.total = 0
      } finally {
        this.loading = false
      }
    },
    handleSearch() {
      this.query.page = 1
      this.fetchList()
    },
    handleReset() {
      this.query = { page: 1, size: this.query.size }
      this.fetchList()
    },
    handleSizeChange(size) {
      this.query.size = size
      this.query.page = 1
      this.fetchList()
    },
    openDetail(row) {
      this.detail = row || {}
      this.detailVisible = true
    },
    openDialog(row) {
      this.form = row ? { ...defaultForm(), ...row } : defaultForm()
      this.dialogVisible = true
      this.$nextTick(() => {
        if (this.$refs.form) this.$refs.form.clearValidate()
      })
    },
    async submitForm() {
      const valid = await this.$refs.form.validate().catch(() => false)
      if (!valid) return
      this.submitting = true
      try {
        const payload = stripReadonly(this.form, 'wms_stocktake_order')
        if (this.form.id) {
          await updateWmsStocktakeOrder(this.form.id, payload)
        } else {
          await createWmsStocktakeOrder(payload)
        }
        this.$message.success(this.form.id ? '更新成功' : '创建成功')
        this.dialogVisible = false
        this.fetchList()
      } catch (e) { /* 拦截器已提示 */ } finally {
        this.submitting = false
      }
    },
    handleDelete(row) {
      this.$confirm(`确定删除该盘点单表吗？`, '提示', { type: 'warning' })
        .then(async () => {
          await deleteWmsStocktakeOrder(row.id)
          this.$message.success('删除成功')
          if (this.list.length === 1 && this.query.page > 1) this.query.page--
          this.fetchList()
        })
        .catch(() => {})
    },
    async handleExport() {
      try {
        const res = await exportWmsStocktakeOrder(this.query)
        const blob = new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
        const url = window.URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = '盘点单表.xlsx'
        a.click()
        window.URL.revokeObjectURL(url)
      } catch (e) { /* 拦截器已提示 */ }
    }
    // <gen:end key="vue-methods">
    // ===== 以下为手写区：自定义方法写在这里，重新生成不会被覆盖 =====
  }
}
</script>

<style scoped>
/* <gen:begin key="vue-style"> */
.app-container {
  padding: 0;
  background: transparent;
}

/* ===== 页头 ===== */
.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #111827;
  letter-spacing: 0.2px;
}

.page-desc {
  margin: 6px 0 0;
  font-size: 12px;
  color: #8a94a6;
}

.page-head__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-top: 4px;
}

.meta-pill {
  padding: 4px 12px;
  border-radius: 20px;
  background: #eef2ff;
  color: #4d7cfe;
  font-size: 12px;
  font-weight: 500;
}

/* ===== 卡片 ===== */
.filter-card,
.table-card {
  border: none;
  border-radius: 12px;
  box-shadow: 0 1px 2px rgba(16, 24, 40, 0.04), 0 6px 20px rgba(16, 24, 40, 0.05);
}

.filter-card {
  margin-bottom: 16px;
}

.filter-card >>> .el-card__body {
  padding: 18px 18px 4px;
}

.filter-card >>> .el-form-item {
  margin-right: 16px;
  margin-bottom: 14px;
}

.filter-card >>> .el-form-item__label {
  color: #5a6478;
  font-size: 13px;
}

.table-card >>> .el-card__header {
  padding: 16px 18px;
  border-bottom: 1px solid #eef1f7;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  display: flex;
  align-items: baseline;
}

.card-title .title {
  font-size: 15px;
  font-weight: 600;
  color: #111827;
}

.card-title .subtitle {
  margin-left: 10px;
  font-size: 12px;
  color: #909399;
}

.card-actions >>> .el-button + .el-button {
  margin-left: 8px;
}

/* ===== 表格 ===== */
.data-table >>> .table-head-cell {
  background: #f7f9fc !important;
  color: #5c6270;
  font-weight: 600;
}

.data-table >>> td {
  padding: 11px 0;
}

.data-table >>> .el-table__row:hover > td {
  background: #f5f9ff;
}

.data-table >>> .el-button--text {
  padding: 4px 6px;
}

/* ===== 分页 ===== */
.pagination {
  margin-top: 18px;
  text-align: right;
}

.danger-btn {
  color: #f56c6c;
}

.child-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.child-tip {
  color: #909399;
  font-size: 12px;
}

.import-summary {
  margin-bottom: 8px;
}

.import-tpl-tip {
  margin-bottom: 10px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 12px;
  line-height: 1.7;
  color: #606266;
}

.import-summary .ok {
  color: #67c23a;
}

.import-summary .bad {
  color: #f56c6c;
}

.import-fail-box {
  max-height: 200px;
  overflow: auto;
  padding: 8px;
  background: #fdf6ec;
  border-radius: 4px;
}

.import-fail-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
  font-weight: 600;
}

.import-fail {
  color: #f56c6c;
  font-size: 12px;
  line-height: 20px;
}

.import-fail-more {
  color: #909399;
  font-size: 12px;
}

.child-table {
  margin-bottom: 8px;
}

.detail-body {
  padding: 0 20px;
}

.detail-row {
  display: flex;
  padding: 8px 0;
  border-bottom: 1px solid #ebeef5;
  font-size: 13px;
}

.detail-label {
  width: 110px;
  color: #909399;
  flex-shrink: 0;
}

.detail-value {
  color: #303133;
  word-break: break-all;
}

.import-result {
  margin-top: 12px;
  font-size: 13px;
}

.import-fail {
  color: #f56c6c;
}
</style>
/* <gen:end key="vue-style"> */