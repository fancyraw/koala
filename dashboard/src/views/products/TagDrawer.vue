<script setup>
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '@/api'

defineProps({ tags: { type: Array, default: () => [] } })
const emit = defineEmits(['refresh'])
const visible = defineModel({ type: Boolean })

const editing = ref(null)
const form = reactive({ id: undefined, name: '', sortOrder: 0, valid: true })

function reset() {
  Object.assign(form, { id: undefined, name: '', sortOrder: 0, valid: true })
  editing.value = null
}

function edit(t) {
  editing.value = t.id
  Object.assign(form, { id: t.id, name: t.name, sortOrder: t.sortOrder, valid: t.valid })
}

async function save() {
  if (!form.name.trim()) return ElMessage.warning('请输入标签名')
  await api.tagSave({ ...form })
  ElMessage.success('已保存')
  reset()
  emit('refresh')
}

async function remove(t) {
  await ElMessageBox.confirm(`删除标签「${t.name}」？`, '提示', { type: 'warning' })
  await api.tagDelete(t.id)
  ElMessage.success('已删除')
  emit('refresh')
}
</script>

<template>
  <el-drawer v-model="visible" title="标签管理" size="400px">
    <div class="editor">
      <el-input v-model="form.name" placeholder="标签名" style="width: 160px" />
      <el-input-number v-model="form.sortOrder" :min="0" controls-position="right" style="width: 110px" />
      <el-switch v-model="form.valid" />
      <el-button type="primary" @click="save">{{ editing ? '更新' : '添加' }}</el-button>
      <el-button v-if="editing" @click="reset">取消</el-button>
    </div>

    <el-table :data="tags" style="width: 100%; margin-top: 16px">
      <el-table-column label="名称" prop="name" />
      <el-table-column label="排序" prop="sortOrder" width="70" align="center" />
      <el-table-column label="状态" width="70">
        <template #default="{ row }">
          <el-tag :type="row.valid ? 'success' : 'info'" size="small">{{ row.valid ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="110">
        <template #default="{ row }">
          <div class="op-btns">
            <span class="link-text" @click="edit(row)">编辑</span>
            <span class="link-text danger" @click="remove(row)">删除</span>
          </div>
        </template>
      </el-table-column>
    </el-table>
  </el-drawer>
</template>

<style lang="scss" scoped>
.editor {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.link-text.danger {
  color: $danger;
}
</style>
