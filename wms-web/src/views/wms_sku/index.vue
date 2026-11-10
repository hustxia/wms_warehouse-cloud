<template>
  <div class="app-container">
    <!-- <gen:begin key="vue-template"> -->
    <!-- 页头：一眼看清「我在哪个页面、这页能干什么」 -->
    <div class="page-head">
      <div class="page-head__main">
        <h2 class="page-title">商品SKU表</h2>
        <p class="page-desc">维护商品SKU表数据，可按条件筛选、编辑与导出</p>
      </div>
      <div class="page-head__meta">
        <span class="meta-pill">{{ total }} 条记录</span>
      </div>
    </div>
    <!-- 列表 -->
    <el-card shadow="never" class="table-card">
      <div slot="header" class="card-header">
        <div class="card-title">
          <span class="title">商品SKU表</span>
          <span class="subtitle">共 {{ total }} 条</span>
        </div>
        <div class="card-actions">
          <el-button v-hasPermi="['wms_warehouse_wms_sku:add']" type="primary" icon="el-icon-plus" size="small" @click="openDialog()">新增商品SKU表</el-button>
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
        <el-table-column prop="skuCode" label="SKU编码" min-width="140" show-overflow-tooltip />
        <el-table-column prop="skuName" label="商品名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="barcode" label="条码" min-width="140" show-overflow-tooltip />
        <el-table-column prop="categoryId" label="分类" width="110" align="right" show-overflow-tooltip />
        <el-table-column prop="spec" label="规格" min-width="140" show-overflow-tooltip />
        <el-table-column prop="unit" label="计量单位" min-width="140" show-overflow-tooltip />
        <el-table-column prop="weight" label="重量" width="124" align="right" show-overflow-tooltip />
        <el-table-column prop="volume" label="体积" width="124" align="right" show-overflow-tooltip />
        <el-table-column prop="shelfLife" label="保质期" width="124" align="right" show-overflow-tooltip />
        <el-table-column prop="isBatch" label="是否批次管理" min-width="208" show-overflow-tooltip
          :formatter="(row) => enumText('isBatch', row.isBatch)" />
        <el-table-column prop="isSerial" label="是否序列号管理" min-width="222" show-overflow-tooltip
          :formatter="(row) => enumText('isSerial', row.isSerial)" />
        <el-table-column prop="status" label="状态" min-width="180" show-overflow-tooltip
          :formatter="(row) => enumText('status', row.status)" />
        <el-table-column prop="createdAt" label="创建时间" width="170" align="center" show-overflow-tooltip />
        <el-table-column prop="updateTime" label="更新时间" width="170" align="center" />
        <el-table-column label="操作" width="216" fixed="right" align="center">
          <template slot-scope="{ row }">
            <el-button type="text" @click="openDetail(row)">详情</el-button>
            <el-button v-hasPermi="['wms_warehouse_wms_sku:edit']" type="text" @click="openDialog(row)">编辑</el-button>
            <el-button v-hasPermi="['wms_warehouse_wms_sku:remove']" type="text" class="danger-btn" @click="handleDelete(row)">删除</el-button>
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
    <el-dialog :title="form.id ? '编辑商品SKU表' : '新增商品SKU表'" :visible.sync="dialogVisible" width="760px" top="6vh" class="form-dialog">
      <el-form ref="form" :model="form" :rules="rules" label-width="96px">
        <el-row :gutter="20">
          <el-col :span="12">
        <el-form-item label="SKU编码" prop="skuCode">
          <el-input v-model="form.skuCode" placeholder="请输入SKU编码" />
        </el-form-item>
          </el-col>
          <el-col :span="12">
        <el-form-item label="商品名称" prop="skuName">
          <el-input v-model="form.skuName" placeholder="请输入商品名称" />
        </el-form-item>
          </el-col>
          <el-col :span="12">
        <el-form-item label="条码" prop="barcode">
          <el-input v-model="form.barcode" placeholder="请输入条码" />
        </el-form-item>
          </el-col>
          <el-col :span="12">
        <el-form-item label="分类" prop="categoryId">
          <el-input-number v-model="form.categoryId" controls-position="right" style="width: 100%" />
        </el-form-item>
          </el-col>
          <el-col :span="12">
        <el-form-item label="规格" prop="spec">
          <el-input v-model="form.spec" placeholder="请输入规格" />
        </el-form-item>
          </el-col>
          <el-col :span="12">
        <el-form-item label="计量单位" prop="unit">
          <el-input v-model="form.unit" placeholder="请输入计量单位" />
        </el-form-item>
          </el-col>
          <el-col :span="12">
        <el-form-item label="重量" prop="weight">
          <el-input-number v-model="form.weight" controls-position="right" style="width: 100%" />
        </el-form-item>
          </el-col>
          <el-col :span="12">
        <el-form-item label="体积" prop="volume">
          <el-input-number v-model="form.volume" controls-position="right" style="width: 100%" />
        </el-form-item>
          </el-col>
          <el-col :span="12">
        <el-form-item label="保质期" prop="shelfLife">
          <el-input-number v-model="form.shelfLife" controls-position="right" style="width: 100%" />
        </el-form-item>
          </el-col>
          <el-col :span="12">
        <el-form-item label="是否批次管理" prop="isBatch">
          <el-select v-model="form.isBatch" placeholder="请选择是否批次管理" clearable style="width: 100%">
            <el-option v-for="d in enumMap.isBatch || []" :key="d.value" :label="d.label" :value="d.value" />
          </el-select>
        </el-form-item>
          </el-col>
          <el-col :span="12">
        <el-form-item label="是否序列号管理" prop="isSerial">
          <el-select v-model="form.isSerial" placeholder="请选择是否序列号管理" clearable style="width: 100%">
            <el-option v-for="d in enumMap.isSerial || []" :key="d.value" :label="d.label" :value="d.value" />
          </el-select>
        </el-form-item>
          </el-col>
          <el-col :span="12">
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择状态" clearable style="width: 100%">
            <el-option v-for="d in enumMap.status || []" :key="d.value" :label="d.label" :value="d.value" />
          </el-select>
        </el-form-item>
          </el-col>
          <el-col :span="12">
        <el-form-item label="创建时间" prop="createdAt">
          <el-date-picker v-model="form.createdAt" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="请选择创建时间" style="width: 100%" />
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
    <el-drawer title="商品SKU表详情" :visible.sync="detailVisible" size="420px">
      <div class="detail-body">
        <div class="detail-row">
          <span class="detail-label">ID</span>
          <span class="detail-value">{{ detail.id }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">SKU编码</span>
          <span class="detail-value">{{ detail.skuCode }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">商品名称</span>
          <span class="detail-value">{{ detail.skuName }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">条码</span>
          <span class="detail-value">{{ detail.barcode }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">分类</span>
          <span class="detail-value">{{ detail.categoryId }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">规格</span>
          <span class="detail-value">{{ detail.spec }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">计量单位</span>
          <span class="detail-value">{{ detail.unit }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">重量</span>
          <span class="detail-value">{{ detail.weight }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">体积</span>
          <span class="detail-value">{{ detail.volume }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">保质期</span>
          <span class="detail-value">{{ detail.shelfLife }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">是否批次管理</span>
          <span class="detail-value">{{ enumText('isBatch', detail.isBatch) }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">是否序列号管理</span>
          <span class="detail-value">{{ enumText('isSerial', detail.isSerial) }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">状态</span>
          <span class="detail-value">{{ enumText('status', detail.status) }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">创建时间</span>
          <span class="detail-value">{{ detail.createdAt }}</span>
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
  getWmsSkuList,
  exportWmsSku,
  createWmsSku,
  updateWmsSku,
  deleteWmsSku
} from '@/api/wms_sku'
// 字典兜底、唯一性查重、导入失败明细下载都直接用通用 request，不依赖宿主是否提供专用 api 模块
import request from '@/utils/request'

const defaultForm = () => ({
  id: undefined,
  skuCode: '',
  skuName: '',
  barcode: '',
  categoryId: undefined,
  spec: '',
  unit: '',
  weight: undefined,
  volume: undefined,
  shelfLife: undefined,
  isBatch: '',
  isSerial: '',
  status: '',
  createdAt: undefined,
})

export default {
  name: 'WmsSku',
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
      dicts: { sys_status: [] },
      // 注释枚举内联在页面里：不依赖后端字典接口，页面秒开即可回显
      enumMap: {
        isBatch: [{value: 1, label: '是'}, {value: 0, label: '否'}],
        isSerial: [{value: 1, label: '是'}, {value: 0, label: '否'}],
        status: [{value: 1, label: '启用'}, {value: 0, label: '停用'}]
      },
      rules: {
        skuCode: [{ required: true, message: '请输入SKU编码', trigger: 'blur' }, { max: 64, message: 'SKU编码长度不能超过64个字符', trigger: 'blur' }],
        skuName: [{ required: true, message: '请输入商品名称', trigger: 'blur' }, { max: 255, message: '商品名称长度不能超过255个字符', trigger: 'blur' }],
        barcode: [{ max: 64, message: '条码长度不能超过64个字符', trigger: 'blur' }],
        spec: [{ max: 128, message: '规格长度不能超过128个字符', trigger: 'blur' }],
        unit: [{ max: 16, message: '计量单位长度不能超过16个字符', trigger: 'blur' }],
        isBatch: [{ required: true, message: '请选择是否批次管理', trigger: 'change' }],
        isSerial: [{ required: true, message: '请选择是否序列号管理', trigger: 'change' }],
        status: [{ required: true, message: '请选择状态', trigger: 'change' }],
        createdAt: [{ required: true, message: '请选择创建时间', trigger: 'change' }],
      },
      form: defaultForm()
      // <gen:end key="vue-data">
    }
  },
  created() {
    this.loadDicts()
    this.fetchList()
  },
  methods: {
    // <gen:begin key="vue-methods">
    async loadDicts() {
      const codes = ['sys_status']
      await Promise.all(codes.map(async code => {
        try {
          // 直接走通用 request，不依赖宿主工程是否提供 @/api/dict；
          // 宿主没有字典接口时这里会抛错，下面 catch 把选项降级为空数组，页面照常可用
          const res = await request({ url: '/api/demo/system/dict/data/type/' + code, method: 'get' })
          this.$set(this.dicts, code, (res && res.data) || [])
        } catch (e) {
          this.$set(this.dicts, code, [])
        }
      }))
    },
    dictText(code, value) {
      const hit = (this.dicts[code] || []).find(d => String(d.value) === String(value))
      return hit ? hit.label : (value === undefined || value === null ? '' : value)
    },
    // 枚举翻译：值可能是数字也可能是字符串，统一按字符串比，避免 1 与 '1' 对不上
    enumText(field, value) {
      const hit = (this.enumMap[field] || []).find(d => String(d.value) === String(value))
      return hit ? hit.label : (value === undefined || value === null || value === '' ? '' : value)
    },
    async fetchList() {
      this.loading = true
      try {
        const res = await getWmsSkuList(this.query)
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
          await updateWmsSku(this.form.id, this.form)
        } else {
          await createWmsSku(this.form)
        }
        this.$message.success(this.form.id ? '更新成功' : '创建成功')
        this.dialogVisible = false
        this.fetchList()
      } catch (e) { /* 拦截器已提示 */ } finally {
        this.submitting = false
      }
    },
    handleDelete(row) {
      this.$confirm(`确定删除该商品SKU表吗？`, '提示', { type: 'warning' })
        .then(async () => {
          await deleteWmsSku(row.id)
          this.$message.success('删除成功')
          if (this.list.length === 1 && this.query.page > 1) this.query.page--
          this.fetchList()
        })
        .catch(() => {})
    },
    async handleExport() {
      try {
        const res = await exportWmsSku(this.query)
        const blob = new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
        const url = window.URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = '商品SKU表.xlsx'
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