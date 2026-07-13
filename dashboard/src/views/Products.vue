<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'
import { api } from '@/api'
import { yuan } from '@/utils/format'
import { uploadFile } from '@/utils/upload'
import CategoryDrawer from './products/CategoryDrawer.vue'
import TagDrawer from './products/TagDrawer.vue'

const route = useRoute()

const loading = ref(false)
const list = ref([])
const total = ref(0)
const categories = ref([])
const tags = ref([])

const query = reactive({
  categoryId: undefined,
  keyword: route.query.keyword || '',
  status: undefined,
  page: 1,
  size: 20,
})

async function loadCategories() {
  categories.value = await api.categories()
}
async function loadTags() {
  tags.value = await api.tags()
}

async function load() {
  loading.value = true
  try {
    const res = await api.products({
      categoryId: query.categoryId,
      keyword: query.keyword || undefined,
      status: query.status,
      page: query.page,
      size: query.size,
    })
    list.value = res.list || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  load()
}
function resetQuery() {
  query.categoryId = undefined
  query.keyword = ''
  query.status = undefined
  search()
}

async function toggleStatus(row) {
  const next = row.isValid === 1 ? 0 : 1
  await api.productStatus(row.id, next)
  row.isValid = next
  ElMessage.success(next === 1 ? '已上架' : '已下架')
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除商品「${row.name}」？`, '提示', { type: 'warning' })
  await api.productDelete(row.id)
  ElMessage.success('已删除')
  load()
}

// ---- 编辑弹窗 ----
const dialog = reactive({ visible: false, saving: false })
const formRef = ref(null)
const blankSku = () => ({ id: undefined, name: '', price: 0, stock: 0 })
const form = reactive({
  id: undefined,
  name: '',
  mainImage: '',
  detailImages: [],
  categoryId: undefined,
  tagId: undefined,
  recommended: false,
  highlights: [],
  perOrderLimit: 0,
  skus: [blankSku()],
})
const highlightText = ref('')

const rules = {
  name: [{ required: true, message: '请输入商品名', trigger: 'blur' }],
  mainImage: [{ required: true, message: '请上传主图', trigger: 'change' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
}

function openCreate() {
  Object.assign(form, {
    id: undefined,
    name: '',
    mainImage: '',
    detailImages: [],
    categoryId: undefined,
    tagId: undefined,
    recommended: false,
    highlights: [],
    perOrderLimit: 0,
    skus: [blankSku()],
  })
  highlightText.value = ''
  dialog.visible = true
}

async function openEdit(row) {
  const d = await api.productDetail(row.id)
  Object.assign(form, {
    id: d.id,
    name: d.name,
    mainImage: d.mainImage,
    detailImages: d.detailImages || [],
    categoryId: d.categoryId,
    tagId: d.tagId || undefined,
    recommended: d.recommended,
    highlights: d.highlights || [],
    perOrderLimit: d.perOrderLimit || 0,
    skus: (d.skus || []).map((s) => ({ id: s.id, name: s.name, price: Number(s.price), stock: s.stock })),
  })
  if (!form.skus.length) form.skus = [blankSku()]
  highlightText.value = (d.highlights || []).join('\n')
  dialog.visible = true
}

function addSku() {
  form.skus.push(blankSku())
}
function removeSku(i) {
  if (form.skus.length <= 1) return ElMessage.warning('至少保留一个规格')
  form.skus.splice(i, 1)
}

async function onMainUpload(file) {
  form.mainImage = await uploadFile(file.file)
  ElMessage.success('主图已上传')
  return false
}
async function onDetailUpload(file) {
  const url = await uploadFile(file.file)
  form.detailImages.push(url)
  return false
}
function removeDetail(i) {
  form.detailImages.splice(i, 1)
}

async function submit() {
  await formRef.value.validate()
  form.highlights = highlightText.value
    .split('\n')
    .map((s) => s.trim())
    .filter(Boolean)
  dialog.saving = true
  try {
    await api.productSave({
      id: form.id,
      name: form.name,
      mainImage: form.mainImage,
      detailImages: form.detailImages,
      categoryId: form.categoryId,
      tagId: form.tagId || null,
      recommended: form.recommended,
      highlights: form.highlights,
      perOrderLimit: form.perOrderLimit,
      skus: form.skus.map((s, idx) => ({ ...s, sortOrder: idx })),
    })
    ElMessage.success('已保存')
    dialog.visible = false
    load()
  } finally {
    dialog.saving = false
  }
}

// ---- 分类 / 标签抽屉 ----
const catDrawer = ref(false)
const tagDrawer = ref(false)

onMounted(() => {
  loadCategories()
  loadTags()
  load()
})
</script>

<template>
  <div>
    <div class="toolbar">
      <el-select v-model="query.categoryId" placeholder="全部分类" clearable style="width: 160px">
        <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px">
        <el-option label="上架" :value="1" />
        <el-option label="下架" :value="0" />
      </el-select>
      <el-input
        v-model="query.keyword"
        placeholder="商品名称"
        clearable
        style="width: 220px"
        @keyup.enter="search"
      />
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="resetQuery">重置</el-button>
      <div class="toolbar-spacer" />
      <el-button @click="catDrawer = true">分类管理</el-button>
      <el-button @click="tagDrawer = true">标签管理</el-button>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增商品</el-button>
    </div>

    <div class="card">
      <el-table v-loading="loading" :data="list" style="width: 100%">
        <el-table-column label="商品" min-width="280">
          <template #default="{ row }">
            <div class="prod-cell">
              <el-image :src="row.mainImage" fit="cover" class="prod-img" />
              <div class="prod-meta">
                <div class="prod-name">{{ row.name }}</div>
                <div class="prod-tags">
                  <el-tag v-if="row.recommended" size="small" type="warning">推荐</el-tag>
                  <el-tag v-if="row.tagName" size="small">{{ row.tagName }}</el-tag>
                </div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="分类" prop="categoryName" width="120" />
        <el-table-column label="价格" width="120" align="right">
          <template #default="{ row }">{{ yuan(row.minPrice) }}</template>
        </el-table-column>
        <el-table-column label="库存" width="120" align="right">
          <template #default="{ row }">
            <span v-if="row.soldOut" class="stock-danger">售罄</span>
            <span v-else :class="{ 'stock-warn': row.lowStock }">{{ row.totalStock }}</span>
          </template>
        </el-table-column>
        <el-table-column label="销量" prop="salesCount" width="100" align="right" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isValid === 1 ? 'success' : 'info'" size="small">
              {{ row.isValid === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <div class="op-btns">
              <span class="link-text" @click="openEdit(row)">编辑</span>
              <span class="link-text" @click="toggleStatus(row)">{{ row.isValid === 1 ? '下架' : '上架' }}</span>
              <span class="link-text danger" @click="remove(row)">删除</span>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="load"
          @size-change="search"
        />
      </div>
    </div>

    <!-- 编辑弹窗 -->
    <el-dialog
      v-model="dialog.visible"
      :title="form.id ? '编辑商品' : '新增商品'"
      width="720px"
      top="6vh"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="form.name" maxlength="128" show-word-limit />
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="选择分类" style="width: 220px">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
          <el-select v-model="form.tagId" placeholder="标签(可空)" clearable style="width: 180px; margin-left: 12px">
            <el-option v-for="t in tags" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
          <el-checkbox v-model="form.recommended" style="margin-left: 16px">推荐</el-checkbox>
        </el-form-item>
        <el-form-item label="主图" prop="mainImage">
          <el-upload :show-file-list="false" :http-request="onMainUpload" accept="image/*">
            <el-image v-if="form.mainImage" :src="form.mainImage" fit="cover" class="up-img" />
            <div v-else class="up-btn"><el-icon><Plus /></el-icon></div>
          </el-upload>
        </el-form-item>
        <el-form-item label="详情图">
          <div class="detail-imgs">
            <div v-for="(img, i) in form.detailImages" :key="i" class="detail-item">
              <el-image :src="img" fit="cover" class="up-img" />
              <el-icon class="detail-del" @click="removeDetail(i)"><Delete /></el-icon>
            </div>
            <el-upload :show-file-list="false" :http-request="onDetailUpload" accept="image/*">
              <div class="up-btn"><el-icon><Plus /></el-icon></div>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item label="产品亮点">
          <el-input
            v-model="highlightText"
            type="textarea"
            :rows="3"
            placeholder="每行一条"
          />
        </el-form-item>
        <el-form-item label="每单限购">
          <el-input-number v-model="form.perOrderLimit" :min="0" />
          <span class="hint">0 表示不限购</span>
        </el-form-item>
        <el-form-item label="规格">
          <div class="sku-list">
            <div v-for="(s, i) in form.skus" :key="i" class="sku-row">
              <el-input v-model="s.name" placeholder="规格名" style="width: 180px" />
              <el-input-number v-model="s.price" :min="0" :precision="2" :step="0.1" controls-position="right" />
              <el-input-number v-model="s.stock" :min="0" controls-position="right" />
              <el-icon class="sku-del" @click="removeSku(i)"><Delete /></el-icon>
            </div>
            <el-button link type="primary" :icon="Plus" @click="addSku">添加规格</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="dialog.saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 分类管理 -->
    <CategoryDrawer v-model="catDrawer" :categories="categories" @refresh="loadCategories" />
    <!-- 标签管理 -->
    <TagDrawer v-model="tagDrawer" :tags="tags" @refresh="loadTags" />
  </div>
</template>

<style lang="scss" scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.toolbar-spacer {
  flex: 1;
}
.card {
  background: #fff;
  border-radius: $radius-card;
  padding: 16px 20px;
  border: 1px solid $border;
}
.prod-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}
.prod-img {
  width: 48px;
  height: 48px;
  border-radius: $radius-control;
  flex-shrink: 0;
}
.prod-name {
  font-size: 14px;
  color: $text-title;
}
.prod-tags {
  margin-top: 4px;
  display: flex;
  gap: 6px;
}
.stock-danger {
  color: $danger;
}
.stock-warn {
  color: $warning;
}
.link-text.danger {
  color: $danger;
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
.up-img {
  width: 88px;
  height: 88px;
  border-radius: $radius-control;
  border: 1px solid $border;
}
.up-btn {
  width: 88px;
  height: 88px;
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
.detail-imgs {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.detail-item {
  position: relative;
}
.detail-del {
  position: absolute;
  top: -6px;
  right: -6px;
  background: $danger;
  color: #fff;
  border-radius: 50%;
  padding: 3px;
  cursor: pointer;
  font-size: 12px;
}
.hint {
  margin-left: 12px;
  color: $text-weak;
  font-size: 12px;
}
.sku-list {
  width: 100%;
}
.sku-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}
.sku-del {
  color: $danger;
  cursor: pointer;
}
</style>
