<script setup>
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'
import { api } from '@/api'
import { uploadFile } from '@/utils/upload'

defineProps({ categories: { type: Array, default: () => [] } })
const emit = defineEmits(['refresh'])
const visible = defineModel({ type: Boolean })

const editing = ref(null)
const form = reactive({ id: undefined, name: '', iconUrl: '', sortOrder: 0, valid: true })

function reset() {
  Object.assign(form, { id: undefined, name: '', iconUrl: '', sortOrder: 0, valid: true })
  editing.value = null
}

function edit(c) {
  editing.value = c.id
  Object.assign(form, { id: c.id, name: c.name, iconUrl: c.iconUrl, sortOrder: c.sortOrder, valid: c.valid })
}

async function onUpload(file) {
  form.iconUrl = await uploadFile(file.file)
  return false
}

async function save() {
  if (!form.name.trim()) return ElMessage.warning('请输入分类名')
  await api.categorySave({ ...form })
  ElMessage.success('已保存')
  reset()
  emit('refresh')
}

async function remove(c) {
  await ElMessageBox.confirm(`删除分类「${c.name}」？`, '提示', { type: 'warning' })
  await api.categoryDelete(c.id)
  ElMessage.success('已删除')
  emit('refresh')
}
</script>

<template>
  <el-drawer v-model="visible" title="分类管理" size="440px">
    <div class="editor">
      <el-input v-model="form.name" placeholder="分类名" style="width: 160px" />
      <el-input-number v-model="form.sortOrder" :min="0" controls-position="right" style="width: 110px" />
      <el-upload :show-file-list="false" :http-request="onUpload" accept="image/*">
        <el-image v-if="form.iconUrl" :src="form.iconUrl" fit="cover" class="icon-img" />
        <div v-else class="icon-btn"><el-icon><Plus /></el-icon></div>
      </el-upload>
      <el-switch v-model="form.valid" />
      <el-button type="primary" @click="save">{{ editing ? '更新' : '添加' }}</el-button>
      <el-button v-if="editing" @click="reset">取消</el-button>
    </div>

    <el-table :data="categories" style="width: 100%; margin-top: 16px">
      <el-table-column label="图标" width="70">
        <template #default="{ row }">
          <el-image v-if="row.iconUrl" :src="row.iconUrl" fit="cover" class="icon-img sm" />
        </template>
      </el-table-column>
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
.icon-img {
  width: 40px;
  height: 40px;
  border-radius: $radius-control;
  border: 1px solid $border;
  &.sm {
    width: 32px;
    height: 32px;
  }
}
.icon-btn {
  width: 40px;
  height: 40px;
  border: 1px dashed $border;
  border-radius: $radius-control;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $text-weak;
  cursor: pointer;
}
.link-text.danger {
  color: $danger;
}
</style>
