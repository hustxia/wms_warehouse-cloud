<template>
  <span class="biz-actions">
    <el-button v-if="canAllocate" type="text" size="mini" class="biz-link" @click="doAllocate">分配</el-button>
    <el-button v-if="canPick" type="text" size="mini" class="biz-link" @click="openPick">拣货</el-button>
    <el-button v-if="canShip" type="text" size="mini" class="biz-link" @click="openShip">发运</el-button>
    <el-button v-if="canCancel" type="text" size="mini" class="biz-link mute" @click="doCancel">取消分配</el-button>
    <span v-if="row.status === 'SHIPPED'" class="biz-muted">已发运</span>

    <!-- 分配结果：FEFO 批次明细展开（S4 效果图确认的形态） -->
    <el-dialog title="分配结果 · 按效期先进先出" :visible.sync="resultVisible" width="760px" append-to-body>
      <div class="biz-hint">
        单号 <b>{{ row.orderNo }}</b>，状态 <b>{{ statusLabel(resultStatus) }}</b>。
        下方为本次实际占用的批次切片；效期近的批次排在前面，库存不足时只分配可用部分。
      </div>
      <el-table :data="slices" size="mini" border>
        <el-table-column type="index" label="#" width="48" align="center" />
        <el-table-column prop="batchNo" label="批次" min-width="140">
          <template slot-scope="{ row: s }">{{ s.batchNo || '不管理批次' }}</template>
        </el-table-column>
        <el-table-column prop="inventoryId" label="库存ID" min-width="160" />
        <el-table-column prop="locationId" label="库位" width="100" />
        <el-table-column prop="qty" label="分配数量" width="110" align="right" />
      </el-table>
      <div class="biz-sum" v-if="shortage > 0">仍有 <b class="shortage">{{ shortage }}</b> 件未满足，可补货后再次分配。</div>
      <div slot="footer"><el-button size="small" type="primary" @click="resultVisible = false">知道了</el-button></div>
    </el-dialog>

    <!-- 拣货确认 -->
    <el-dialog title="拣货确认" :visible.sync="pickVisible" width="720px" :close-on-click-modal="false" append-to-body>
      <div class="biz-hint">拣货数量不得超过「已分配 - 已拣」；确认后同时扣在库量并销掉占用。</div>
      <el-table :data="pickRows" size="mini" border v-loading="loading">
        <el-table-column prop="lineNo" label="行" width="48" align="center" />
        <el-table-column prop="skuId" label="SKU" width="100" />
        <el-table-column prop="orderQty" label="订单量" width="90" align="right" />
        <el-table-column prop="allocatedQty" label="已分配" width="90" align="right" />
        <el-table-column prop="pickedQty" label="已拣" width="80" align="right" />
        <el-table-column label="本次拣货 *" width="150" align="right">
          <template slot-scope="{ row: it }">
            <el-input-number v-model="it.qty" :min="0" :max="Math.max(0, qty(it.allocatedQty) - qty(it.pickedQty))"
              :precision="3" :step="1" controls-position="right" size="mini" style="width: 130px" />
          </template>
        </el-table-column>
      </el-table>
      <div slot="footer">
        <el-button size="small" @click="pickVisible = false">取消</el-button>
        <el-button size="small" type="primary" :loading="submitting" @click="submitPick">确认拣货</el-button>
      </div>
    </el-dialog>

    <!-- 发运 -->
    <el-dialog title="发运登记" :visible.sync="shipVisible" width="480px" :close-on-click-modal="false" append-to-body>
      <el-form :model="shipForm" label-width="90px" size="small">
        <el-form-item label="承运商" required><el-input v-model="shipForm.carrier" placeholder="如：顺丰速运" /></el-form-item>
        <el-form-item label="运单号" required><el-input v-model="shipForm.trackingNo" placeholder="如：SF20261107001" /></el-form-item>
      </el-form>
      <div slot="footer">
        <el-button size="small" @click="shipVisible = false">取消</el-button>
        <el-button size="small" type="primary" :loading="submitting" @click="submitShip">确认发运</el-button>
      </div>
    </el-dialog>
  </span>
</template>

<script>
import {
  allocateOutbound, cancelAllocateOutbound, pickOutbound, shipOutbound,
  outboundItems, newRequestId, statusLabel, qty
} from '@/api/wms_biz'

export default {
  name: 'BizOutboundActions',
  props: { row: { type: Object, required: true } },
  data() {
    return {
      resultVisible: false,
      pickVisible: false,
      shipVisible: false,
      slices: [],
      resultStatus: '',
      shortage: 0,
      pickRows: [],
      shipForm: { carrier: '', trackingNo: '' },
      loading: false,
      submitting: false
    }
  },
  computed: {
    canAllocate() { return ['CREATED', 'PARTIAL'].indexOf(this.row.status) >= 0 },
    canCancel() { return ['ALLOCATED', 'PARTIAL'].indexOf(this.row.status) >= 0 },
    canPick() { return ['ALLOCATED', 'PARTIAL', 'PICKING'].indexOf(this.row.status) >= 0 },
    canShip() { return this.row.status === 'PICKED' }
  },
  methods: {
    statusLabel,
    qty,
    async doAllocate() {
      this.submitting = true
      try {
        const res = await allocateOutbound(this.row.id, { requestId: newRequestId('allocate') })
        const d = res.data || {}
        this.resultStatus = d.status
        this.slices = []
        this.shortage = 0
        ;(d.items || []).forEach(it => {
          this.shortage += qty(it.shortageQty)
          ;(it.allocations || []).forEach(s => this.slices.push({ ...s }))
        })
        this.resultVisible = true
        this.$message.success(d.replayed ? '重复请求，已返回上次分配结果' : '分配完成')
        this.$emit('done')
      } catch (e) { /* 拦截器已提示 */ } finally {
        this.submitting = false
      }
    },
    async doCancel() {
      try {
        await this.$confirm('取消后将释放本单全部库存占用，确定继续？', '取消分配', { type: 'warning' })
        await cancelAllocateOutbound(this.row.id, { requestId: newRequestId('cancel-allocate') })
        this.$message.success('已释放占用，单据回到已创建')
        this.$emit('done')
      } catch (e) { /* 用户取消或拦截器已提示 */ }
    },
    async openPick() {
      this.loading = true
      this.pickVisible = true
      try {
        const items = await outboundItems(this.row.id)
        this.pickRows = items
          .filter(it => qty(it.allocatedQty) - qty(it.pickedQty) > 0)
          .map(it => ({ ...it, qty: qty(it.allocatedQty) - qty(it.pickedQty) }))
      } catch (e) {
        this.pickRows = []
      } finally {
        this.loading = false
      }
    },
    async submitPick() {
      const items = this.pickRows.filter(r => qty(r.qty) > 0)
        .map(r => ({ itemId: r.id, pickedQty: qty(r.qty) }))
      if (!items.length) return this.$message.warning('请至少填写一行拣货数量')
      this.submitting = true
      try {
        const res = await pickOutbound(this.row.id, { requestId: newRequestId('pick'), items })
        this.$message.success('拣货成功，本次 ' + qty(res.data.pickedQty) + ' 件')
        this.pickVisible = false
        this.$emit('done')
      } catch (e) { /* 拦截器已提示 */ } finally {
        this.submitting = false
      }
    },
    openShip() {
      this.shipForm = { carrier: '', trackingNo: '' }
      this.shipVisible = true
    },
    async submitShip() {
      if (!this.shipForm.carrier.trim() || !this.shipForm.trackingNo.trim()) {
        return this.$message.warning('承运商与运单号必填')
      }
      this.submitting = true
      try {
        const res = await shipOutbound(this.row.id, {
          requestId: newRequestId('ship'),
          carrier: this.shipForm.carrier.trim(),
          trackingNo: this.shipForm.trackingNo.trim()
        })
        this.$message.success('发运成功，发货单 ' + res.data.shipmentNo)
        this.shipVisible = false
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
.biz-link.mute { color: #8a94a6; }
.biz-muted { color: #9aa3b5; font-size: 12px; }
.biz-hint { background: #f7f9fc; border-left: 3px solid #d7dfec; padding: 8px 12px; border-radius: 0 4px 4px 0; font-size: 12px; color: #6b7280; margin-bottom: 14px; }
.biz-hint b { color: #1f2937; }
.biz-sum { margin-top: 12px; font-size: 12px; color: #8a94a6; }
.shortage { color: #e1251b; }
</style>
