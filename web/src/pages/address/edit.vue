<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { api } from '@/api'
import { toast } from '@/utils/toast'

const editingId = ref(null)
const form = ref({
  name: '',
  phone: '',
  provinceCode: '',
  cityCode: '',
  districtCode: '',
  province: '',
  city: '',
  district: '',
  detail: '',
  isDefault: false,
})

// 地区级联选择
const regionVisible = ref(false)
const regionStep = ref(0) // 0省 1市 2区
const provinces = ref([])
const cities = ref([])
const districts = ref([])
const picking = ref({ province: null, city: null, district: null })

const regionText = computed(() => {
  const { province, city, district } = form.value
  if (!province) return '请选择所在地区'
  return [province, city, district].filter(Boolean).join(' ')
})

const submitting = ref(false)

onLoad(async (opts) => {
  if (opts.id) {
    editingId.value = Number(opts.id)
    const a = await api.addressDetail(editingId.value)
    form.value = {
      name: a.name,
      phone: a.phone,
      provinceCode: a.provinceCode,
      cityCode: a.cityCode,
      districtCode: a.districtCode,
      province: a.province,
      city: a.city,
      district: a.district,
      detail: a.detail,
      isDefault: !!a.isDefault,
    }
    uni.setNavigationBarTitle({ title: '编辑地址' })
  } else {
    uni.setNavigationBarTitle({ title: '新增地址' })
  }
})

async function openRegion() {
  regionStep.value = 0
  provinces.value = await api.regions()
  regionVisible.value = true
}

async function pickRegion(node) {
  if (regionStep.value === 0) {
    picking.value.province = node
    if (node.hasChildren) {
      cities.value = await api.regions(node.code)
      regionStep.value = 1
    } else {
      commitRegion()
    }
  } else if (regionStep.value === 1) {
    picking.value.city = node
    if (node.hasChildren) {
      districts.value = await api.regions(node.code)
      regionStep.value = 2
    } else {
      commitRegion()
    }
  } else {
    picking.value.district = node
    commitRegion()
  }
}

function commitRegion() {
  const { province, city, district } = picking.value
  form.value.provinceCode = province?.code || ''
  form.value.province = province?.name || ''
  form.value.cityCode = city?.code || ''
  form.value.city = city?.name || ''
  form.value.districtCode = district?.code || ''
  form.value.district = district?.name || ''
  regionVisible.value = false
}

const currentNodes = computed(() => {
  if (regionStep.value === 0) return provinces.value
  if (regionStep.value === 1) return cities.value
  return districts.value
})

async function save() {
  if (submitting.value) return
  const f = form.value
  if (!f.name.trim()) return toast('请输入收货人')
  if (!/^1[3-9]\d{9}$/.test(f.phone)) return toast('手机号格式不正确')
  if (!f.provinceCode) return toast('请选择所在地区')
  if (!f.detail.trim()) return toast('请输入详细地址')

  submitting.value = true
  const payload = {
    name: f.name.trim(),
    phone: f.phone,
    provinceCode: f.provinceCode,
    cityCode: f.cityCode,
    districtCode: f.districtCode,
    detail: f.detail.trim(),
    isDefault: f.isDefault,
  }
  try {
    if (editingId.value) {
      await api.addressUpdate({ id: editingId.value, ...payload })
    } else {
      await api.addressAdd(payload)
    }
    toast('已保存')
    setTimeout(() => uni.navigateBack(), 500)
  } catch (e) {
    submitting.value = false
  }
}
</script>

<template>
  <view class="addr-edit">
    <view class="form">
      <view class="field">
        <text class="label">收货人</text>
        <input v-model="form.name" class="input" placeholder="请输入姓名" placeholder-class="ph" />
      </view>
      <view class="field">
        <text class="label">手机号</text>
        <input v-model="form.phone" class="input" type="number" maxlength="11" placeholder="请输入手机号" placeholder-class="ph" />
      </view>
      <view class="field" @tap="openRegion">
        <text class="label">所在地区</text>
        <text class="input region" :class="{ ph: !form.province }">{{ regionText }}</text>
        <text class="arrow">›</text>
      </view>
      <view class="field textarea-field">
        <text class="label">详细地址</text>
        <textarea v-model="form.detail" class="textarea" placeholder="街道、楼牌号等" placeholder-class="ph" auto-height />
      </view>
      <view class="field switch-field">
        <text class="label">设为默认</text>
        <switch :checked="form.isDefault" color="#ff2442" @change="form.isDefault = $event.detail.value" />
      </view>
    </view>

    <view class="save-bar">
      <view class="save-btn" :class="{ disabled: submitting }" hover-class="hover-dim" :hover-stay-time="0" @tap="save">保存</view>
    </view>

    <!-- 地区选择弹层 -->
    <view v-if="regionVisible" class="region-mask" @tap="regionVisible = false">
      <view class="region-sheet" @tap.stop>
        <view class="region-head">
          <text class="rh-title">选择地区</text>
          <text class="rh-close" @tap="regionVisible = false">✕</text>
        </view>
        <view class="region-crumbs">
          <text v-if="picking.province" class="crumb">{{ picking.province.name }}</text>
          <text v-if="picking.city && regionStep >= 2" class="crumb">{{ picking.city.name }}</text>
        </view>
        <scroll-view scroll-y class="region-list">
          <view v-for="node in currentNodes" :key="node.code" class="region-item" @tap="pickRegion(node)">
            {{ node.name }}
          </view>
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.addr-edit {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: $page-bg;
}
.form {
  margin: 24rpx $safe-x 0;
  background: #fff;
  border-radius: $radius-card;
  padding: 0 24rpx;
}
.field {
  display: flex;
  align-items: center;
  min-height: 100rpx;
  border-bottom: 2rpx solid $divider;
}
.field:last-child {
  border-bottom: none;
}
.label {
  width: 160rpx;
  font-size: $fs-body;
  color: $text-title;
}
.input {
  flex: 1;
  font-size: $fs-body;
  color: $text-body;
}
.region.ph {
  color: $text-placeholder;
}
:deep(.ph) {
  color: $text-placeholder;
}
.arrow {
  font-size: 36rpx;
  color: $text-placeholder;
}
.textarea-field {
  align-items: flex-start;
  padding: 24rpx 0;
}
.textarea {
  flex: 1;
  min-height: 120rpx;
  font-size: $fs-body;
  color: $text-body;
}
.switch-field {
  justify-content: space-between;
}
.save-bar {
  margin-top: auto;
  padding: 20rpx $safe-x calc(20rpx + env(safe-area-inset-bottom));
}
.save-btn {
  height: 88rpx;
  background: $brand;
  color: #fff;
  border-radius: $radius-pill;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: $fs-title;
  font-weight: 600;
}
.save-btn.disabled {
  opacity: 0.6;
}
.region-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 200;
  display: flex;
  align-items: flex-end;
}
.region-sheet {
  width: 100%;
  background: #fff;
  border-radius: $radius-card $radius-card 0 0;
  max-height: 70vh;
  display: flex;
  flex-direction: column;
  padding-bottom: env(safe-area-inset-bottom);
}
.region-head {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 28rpx;
  position: relative;
}
.rh-title {
  font-size: $fs-title;
  font-weight: 600;
  color: $text-title;
}
.rh-close {
  position: absolute;
  right: 28rpx;
  font-size: 36rpx;
  color: $text-placeholder;
}
.region-crumbs {
  display: flex;
  gap: 20rpx;
  padding: 0 $safe-x 16rpx;
}
.crumb {
  font-size: $fs-aux;
  color: $brand;
}
.region-list {
  flex: 1;
  max-height: 50vh;
}
.region-item {
  padding: 28rpx $safe-x;
  font-size: $fs-body;
  color: $text-body;
  border-bottom: 2rpx solid $divider;
}
</style>
