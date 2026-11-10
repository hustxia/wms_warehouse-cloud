<template>
  <span class="biz-actions">
    <el-button v-if="canReceive" type="text" size="mini" class="biz-link" @click="openReceive">收货</el-button>
    <el-button v-if="canPutaway" type="text" size="mini" class="biz-link" @click="openPutaway">上架</el-button>
    <span v-if="!canReceive && !canPutaway" class="biz-muted">{{ statusLabel(row.status) }}</span>

    <!-- 收货登记：逐行录入本次收货数量，支持分批 -->
    <el-dialog title="收货登记" :visible.sync="receiveVisible" width="820px" :close-on-click-modal="false" append-to-body>
      <div class="biz-hint">
        单号 <b>{{ row.orderNo }}</b>；本次数量累加到「实收」，累计不得超过预期数量。批次按明细带出。
      </div>
      <el-table :data="receiveRows" size="mini" border v-loading="loading">
        <el-table-column prop="lineNo" label="行" width="48" align="center" />
        <el-table-column prop="skuId" label="SKU" width="100" />
        <el-table-column prop="batchNo" label="批次" min-width="120">
          <template slot-scope="{ row: it }">{{ it.batchNo || '不管理批次' }}</template>
        </el-table-column>
        <el-table-column prop="expectedQty" label="预期" width="90" align="right" />
        <el-table-column prop="receivedQty" label="已收" width="90" align="right" />
        <el-table-column label="本次收货 *" width="150" align="right">
          <template slot-scope="{ row: it }">
            <el-input-number v-model="it.qty" :min="0" :max="remain(it, 'expectedQty', 'receivedQty')"
              :precision="3" :step="1" controls-position="right" size="mini" style="width: 130px" />
          </template>
        </el-table-column>
      </el-table>
      <div class="biz-sum">本次合计 <b>{{ receiveTotal }}</b>；提交后状态按实收进度推进。</div>
      <div slot="footer">
        <el-button size="small" @click="receiveVisible = false">取消</el-button>
        <el-button size="small" type="primary" :loading="submitting" @click="submitReceive">确认收货</el-button>
      </div>
    </el-dialog>

    <!-- 上架：按目标库位增加库存并写流水 -->
    <el-dialog title="上架" :visible.sync="putawayVisible" width="860px" :close-on-click-modal="false" append-to-body>
      <div class="biz-hint">
        单号 <b>{{ row.orderNo }}</b>；上架数量不得超过「已收 - 已上架」。库位必须属于本单仓库（{{ row.warehouseId }}）。
      </div>
      <el-table :data="putawayRows" size="mini" border v-loading="loading">
        <el-table-column prop="lineNo" label="行" width="48" align="center" />
        <el-table-column prop="skuId" label="SKU" width="100" />
        <el-table-column prop="batchNo" label="批次" min-width="110">
          <template slot-scope="{ row: it }">{{ it.batchNo || '不管理批次' }}</template>
        </el-table-column>
        <el-table-column prop="receivedQty" label="已收" width="80" align="right" />
        <el-table-column prop="putawayQty" label="已上架" width="80" align="right" />
        <el-table-column label="目标库位 *" min-width="170">
          <template slot-scope="{ row: it }">
            <el-select v-model="it.locationId" filterable size="mini" placeholder="选择库位" style="width: 100%">
              <el-option v-for="loc in locOptions" :key="loc.id" :label="loc.locationCode + ' · 仓' + loc.warehouseId"
                :value="loc.id" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="上架数量 *" width="150" align="right">
          <template slot-scope="{ row: it }">
            <el-input-number v-model="it.qty" :min="0" :max="remain(it, 'receivedQty', 'putawayQty')"
              :precision="3" :step="1" controls-position="right" size="mini" style="width: 130px" />
          </template>
        </el-table-column>
      </el-table>
      <div slot="footer">
        <el-button size="small" @click="putawayVisible = false">取消</el-button>
        <el-button size="small" type="primary" :loading="submitting" @click="submitPutaway">确认上架</el-button>
      </div>
    </el-dialog>
  </span>
</template>

<script>
import {
  receiveInbound, putawayInbound, inboundItems, locations,
  newRequestId, statusLabel, qty
} from '@/api/wms_biz'

export default {
  name: 'BizInboundActions',
  props: {
    row: { type: Object, required: true }
  },
  data() {
    return {
      receiveVisible: false,
      putawayVisible: false,
      receiveRows: [],
      putawayRows: [],
      locOptions: [],
      loading: false,
      submitting: false
    }
  },
  computed: {
    canReceive() {
      return ['CREATED', 'IN_TRANSIT', 'RECEIVING'].indexOf(this.row.status) >= 0
    },
    canPutaway() {
      return this.row.status === 'RECEIVED'
    },
    receiveTotal() {
      return this.receiveRows.reduce((s, r) => s + qty(r.qty), 0).toFixed(3)
    }
  },
  methods: {
    statusLabel,
    remain(it, totalField, doneField) {
      const v = qty(it[totalField]) - qty(it[doneField])
      return v > 0 ? v : 0
    },
    async openReceive() {
      this.loading = true
      this.receiveVisible = true
      try {
        const items = await inboundItems(this.row.id)
        this.receiveRows = items.map(it => ({ ...it, qty: this.remain(it, 'expectedQty', 'receivedQty') }))
      } catch (e) {
        this.receiveRows = []
      } finally {
        this.loading = false
      }
    },
    async openPutaway() {
      this.loading = true
      this.putawayVisible = true
      try {
        const [items, locs] = await Promise.all([inboundItems(this.row.id), locations()])
        this.locOptions = locs
        this.putawayRows = items
          .filter(it => this.remain(it, 'receivedQty', 'putawayQty') > 0)
          .map(it => ({ ...it, qty: this.remain(it, 'receivedQty', 'putawayQty'), locationId: it.locationId || null }))
      } catch (e) {
        this.putawayRows = []
      } finally {
        this.loading = false
      }
    },
    async submitReceive() {
      const items = this.receiveRows
        .filter(r => qty(r.qty) > 0)
        .map(r => ({ itemId: r.id, receivedQty: qty(r.qty) }))
      if (!items.length) return this.$message.warning('请至少填写一行收货数量')
      this.submitting = true
      try {
        const res = await receiveInbound(this.row.id, { requestId: newRequestId('receive'), items })
        this.$message.success(res.data && res.data.replayed ? '重复请求，已返回上次结果' : '收货成功，状态：' + statusLabel(res.data.status))
        this.receiveVisible = false
        this.$emit('done')
      } catch (e) { /* axios 拦截器已统一提示 */ } finally {
        this.submitting = false
      }
    },
    async submitPutaway() {
      const items = this.putawayRows
        .filter(r => qty(r.qty) > 0)
        .map(r => ({ itemId: r.id, locationId: r.locationId, putawayQty: qty(r.qty) }))
      if (!items.length) return this.$message.warning('请至少填写一行上架数量')
      if (items.some(i => !i.locationId)) return this.$message.warning('请为每行选择目标库位')
      this.submitting = true
      try {
        const res = await putawayInbound(this.row.id, { requestId: newRequestId('putaway'), items })
        this.$message.success('上架成功，本次 ' + qty(res.data.putawayQty) + ' 件')
        this.putawayVisible = false
        this.$emit('done')
      } catch (e) { /* 拦截器已提示 */ } finally {
        this.submitting = false
      }
    }
  }
}
</script>

<style scoped>
.biz-link { color: #e1251b; padding: 0 4px; }
.biz-muted { color: #9aa3b5; font-size: 12px; }
.biz-hint { background: #f7f9fc; border-left: 3px solid #d7dfec; padding: 8px 12px; border-radius: 0 4px 4px 0; font-size: 12px; color: #6b7280; margin-bottom: 14px; }
.biz-hint b { color: #1f2937; }
.biz-sum { margin-top: 12px; font-size: 12px; color: #8a94a6; }
.biz-sum b { color: #1f2937; }
</style>
