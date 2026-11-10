<template>
  <div class="app-container">
    <!-- <gen:begin key="vue-template"> -->
    <!-- 页头：一眼看清「我在哪个页面、这页能干什么」 -->
    <div class="page-head">
      <div class="page-head__main">
        <h2 class="page-title">出库单主表</h2>
        <p class="page-desc">维护出库单主表数据，可按条件筛选、编辑与导出</p>
      </div>
      <div class="page-head__meta">
        <span class="meta-pill">{{ total }} 条记录</span>
      </div>
    </div>
    <!-- 列表 -->
    <biz-flow-steps :steps="['创建', '已分配', '拣货中', '已拣货', '已发运']" />
    <el-card shadow="never" class="table-card">
      <div slot="header" class="card-header">
        <div class="card-title">
          <span class="title">出库单主表</span>
          <span class="subtitle">共 {{ total }} 条</span>
        </div>
        <div class="card-actions">
          <el-button v-hasPermi="['wms_warehouse_wms_outbound_order:add']" type="primary" icon="el-icon-plus" size="small" @click="openDialog()">新增出库单主表</el-button>
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
        <el-table-column prop="orderNo" label="出库单号" min-width="140" show-overflow-tooltip />
        <el-table-column prop="orderType" label="类型" min-width="260" show-overflow-tooltip />
        <el-table-column prop="warehouseId" label="仓库" width="110" align="right" show-overflow-tooltip />
        <el-table-column prop="ownerId" label="货主" width="110" align="right" show-overflow-tooltip />
        <el-table-column prop="customerId" label="客户" width="110" align="right" show-overflow-tooltip />
        <el-table-column prop="refNo" label="外部单号" min-width="222" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" min-width="260" show-overflow-tooltip />
        <el-table-column prop="priority" label="优先级" width="110" align="right" show-overflow-tooltip />
        <el-table-column prop="carrier" label="承运商" min-width="140" show-overflow-tooltip />
        <el-table-column prop="trackingNo" label="运单号" min-width="140" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" width="170" align="center" show-overflow-tooltip />
        <el-table-column prop="updatedAt" label="更新时间" width="170" align="center" show-overflow-tooltip />
        <el-table-column prop="updateTime" label="更新时间" width="170" align="center" />
        <el-table-column label="操作" width="320" fixed="right" align="center">
          <template slot-scope="{ row }">
            <el-button type="text" @click="openDetail(row)">详情</el-button>
            <biz-outbound-actions :row="row" @done="fetchList" />
            <el-button v-hasPermi="['wms_warehouse_wms_outbound_order:edit']" type="text" @click="openDialog(row)">编辑</el-button>
            <el-button v-hasPermi="['wms_warehouse_wms_outbound_order:remove']" type="text" class="danger-btn" @click="handleDelete(row)">删除</el-button>
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
    <el-dialog :title="form.id ? '编辑出库单主表' : '新增出库单主表'" :visible.sync="dialogVisible" width="760px" top="6vh" class="form-dialog">
      <el-form ref="form" :model="form" :rules="rules" label-width="96px">
        <el-alert
          title="单据状态、承运商与运单号由业务端点（分配 / 拣货 / 发运）写入，不可手工修改"
          type="info" :closable="false" show-icon style="margin-bottom: 12px" />
        <el-row :gutter="20">
          <el-col :span="12">
        <el-form-item label="出库单号" prop="orderNo">
          <el-input v-model="form.orderNo" placeholder="请输入出库单号" />
        </el-form-item>
          </el-col>
          <el-col :span="12">
        <el-form-item label="类型" prop="orderType">
          <el-input v-model="form.orderType" placeholder="请输入类型" />
        </el-form-item>
          </el-col>
          <el-col :span="12">
        <el-form-item label="仓库" prop="warehouseId">
          <el-input-number v-model="form.warehouseId" controls-position="right" style="width: 100%" />
        </el-form-item>
          </el-col>
          <el-col :span="12">
        <el-form-item label="货主" prop="ownerId">
          <el-input-number v-model="form.ownerId" controls-position="right" style="width: 100%" />
        </el-form-item>
          </el-col>
          <el-col :span="12">
        <el-form-item label="客户" prop="customerId">
          <el-input-number v-model="form.customerId" controls-position="right" style="width: 100%" />
        </el-form-item>
          </el-col>
          <el-col :span="12">
        <el-form-item label="外部单号" prop="refNo">
          <el-input v-model="form.refNo" placeholder="请输入外部单号" />
        </el-form-item>
          </el-col>
          <el-col :span="12">
        <el-form-item label="状态" prop="status">
          <el-input v-model="form.status" placeholder="由业务端点写入" disabled />
        </el-form-item>
          </el-col>
          <el-col :span="12">
        <el-form-item label="优先级" prop="priority">
          <el-input-number v-model="form.priority" controls-position="right" style="width: 100%" />
        </el-form-item>
          </el-col>
          <el-col :span="12">
        <el-form-item label="承运商" prop="carrier">
          <el-input v-model="form.carrier" placeholder="由业务端点写入" disabled />
        </el-form-item>
          </el-col>
          <el-col :span="12">
        <el-form-item label="运单号" prop="trackingNo">
          <el-input v-model="form.trackingNo" placeholder="由业务端点写入" disabled />
        </el-form-item>
          </el-col>
          <el-col :span="12">
        <el-form-item label="创建时间" prop="createdAt">
          <el-date-picker v-model="form.createdAt" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="由业务端点写入" style="width: 100%" disabled />
        </el-form-item>
          </el-col>
          <el-col :span="12">
        <el-form-item label="更新时间" prop="updatedAt">
          <el-date-picker v-model="form.updatedAt" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="由业务端点写入" style="width: 100%" disabled />
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
    <el-drawer title="出库单主表详情" :visible.sync="detailVisible" size="420px">
      <div class="detail-body">
        <div class="detail-row">
          <span class="detail-label">ID</span>
          <span class="detail-value">{{ detail.id }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">出库单号</span>
          <span class="detail-value">{{ detail.orderNo }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">类型</span>
          <span class="detail-value">{{ detail.orderType }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">仓库</span>
          <span class="detail-value">{{ detail.warehouseId }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">货主</span>
          <span class="detail-value">{{ detail.ownerId }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">客户</span>
          <span class="detail-value">{{ detail.customerId }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">外部单号</span>
          <span class="detail-value">{{ detail.refNo }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">状态</span>
          <span class="detail-value">{{ detail.status }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">优先级</span>
          <span class="detail-value">{{ detail.priority }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">承运商</span>
          <span class="detail-value">{{ detail.carrier }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">运单号</span>
          <span class="detail-value">{{ detail.trackingNo }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">创建时间</span>
          <span class="detail-value">{{ detail.createdAt }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">更新时间</span>
          <span class="detail-value">{{ detail.updatedAt }}</span>
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
  getWmsOutboundOrderList,
  exportWmsOutboundOrder,
  createWmsOutboundOrder,
  updateWmsOutboundOrder,
  deleteWmsOutboundOrder
} from '@/api/wms_outbound_order'

// V1.1 F-02：提交前剔除只读字段（后端 ProtectedFieldGuard 携带即拒绝）
import { stripReadonly } from '@/utils/readonly'

const defaultForm = () => ({
  id: undefined,
  orderNo: '',
  orderType: '',
  warehouseId: undefined,
  ownerId: undefined,
  customerId: undefined,
  refNo: '',
  status: '',
  priority: undefined,
  carrier: '',
  trackingNo: '',
  createdAt: undefined,
  updatedAt: undefined,
})

import BizFlowSteps from '@/components/biz/BizFlowSteps.vue'
import BizOutboundActions from '@/components/biz/BizOutboundActions.vue'

export default {
  name: 'WmsOutboundOrder',
  components: { BizFlowSteps, BizOutboundActions },
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
      rules: {
        orderNo: [{ required: true, message: '请输入出库单号', trigger: 'blur' }, { max: 64, message: '出库单号长度不能超过64个字符', trigger: 'blur' }],
        orderType: [{ required: true, message: '请输入类型', trigger: 'blur' }, { max: 32, message: '类型长度不能超过32个字符', trigger: 'blur' }],
        warehouseId: [{ required: true, message: '请输入仓库ID', trigger: 'blur' }],
        ownerId: [{ required: true, message: '请输入货主ID', trigger: 'blur' }],
        refNo: [{ max: 64, message: '外部单号长度不能超过64个字符', trigger: 'blur' }],
        priority: [{ required: true, message: '请输入优先级', trigger: 'blur' }],
        // V1.1 F-02：status / carrier / trackingNo / 审计时间只读，required 同步删除
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
    async fetchList() {
      this.loading = true
      try {
        const res = await getWmsOutboundOrderList(this.query)
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
        const payload = stripReadonly(this.form, 'wms_outbound_order')
        if (this.form.id) {
          await updateWmsOutboundOrder(this.form.id, payload)
        } else {
          await createWmsOutboundOrder(payload)
        }
        this.$message.success(this.form.id ? '更新成功' : '创建成功')
        this.dialogVisible = false
        this.fetchList()
      } catch (e) { /* 拦截器已提示 */ } finally {
        this.submitting = false
      }
    },
    handleDelete(row) {
      this.$confirm(`确定删除该出库单主表吗？`, '提示', { type: 'warning' })
        .then(async () => {
          await deleteWmsOutboundOrder(row.id)
          this.$message.success('删除成功')
          if (this.list.length === 1 && this.query.page > 1) this.query.page--
          this.fetchList()
        })
        .catch(() => {})
    },
    async handleExport() {
      try {
        const res = await exportWmsOutboundOrder(this.query)
        const blob = new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
        const url = window.URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = '出库单主表.xlsx'
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