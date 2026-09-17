<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px" class="model-search-form">
      <el-form-item label="模型名称" prop="modelName">
        <el-input v-model="queryParams.modelName" placeholder="请输入模型名称" clearable class="model-search-control" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="平台" prop="platform">
        <el-select v-model="queryParams.platform" placeholder="请选择平台" clearable filterable class="model-search-control">
          <el-option v-for="item in ai_platform_name" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable class="model-search-control">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item class="model-search-button-item">
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="mb8 model-toolbar">
      <div class="model-toolbar-actions">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['aigate:model:add']">新增</el-button>
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate()" v-hasPermi="['aigate:model:edit']">修改</el-button>
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()" v-hasPermi="['aigate:model:remove']">删除</el-button>
      </div>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </div>

    <el-table v-loading="loading" :data="modelList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" align="center" prop="modelId" width="80" />
      <el-table-column label="平台" align="center" prop="platform" width="140">
        <template #default="scope">
          <dict-tag :options="ai_platform_name" :value="scope.row.platform" />
        </template>
      </el-table-column>
      <el-table-column label="模型名称" align="center" prop="modelName" min-width="160" :show-overflow-tooltip="true" />
     
      <el-table-column label="输入价" align="center" prop="officialInputPrice" width="100">
        <template #default="scope">{{ formatCurrency(scope.row.officialInputPrice, 6, true) }}</template>
      </el-table-column>
      <el-table-column label="输出价" align="center" prop="officialOutputPrice" width="100">
        <template #default="scope">{{ formatCurrency(scope.row.officialOutputPrice, 6, true) }}</template>
      </el-table-column>
      <el-table-column label="缓存读" align="center" prop="officialCacheReadPrice" width="100">
        <template #default="scope">{{ formatCurrency(scope.row.officialCacheReadPrice, 6, true) }}</template>
      </el-table-column>
      <el-table-column label="缓存写" align="center" prop="officialCacheWritePrice" width="100">
        <template #default="scope">{{ formatCurrency(scope.row.officialCacheWritePrice, 6, true) }}</template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="scope">
          <el-switch v-model="scope.row.status" active-value="0" inactive-value="1" active-text="正常" inactive-text="停用" inline-prompt @change="handleStatusChange(scope.row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="160" fixed="right" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['aigate:model:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['aigate:model:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-drawer :title="title" v-model="open" direction="rtl" :size="drawerSize" append-to-body>
      <el-form ref="modelRef" :model="form" :rules="rules" label-width="96px">
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="平台" prop="platform">
              <el-select v-model="form.platform" placeholder="请选择平台" filterable style="width: 100%">
                <el-option v-for="item in ai_platform_name" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :span="24">
            <el-form-item label="模型名称" prop="modelName">
              <el-input v-model="form.modelName" placeholder="请输入模型名称" />
            </el-form-item>
          </el-col>
      
          <el-col :span="24">
            <el-form-item label="输入价格" prop="officialInputPrice">
              <el-input-number v-model="form.officialInputPrice" :min="0" :step="1" style="width: 100%">
                <template #prefix>$</template>
              </el-input-number>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="输出价格" prop="officialOutputPrice">
              <el-input-number v-model="form.officialOutputPrice" :min="0" :step="1" style="width: 100%">
                <template #prefix>$</template>
              </el-input-number>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="缓存读价" prop="officialCacheReadPrice">
              <el-input-number v-model="form.officialCacheReadPrice" :min="0" :step="1" style="width: 100%">
                <template #prefix>$</template>
              </el-input-number>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="缓存写价" prop="officialCacheWritePrice">
              <el-input-number v-model="form.officialCacheWritePrice" :min="0" :step="1" style="width: 100%">
                <template #prefix>$</template>
              </el-input-number>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="图标" prop="icon">
              <el-input v-model="form.icon" placeholder="请输入图标标识或地址" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="状态" prop="status">
          <div class="model-switch-row">
            <el-switch v-model="form.status" active-value="0" inactive-value="1" active-text="正常" inactive-text="停用" inline-prompt />
          </div>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts" name="AiModel">
import { addAiModel, delAiModel, getAiModel, listAiModel, updateAiModel } from "@/api/aigate/model";
import type { AiModel, AiModelQueryParams } from "@/types";
import { formatCurrency, statusOptions, useAiDrawerSize } from "../common";

const { proxy } = getCurrentInstance() as any;
const { ai_platform_name } = useDict("ai_platform_name");

const drawerSize = useAiDrawerSize();

const modelList = ref<AiModel[]>([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref<number[]>([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");

const data = reactive({
  form: {} as AiModel,
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    modelName: undefined,
    platform: undefined,
    status: undefined
  } as AiModelQueryParams,
  rules: {
    modelName: [{ required: true, message: "模型名称不能为空", trigger: "blur" }],
    platform: [{ required: true, message: "平台不能为空", trigger: "change" }],
    officialInputPrice: [{ required: true, message: "输入价格不能为空", trigger: "blur" }],
    officialOutputPrice: [{ required: true, message: "输出价格不能为空", trigger: "blur" }],
    status: [{ required: true, message: "状态不能为空", trigger: "change" }]
  }
});

const { queryParams, form, rules } = toRefs(data);

function getList() {
  loading.value = true;
  listAiModel(queryParams.value).then((response) => {
    modelList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
}

function cancel() {
  open.value = false;
  reset();
}

function reset() {
  form.value = {
    modelId: undefined,
    modelName: undefined,
    description: undefined,
    icon: undefined,
    platform: "openai",
    officialInputPrice: 0,
    officialOutputPrice: 0,
    officialCacheReadPrice: undefined,
    officialCacheWritePrice: undefined,
    status: "0",
    remark: undefined
  };
  proxy.resetForm("modelRef");
}

function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

function resetQuery() {
  proxy.resetForm("queryRef");
  handleQuery();
}

function handleSelectionChange(selection: AiModel[]) {
  ids.value = selection.map((item) => item.modelId!);
  single.value = selection.length !== 1;
  multiple.value = !selection.length;
}

function handleAdd() {
  reset();
  open.value = true;
  title.value = "添加AI模型";
}

function handleUpdate(row?: AiModel) {
  reset();
  const modelId = row?.modelId || ids.value[0];
  getAiModel(modelId).then((response) => {
    form.value = response.data || {};
    open.value = true;
    title.value = "修改AI模型";
  });
}

function handleStatusChange(row: AiModel) {
  updateAiModel(row).then(() => {
    proxy.$modal.msgSuccess("修改成功");
  }).catch(() => {
    getList();
  });
}

function submitForm() {
  proxy.$refs["modelRef"].validate((valid: boolean) => {
    if (valid) {
      const request = form.value.modelId !== undefined ? updateAiModel(form.value) : addAiModel(form.value);
      request.then(() => {
        proxy.$modal.msgSuccess(form.value.modelId !== undefined ? "修改成功" : "新增成功");
        open.value = false;
        getList();
      });
    }
  });
}

function handleDelete(row?: AiModel) {
  const modelIds = row?.modelId || ids.value;
  proxy.$modal.confirm('是否确认删除AI模型编号为"' + modelIds + '"的数据项？').then(() => {
    return delAiModel(modelIds);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

getList();
</script>

<style scoped>
.model-search-form {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
}

.model-search-form :deep(.model-search-control) {
  width: 200px;
}

.model-toolbar {
  display: flex;
  align-items: center;
  width: 100%;
  gap: 10px;
}

.model-toolbar-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  min-width: 0;
  gap: 10px;
}

.model-toolbar-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.model-toolbar :deep(.top-right-btn) {
  margin-left: auto;
}

.model-switch-row {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  width: 100%;
}

@media (max-width: 768px) {
  .model-search-form {
    width: 100%;
  }

  .model-search-form :deep(.el-form-item) {
    flex: 1 0 100%;
    width: 100%;
    max-width: 100%;
    margin-right: 0;
    margin-bottom: 8px;
  }

  .model-search-form :deep(.el-form-item__content) {
    flex: 1;
    min-width: 0;
  }

  .model-search-form :deep(.model-search-control) {
    width: 100%;
  }

  .model-search-button-item :deep(.el-form-item__content) {
    display: flex;
    width: 100%;
    gap: 10px;
  }

  .model-search-button-item :deep(.el-button) {
    flex: 1;
    min-width: 0;
  }

  .model-search-button-item :deep(.el-button + .el-button) {
    margin-left: 0;
  }

  .model-toolbar {
    flex-wrap: wrap;
  }

  .model-toolbar-actions {
    width: 100%;
  }

  .model-toolbar :deep(.top-right-btn) {
    width: 100%;
    justify-content: flex-end;
    margin-left: 0;
  }
}
</style>
