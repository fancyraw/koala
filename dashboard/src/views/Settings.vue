<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '@/api'

const active = ref('shipping')
const loading = ref(false)
const saving = ref(false)

// 各分组字段定义（label + 输入类型），值统一以字符串存取
const GROUPS = {
  shipping: {
    label: '配送设置',
    fields: [
      { key: 'base_fee', label: '基础运费(元)', type: 'number' },
      { key: 'free_threshold', label: '包邮门槛(元)', type: 'number', tip: '商品合计达到即免运费' },
    ],
  },
  payment: {
    label: '支付设置',
    fields: [
      {
        key: 'active_channel',
        label: '支付渠道',
        type: 'select',
        options: [{ label: '微信支付', value: 'wechat' }],
      },
      { key: 'wechat_mch_id', label: '微信商户号', type: 'text', tip: '密钥通过环境变量配置，不在此填写' },
    ],
  },
  order: {
    label: '订单设置',
    fields: [
      { key: 'pay_timeout_minutes', label: '支付超时(分钟)', type: 'number' },
      { key: 'auto_confirm_days', label: '自动确认收货(天)', type: 'number' },
    ],
  },
  system: {
    label: '维护模式',
    fields: [
      { key: 'maintenance_mode', label: '开启维护模式', type: 'switch' },
      { key: 'maintenance_notice', label: '维护提示文案', type: 'textarea' },
    ],
  },
}

// 每组当前值 { group: { key: value } }
const model = reactive({ shipping: {}, payment: {}, order: {}, system: {} })

async function loadGroup(group) {
  loading.value = true
  try {
    const rows = await api.config(group)
    const map = {}
    ;(rows || []).forEach((r) => {
      map[r.configKey] = r.configValue
    })
    model[group] = map
  } finally {
    loading.value = false
  }
}

function onTab(group) {
  if (!Object.keys(model[group]).length) loadGroup(group)
}

async function save(group) {
  saving.value = true
  try {
    const items = GROUPS[group].fields.map((f) => ({
      key: f.key,
      value: String(model[group][f.key] ?? ''),
    }))
    await api.configSave({ group, items })
    ElMessage.success('已保存')
  } finally {
    saving.value = false
  }
}

onMounted(() => loadGroup('shipping'))
</script>

<template>
  <div class="card">
    <el-tabs v-model="active" @tab-change="onTab">
      <el-tab-pane v-for="(g, group) in GROUPS" :key="group" :label="g.label" :name="group">
        <el-form v-loading="loading" label-width="140px" class="settings-form">
          <el-form-item v-for="f in g.fields" :key="f.key" :label="f.label">
            <template v-if="f.type === 'switch'">
              <el-switch
                :model-value="model[group][f.key] === '1'"
                @update:model-value="(v) => (model[group][f.key] = v ? '1' : '0')"
              />
            </template>
            <el-input-number
              v-else-if="f.type === 'number'"
              v-model="model[group][f.key]"
              :min="0"
              controls-position="right"
            />
            <el-select v-else-if="f.type === 'select'" v-model="model[group][f.key]" style="width: 220px">
              <el-option v-for="o in f.options" :key="o.value" :label="o.label" :value="o.value" />
            </el-select>
            <el-input
              v-else-if="f.type === 'textarea'"
              v-model="model[group][f.key]"
              type="textarea"
              :rows="3"
              style="width: 360px"
            />
            <el-input v-else v-model="model[group][f.key]" style="width: 260px" />
            <span v-if="f.tip" class="hint">{{ f.tip }}</span>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="saving" @click="save(group)">保存</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style lang="scss" scoped>
.card {
  background: #fff;
  border-radius: $radius-card;
  padding: 16px 24px;
  border: 1px solid $border;
}
.settings-form {
  max-width: 640px;
  padding-top: 12px;
}
.hint {
  margin-left: 12px;
  color: $text-weak;
  font-size: 12px;
}
</style>
