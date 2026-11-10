<template>
  <div class="app-container">
    <!-- <gen:begin key="vue-template"> -->
    <!-- 页头：一眼看清「我在哪个页面、这页能干什么」 -->
    <div class="page-head">
      <div class="page-head__main">
        <h2 class="page-title">上架任务表</h2>
        <p class="page-desc">维护上架任务表数据，可按条件筛选、编辑与导出</p>
      </div>
      <div class="page-head__meta">
        <span class="meta-pill">{{ total }} 条记录</span>
      </div>
    </div>
    <!-- 列表 -->
    <el-card shadow="never" class="table-card">
      <div slot="header" class="card-header">
        <div class="card-title">
          <span class="title">上架任务表</span>
          <span class="subtitle">共 {{ total }} 条</span>
        </div>
        <div class="card-actions">
          <el-button v-hasPermi="['wms_warehouse_wms_putaway_task:add']" type="primary" icon="el-icon-plus" size="small" @click="openDialog()">新增上架任务表</el-button>
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
        <el-table-column prop="taskNo" label="上架任务号" min-width="140" show-overflow-tooltip />
        <el-table-column prop="orderId" label="入库单" width="110" align="right" show-overflow-tooltip />
        <el-table-column prop="itemId" label="入库单明细" width="138" align="right" show-overflow-tooltip />
        <el-table-column prop="skuId" label="SKU ID" width="124" align="right" show-overflow-tooltip />
        <el-table-column prop="fromLocation" label="来源库位" width="166" align="right" show-overflow-tooltip />
        <el-table-column prop="toLocation" label="目标库位" width="110" align="right" show-overflow-tooltip />
        <el-table-column prop="batchNo" label="批次号" min-width="140" show-overflow-tooltip />
        <el-table-column prop="taskQty" label="任务数量" width="110" align="right" show-overflow-tooltip />
        <el-table-column prop="doneQty" label="已完成数量" width="110" align="right" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" min-width="260" show-overflow-tooltip />
        <el-table-column prop="assigneeId" label="作业员" width="110" align="right" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" width="170" align="center" show-overflow-tooltip />
        <el-table-column prop="finishedAt" label="完成时间" width="170" align="center" show-overflow-tooltip />
        <el-table-column prop="updateTime" label="更新时间" width="170" align="center" />
        <el-table-column label="操作" width="216" fixed="right" align="center">
          <template slot-scope="{ row }">
            <el-button type="text" @click="openDetail(row)">详情</el-button>
            <el-button v-hasPermi="['wms_warehouse_wms_putaway_task:edit']" type="text" @click="openDialog(row)">编辑</el-button>
            <el-button v-hasPermi="['wms_warehouse_wms_putaway_task:remove']" type="text" class="danger-btn" @click="handleDelete(row)">删除</el-button>
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
    <el-dialog :title="form.id ? '编辑上架任务表' : '新增上架任务表'" :visible.sync="dialogVisible" width="760px" top="6vh" class="form-dialog">
      <el-form ref="form" :model="form" :rules="rules" label-width="96px">
        <el-row :gutter="20">
          <el-col :span="12">
        <el-form-item label="上架任务号" prop="taskNo">
          <el-input v-model="form.taskNo" placeholder="请输入上架任务号" />
        </el-form-item>
          </el-col>
          <el-col :span="12">
        <el-form-item label="入库单" prop="orderId">
          <el-input-number v-model="form.orderId" controls-position="right" style="width: 100%" />
        </el-form-item>
          </el-col>
          <el-col :span="12">
        <el-form-item label="入库单明细" prop="itemId">
          <el-input-number v-model="form.itemId" controls-position="right" style="width: 100%" />
        </el-form-item>
          </el-col>
          <el-col :span="12">
        <el-form-item label="SKU ID" prop="skuId">
          <el-input-number v-model="form.skuId" controls-position="right" style="width: 100%" />
        </el-form-item>
          </el-col>
          <el-col :span="12">
        <el-form-item label="来源库位" prop="fromLocation">
          <el-input-number v-model="form.fromLocation" controls-position="right" style="width: 100%" />
        </el-form-item>
          </el-col>
          <el-col :span="12">
        <el-form-item label="目标库位" prop="toLocation">
          <el-input-number v-model="form.toLocation" controls-position="right" style="width: 100%" />
        </el-form-item>
          </el-col>
          <el-col :span="12">
        <el-form-item label="批次号" prop="batchNo">
          <el-input v-model="form.batchNo" placeholder="请输入批次号" />
        </el-form-item>
          </el-col>
          <el-col :span="12">
        <el-form-item label="任务数量" prop="taskQty">
          <el-input-number v-model="form.taskQty" controls-position="right" style="width: 100%" />
        </el-form-item>
          </el-col>
          <el-col :span="12">
        <el-form-item label="已完成数量" prop="doneQty">
          <el-input-number v-model="form.doneQty" controls-position="right" style="width: 100%" />
        </el-form-item>
          </el-col>
          <el-col :span="12">
        <el-form-item label="状态" prop="status">
          <el-input v-model="form.status" placeholder="请输入状态" />
        </el-form-item>
          </el-col>
          <el-col :span="12">
        <el-form-item label="作业员" prop="assigneeId">
          <el-input-number v-model="form.assigneeId" controls-position="right" style="width: 100%" />
        </el-form-item>
          </el-col>
          <el-col :span="12">
        <el-form-item label="创建时间" prop="createdAt">
          <el-date-picker v-model="form.createdAt" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="请选择创建时间" style="width: 100%" />
        </el-form-item>
          </el-col>
          <el-col :span="12">
        <el-form-item label="完成时间" prop="finishedAt">
          <el-date-picker v-model="form.finishedAt" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="请选择完成时间" style="width: 100%" />
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
    <el-drawer title="上架任务表详情" :visible.sync="detailVisible" size="420px">
      <div class="detail-body">
        <div class="detail-row">
          <span class="detail-label">ID</span>
          <span class="detail-value">{{ detail.id }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">上架任务号</span>
          <span class="detail-value">{{ detail.taskNo }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">入库单</span>
          <span class="detail-value">{{ detail.orderId }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">入库单明细</span>
          <span class="detail-value">{{ detail.itemId }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">SKU ID</span>
          <span class="detail-value">{{ detail.skuId }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">来源库位</span>
          <span class="detail-value">{{ detail.fromLocation }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">目标库位</span>
          <span class="detail-value">{{ detail.toLocation }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">批次号</span>
          <span class="detail-value">{{ detail.batchNo }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">任务数量</span>
          <span class="detail-value">{{ detail.taskQty }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">已完成数量</span>
          <span class="detail-value">{{ detail.doneQty }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">状态</span>
          <span class="detail-value">{{ detail.status }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">作业员</span>
          <span class="detail-value">{{ detail.assigneeId }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">创建时间</span>
          <span class="detail-value">{{ detail.createdAt }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">完成时间</span>
          <span class="detail-value">{{ detail.finishedAt }}</span>
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
  getWmsPutawayTaskList,
  exportWmsPutawayTask,
  createWmsPutawayTask,
  updateWmsPutawayTask,
  deleteWmsPutawayTask
} from '@/api/wms_putaway_task'

const defaultForm = () => ({
  id: undefined,
  taskNo: '',
  orderId: undefined,
  itemId: undefined,
  skuId: undefined,
  fromLocation: undefined,
  toLocation: undefined,
  batchNo: '',
  taskQty: undefined,
  doneQty: undefined,
  status: '',
  assigneeId: undefined,
  createdAt: undefined,
  finishedAt: undefined,
})

export default {
  name: 'WmsPutawayTask',
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
        taskNo: [{ required: true, message: '请输入上架任务号', trigger: 'blur' }, { max: 64, message: '上架任务号长度不能超过64个字符', trigger: 'blur' }],
        orderId: [{ required: true, message: '请输入入库单ID', trigger: 'blur' }],
        itemId: [{ required: true, message: '请输入入库单明细ID', trigger: 'blur' }],
        skuId: [{ required: true, message: '请输入SKU ID', trigger: 'blur' }],
        batchNo: [{ required: true, message: '请输入批次号', trigger: 'blur' }, { max: 64, message: '批次号长度不能超过64个字符', trigger: 'blur' }],
        taskQty: [{ required: true, message: '请输入任务数量', trigger: 'blur' }],
        doneQty: [{ required: true, message: '请输入已完成数量', trigger: 'blur' }],
        status: [{ required: true, message: '请输入状态', trigger: 'blur' }, { max: 32, message: '状态长度不能超过32个字符', trigger: 'blur' }],
        createdAt: [{ required: true, message: '请选择创建时间', trigger: 'change' }],
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
        const res = await getWmsPutawayTaskList(this.query)
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
        if (this.form.id) {
          await updateWmsPutawayTask(this.form.id, this.form)
        } else {
          await createWmsPutawayTask(this.form)
        }
        this.$message.success(this.form.id ? '更新成功' : '创建成功')
        this.dialogVisible = false
        this.fetchList()
      } catch (e) { /* 拦截器已提示 */ } finally {
        this.submitting = false
      }
    },
    handleDelete(row) {
      this.$confirm(`确定删除该上架任务表吗？`, '提示', { type: 'warning' })
        .then(async () => {
          await deleteWmsPutawayTask(row.id)
          this.$message.success('删除成功')
          if (this.list.length === 1 && this.query.page > 1) this.query.page--
          this.fetchList()
        })
        .catch(() => {})
    },
    async handleExport() {
      try {
        const res = await exportWmsPutawayTask(this.query)
        const blob = new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
        const url = window.URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = '上架任务表.xlsx'
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