<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Top, Bottom } from '@element-plus/icons-vue'
import { api } from '@/api'
import { uploadFile } from '@/utils/upload'

const loading = ref(false)
const list = ref([])

async function load() {
  loading.value = true
  try {
    list.value = await api.banners()
  } finally {
    loading.value = false
  }
}

const dialog = reactive({ visible: false, saving: false })
const form = reactive({ id: undefined, imageUrl: '', linkUrl: '', sortOrder: 0, valid: true })

function openCreate() {
  Object.assign(form, { id: undefined, imageUrl: '', linkUrl: '', sortOrder: list.value.length, valid: true })
  dialog.visible = true
}
function openEdit(b) {
  Object.assign(form, { id: b.id, imageUrl: b.imageUrl, linkUrl: b.linkUrl, sortOrder: b.sortOrder, valid: b.valid })
  dialog.visible = true
}

async function onUpload(file) {
  form.imageUrl = await uploadFile(file.file)
  ElMessage.success('图片已上传')
  return false
}

async function submit() {
  if (!form.imageUrl) return ElMessage.warning('请上传图片')
  dialog.saving = true
  try {
    await api.bannerSave({ ...form })
    ElMessage.success('已保存')
    dialog.visible = false
    load()
  } finally {
    dialog.saving = false
  }
}

async function remove(b) {
  await ElMessageBox.confirm('确认删除该 Banner？', '提示', { type: 'warning' })
  await api.bannerDelete(b.id)
  ElMessage.success('已删除')
  load()
}

async function toggleValid(b) {
  await api.bannerSave({ id: b.id, imageUrl: b.imageUrl, linkUrl: b.linkUrl, sortOrder: b.sortOrder, valid: !b.valid })
  load()
}

async function move(index, dir) {
  const target = index + dir
  if (target < 0 || target >= list.value.length) return
  const arr = [...list.value]
  ;[arr[index], arr[target]] = [arr[target], arr[index]]
  const items = arr.map((b, i) => ({ id: b.id, sortOrder: i }))
  await api.bannerSort(items)
  load()
}

onMounted(load)
</script>

<template>
  <div>
    <div class="toolbar">
      <div class="page-hint">首页轮播图，拖动排序按序号展示</div>
      <div class="toolbar-spacer" />
      <el-button type="primary" :icon="Plus" @click="openCreate">新增 Banner</el-button>
    </div>

    <div v-loading="loading" class="banner-list">
      <div v-for="(b, i) in list" :key="b.id" class="banner-item">
        <el-image :src="b.imageUrl" fit="cover" class="banner-img" />
        <div class="banner-info">
          <div class="banner-link">{{ b.linkUrl || '无跳转链接' }}</div>
          <el-tag :type="b.valid ? 'success' : 'info'" size="small">{{ b.valid ? '已上线' : '已下线' }}</el-tag>
        </div>
        <div class="banner-ops">
          <el-button :icon="Top" circle size="small" :disabled="i === 0" @click="move(i, -1)" />
          <el-button :icon="Bottom" circle size="small" :disabled="i === list.length - 1" @click="move(i, 1)" />
          <span class="link-text" @click="openEdit(b)">编辑</span>
          <span class="link-text" @click="toggleValid(b)">{{ b.valid ? '下线' : '上线' }}</span>
          <span class="link-text danger" @click="remove(b)">删除</span>
        </div>
      </div>
      <el-empty v-if="!loading && !list.length" description="暂无 Banner" />
    </div>

    <el-dialog v-model="dialog.visible" :title="form.id ? '编辑 Banner' : '新增 Banner'" width="520px">
      <el-form label-width="88px">
        <el-form-item label="图片" required>
          <el-upload :show-file-list="false" :http-request="onUpload" accept="image/*">
            <el-image v-if="form.imageUrl" :src="form.imageUrl" fit="cover" class="up-img" />
            <div v-else class="up-btn"><el-icon><Plus /></el-icon></div>
          </el-upload>
        </el-form-item>
        <el-form-item label="跳转链接">
          <el-input v-model="form.linkUrl" placeholder="选填，如商品页路径" />
        </el-form-item>
        <el-form-item label="是否上线">
          <el-switch v-model="form.valid" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="dialog.saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}
.page-hint {
  color: $text-weak;
  font-size: 13px;
}
.toolbar-spacer {
  flex: 1;
}
.banner-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 120px;
}
.banner-item {
  display: flex;
  align-items: center;
  gap: 16px;
  background: #fff;
  border: 1px solid $border;
  border-radius: $radius-card;
  padding: 12px 16px;
}
.banner-img {
  width: 200px;
  height: 80px;
  border-radius: $radius-control;
  flex-shrink: 0;
}
.banner-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.banner-link {
  color: $text-body;
  font-size: 13px;
}
.banner-ops {
  display: flex;
  align-items: center;
  gap: 12px;
}
.link-text.danger {
  color: $danger;
}
.up-img {
  width: 240px;
  height: 96px;
  border-radius: $radius-control;
  border: 1px solid $border;
}
.up-btn {
  width: 240px;
  height: 96px;
  border: 1px dashed $border;
  border-radius: $radius-control;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $text-weak;
  font-size: 24px;
  cursor: pointer;
  &:hover {
    border-color: $brand;
    color: $brand;
  }
}
</style>
