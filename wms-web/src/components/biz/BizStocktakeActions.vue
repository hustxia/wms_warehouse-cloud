<template>
  <span class="biz-actions">
    <!-- 工具栏模式：新建盘点单 -->
    <template v-if="mode === 'toolbar'">
      <el-button type="primary" size="small" icon="el-icon-plus" class="biz-create" @click="openCreate">新建盘点单</el-button>
    </template>

    <!-- 行内模式：冻结 / 录实盘 / 确认调整 -->
    <template v-else>
      <el-button v-if="row.status === 'CREATED'" type="text" size="mini" class="biz-link" @click="doFreeze">冻结</el-button>
      <el-button v-if="row.status === 'FROZEN'" type="text" size="mini" class="biz-link" @click="openItems">录实盘</el-button>
      <el-button v-if="row.status === 'FROZEN'" type="text" size="mini" class="biz-link" @click="doConfirm">确认调整</el-button>
      <span v-if="row.status === 'CONFIRMED'" class="biz-muted">已确认</span>
    </template>

    <!-- 新建盘点单：按仓库 / 库位圈定快照范围 -->
    <el-dialog title="新建盘点单" :visible.sync="createVisible" width="520px" :close-on-click-modal="false" append-to-body>
      <div class="biz-hint">按范围快照当前账面数量；之后库存变动不影响本次差异。冻结期间该范围不可再被分配。</div>
      <el-form :model="createForm" label-width="100px" size="small">
        <el-form-item label="仓库 ID" required>
          <el-input-number v-model="createForm.warehouseId" :min="1" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="库位">
          <el-select v-model="createForm.locationId" clearable filterable placeholder="全部库位（全仓盘点）" style="width: 100%">
            <el-option v-for="loc in locOptions" :key="loc.id" :label="loc.locationCode + ' · 仓' + loc.warehouseId" :value="loc.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="盘点类型"><el-input v-model="createForm.stocktakeType" placeholder="如：抽盘 / 全盘" /></el-form-item>
      </el-form>
      <div slot="footer">
        <el-button size="small" @click="createVisible = false">取消</el-button>
        <el-button size="small" type="primary" :loading="submitting" @click="submitCreate">创建并生成明细</el-button>
      </div>
    </el-dialog>

    <!-- 录实盘：逐行输入，差异由数据库生成列计算 -->
    <el-dialog title="录入实盘数量" :visible.sync="itemsVisible" width="860px" :close-on-click-modal="false" append-to-body>
      <div class="biz-hint">
        盘点单 <b>{{ row.stocktakeNo }}</b>（{{ statusLabel(row.status) }}）。
        差异 = 实盘 − 账面；确认调整前可反复修改，未录入的行不参与调整。
      </div>
      <el-table :data="itemRows" size="mini" border v-loading="loading">
        <el-table-column prop="skuId" label="SKU" width="100" />
        <el-table-column prop="locationId" label="库位" width="100" />
        <el-table-column prop="batchNo" label="批次" min-width="110">
          <template slot-scope="{ row: it }">{{ it.batchNo || '不管理批次' }}</template>
        </el-table-column>
        <el-table-column prop="systemQty" label="账面数量" width="100" align="right" />
        <el-table-column prop="diffQty" label="差异" width="90" align="right">
          <template slot-scope="{ row: it }">
            <span :class="qty(it.diffQty) < 0 ? 'neg' : 'pos'">{{ it.countedQty === null || it.countedQty === undefined ? '—' : it.diffQty }}</span>
          </template>
        </el-table-column>
        <el-table-column label="实盘数量" width="160" align="right">
          <template slot-scope="{ row: it }">
            <el-input-number v-model="it.countedQty" :min="0" :precision="3" :step="1" controls-position="right"
              size="mini" style="width: 130px" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="center">
          <template slot-scope="{ row: it }">
            <el-button type="text" size="mini" class="biz-link" :loading="it.saving" @click="saveCount(it)">保存</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div slot="footer">
        <el-button size="small" @click="itemsVisible = false">关闭</el-button>
        <el-button size="small" type="primary" @click="doConfirm">确认调整（先解冻再调账）</el-button>
      </div>
    </el-dialog>
  </span>
</template>

<script>
import {
  createStocktake, freezeStocktake, confirmStocktake, countStocktakeItem,
  stocktakeItems, locations, newRequestId, statusLabel, qty
} from '@/api/wms_biz'

export default {
  name: 'BizStocktakeActions',
  props: {
    row: { type: Object, default: () => ({}) },
    mode: { type: String, default: 'row' }
  },
  data() {
    return {
      createVisible: false,
      itemsVisible: false,
      createForm: { warehouseId: 1001, locationId: null, stocktakeType: '抽盘' },
      locOptions: [],
      itemRows: [],
      loading: false,
      submitting: false
    }
  },
  methods: {
    statusLabel,
    qty,
    async openCreate() {
      this.createVisible = true
      if (!this.locOptions.length) {
        try { this.locOptions = await locations() } catch (e) { this.locOptions = [] }
      }
    },
    async submitCreate() {
      if (!this.createForm.warehouseId) return this.$message.warning('请填写仓库 ID')
      this.submitting = true
      try {
        const res = await createStocktake({
          requestId: newRequestId('stocktake-create'),
          warehouseId: this.createForm.warehouseId,
          locationId: this.createForm.locationId || undefined,
          stocktakeType: this.createForm.stocktakeType
        })
        this.$message.success('已生成盘点单，快照明细 ' + qty(res.data.itemCount) + ' 行')
        this.createVisible = false
        this.$emit('done')
      } catch (e) { /* 拦截器已提示 */ } finally {
        this.submitting = false
      }
    },
    async doFreeze() {
      this.submitting = true
      try {
        const res = await freezeStocktake(this.row.id, { requestId: newRequestId('stocktake-freeze') })
        this.$message.success('已冻结 ' + qty(res.data.lockedCount) + ' 行库存')
        this.$emit('done')
      } catch (e) { /* 拦截器已提示 */ } finally {
        this.submitting = false
      }
    },
    async openItems() {
      this.loading = true
      this.itemsVisible = true
      try {
        const rows = await stocktakeItems(this.row.id)
        this.itemRows = rows.map(it => ({
          ...it,
          countedQty: it.countedQty === null || it.countedQty === undefined ? qty(it.systemQty) : qty(it.countedQty),
          saving: false,
          _rid: newRequestId('count-' + it.id)
        }))
      } catch (e) {
        this.itemRows = []
      } finally {
        this.loading = false
      }
    },
    async saveCount(it) {
      it.saving = true
      try {
        const res = await countStocktakeItem(it.id, { requestId: it._rid, countedQty: qty(it.countedQty) })
        it.diffQty = res.data.diffQty
        this.$message.success('已录入，差异 ' + res.data.diffQty)
        this.$emit('done')
      } catch (e) { /* 拦截器已提示 */ } finally {
        it.saving = false
      }
    },
    async doConfirm() {
      try {
        await this.$confirm('确认后将按差异调整账面库存（先解冻再调账），且不可撤销，确定继续？', '确认调整', { type: 'warning' })
        const res = await confirmStocktake(this.row.id, { requestId: newRequestId('stocktake-confirm') })
        this.$message.success('调整完成：' + qty(res.data.adjustedCount) + ' 行，差异合计 ' + qty(res.data.diffTotal))
        this.itemsVisible = false
        this.$emit('done')
      } catch (e) { /* 用户取消或拦截器已提示 */ }
    }
  }
}
</script>

<style scoped>
.biz-create { background: #e1251b; border-color: #e1251b; }
.biz-link { color: #e1251b; padding: 0 4px; }
.biz-muted { color: #9aa3b5; font-size: 12px; }
.biz-hint { background: #f7f9fc; border-left: 3px solid #d7dfec; padding: 8px 12px; border-radius: 0 4px 4px 0; font-size: 12px; color: #6b7280; margin-bottom: 14px; }
.biz-hint b { color: #1f2937; }
.neg { color: #e1251b; }
.pos { color: #5a9e4a; }
</style>
