/* eslint-disable */
// 该文件由代码生成器自动维护，请勿手工编辑。
// 注册/删除菜单后会自动重建；要移除某个生成页面，请先删除表配置。
// 结构为三级：系统 → 业务域 → 页面，菜单树与路由共用同一套分组推导。
import Layout from '@/layout/index.vue'
const GroupView = { name: 'GroupView', render: h => h('router-view') }

export default [
  {
    path: '/wms_warehouse',
    component: Layout,
    meta: { title: 'wms 系统' },
    children: [
      {
        path: 'base',
        component: GroupView,
        meta: { title: '基础资料' },
        children: [
          {
            path: 'wms_warehouse/list',
            name: 'WmsWarehouse',
            component: () => import('@/views/wms_warehouse/index.vue'),
            meta: { title: '仓库主表' }
          },
          {
            path: 'wms_location/list',
            name: 'WmsLocation',
            component: () => import('@/views/wms_location/index.vue'),
            meta: { title: '库位表' }
          }
        ]
      },
      {
        path: 'other',
        component: GroupView,
        meta: { title: '其他' },
        children: [
          {
            path: 'wms_zone/list',
            name: 'WmsZone',
            component: () => import('@/views/wms_zone/index.vue'),
            meta: { title: '库区表' }
          },
          {
            path: 'wms_owner/list',
            name: 'WmsOwner',
            component: () => import('@/views/wms_owner/index.vue'),
            meta: { title: '货主表' }
          },
          {
            path: 'wms_wave/list',
            name: 'WmsWave',
            component: () => import('@/views/wms_wave/index.vue'),
            meta: { title: '波次表' }
          }
        ]
      },
      {
        path: 'product',
        component: GroupView,
        meta: { title: '产品管理' },
        children: [
          {
            path: 'wms_sku/list',
            name: 'WmsSku',
            component: () => import('@/views/wms_sku/index.vue'),
            meta: { title: '商品SKU表' }
          }
        ]
      },
      {
        path: 'stock',
        component: GroupView,
        meta: { title: '库存管理' },
        children: [
          {
            path: 'wms_inventory/list',
            name: 'WmsInventory',
            component: () => import('@/views/wms_inventory/index.vue'),
            meta: { title: '库存表' }
          },
          {
            path: 'wms_inventory_txn/list',
            name: 'WmsInventoryTxn',
            component: () => import('@/views/wms_inventory_txn/index.vue'),
            meta: { title: '库存事务流水表' }
          },
          {
            path: 'wms_inventory_lock/list',
            name: 'WmsInventoryLock',
            component: () => import('@/views/wms_inventory_lock/index.vue'),
            meta: { title: '库存锁定表' }
          },
          {
            path: 'wms_allocation/list',
            name: 'WmsAllocation',
            component: () => import('@/views/wms_allocation/index.vue'),
            meta: { title: '库存分配明细表' }
          },
          {
            path: 'wms_stocktake_order/list',
            name: 'WmsStocktakeOrder',
            component: () => import('@/views/wms_stocktake_order/index.vue'),
            meta: { title: '盘点单表' }
          },
          {
            path: 'wms_stocktake_item/list',
            name: 'WmsStocktakeItem',
            component: () => import('@/views/wms_stocktake_item/index.vue'),
            meta: { title: '盘点明细表' }
          },
          {
            path: 'wms_transfer_order/list',
            name: 'WmsTransferOrder',
            component: () => import('@/views/wms_transfer_order/index.vue'),
            meta: { title: '移库单表' }
          }
        ]
      },
      {
        path: 'inbound',
        component: GroupView,
        meta: { title: '入库管理' },
        children: [
          {
            path: 'wms_inbound_order/list',
            name: 'WmsInboundOrder',
            component: () => import('@/views/wms_inbound_order/index.vue'),
            meta: { title: '入库单主表' }
          },
          {
            path: 'wms_inbound_order_item/list',
            name: 'WmsInboundOrderItem',
            component: () => import('@/views/wms_inbound_order_item/index.vue'),
            meta: { title: '入库单明细表' }
          },
          {
            path: 'wms_receipt_record/list',
            name: 'WmsReceiptRecord',
            component: () => import('@/views/wms_receipt_record/index.vue'),
            meta: { title: '收货记录表' }
          },
          {
            path: 'wms_putaway_task/list',
            name: 'WmsPutawayTask',
            component: () => import('@/views/wms_putaway_task/index.vue'),
            meta: { title: '上架任务表' }
          }
        ]
      },
      {
        path: 'outbound',
        component: GroupView,
        meta: { title: '出库管理' },
        children: [
          {
            path: 'wms_outbound_order/list',
            name: 'WmsOutboundOrder',
            component: () => import('@/views/wms_outbound_order/index.vue'),
            meta: { title: '出库单主表' }
          },
          {
            path: 'wms_outbound_order_item/list',
            name: 'WmsOutboundOrderItem',
            component: () => import('@/views/wms_outbound_order_item/index.vue'),
            meta: { title: '出库单明细表' }
          },
          {
            path: 'wms_pick_task/list',
            name: 'WmsPickTask',
            component: () => import('@/views/wms_pick_task/index.vue'),
            meta: { title: '拣货任务表' }
          },
          {
            path: 'wms_shipment/list',
            name: 'WmsShipment',
            component: () => import('@/views/wms_shipment/index.vue'),
            meta: { title: '发货单表' }
          }
        ]
      }
    ]
  },
]
