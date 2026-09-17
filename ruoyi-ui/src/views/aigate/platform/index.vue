<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px" class="platform-search-form">
      <el-form-item label="平台" prop="platformVal">
        <el-select v-model="queryParams.platformVal" placeholder="请选择平台" clearable filterable class="platform-search-control">
          <el-option v-for="item in ai_platform_name" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable class="platform-search-control">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item class="platform-search-button-item">
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="mb8 platform-toolbar">
      <div class="platform-toolbar-actions">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['aigate:platform:add']">新增</el-button>
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate()" v-hasPermi="['aigate:platform:edit']">修改</el-button>
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()" v-hasPermi="['aigate:platform:remove']">删除</el-button>
      </div>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </div>

    <el-table v-loading="loading" :data="platformList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" align="center" prop="platformId" width="80" />
      <el-table-column label="平台名称" align="center" prop="platformName" :show-overflow-tooltip="true">
        <template #default="scope">
          {{ scope.row.platformName || "-" }}
        </template>
      </el-table-column>
      <el-table-column label="平台标识" align="center" prop="platformVal" width="120">
        <template #default="scope">
          {{ scope.row.platformVal || "-" }}
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="scope">
          <el-switch v-model="scope.row.status" active-value="0" inactive-value="1" active-text="正常" inactive-text="停用" inline-prompt @change="handleStatusChange(scope.row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="160" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['aigate:platform:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['aigate:platform:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-drawer :title="title" v-model="open" direction="rtl" :size="drawerSize" append-to-body>
      <el-form ref="platformRef" :model="form" :rules="rules" label-width="92px">
        <el-form-item label="平台名称" prop="platformVal">
          <el-select v-model="form.platformVal" placeholder="请选择平台" filterable style="width: 100%" @change="handlePlatformChange">
            <el-option v-for="item in ai_platform_name" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio v-for="item in statusOptions" :key="item.value" :value="item.value">{{ item.label }}</el-radio>
          </el-radio-group>
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

<script setup lang="ts" name="AiPlatform">
import { addAiPlatform, delAiPlatform, getAiPlatform, listAiPlatform, updateAiPlatform } from "@/api/aigate/platform";
import type { AiPlatformRecord, AiPlatformRecordQueryParams } from "@/types";
import { statusOptions, useAiDrawerSize } from "../common";

const { proxy } = getCurrentInstance() as any;
const { ai_platform_name } = useDict("ai_platform_name");

const drawerSize = useAiDrawerSize();
const platformList = ref<AiPlatformRecord[]>([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref<number[]>([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");

const data = reactive({
  form: {} as AiPlatformRecord,
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    platformVal: undefined,
    platformName: undefined,
    status: undefined
  } as AiPlatformRecordQueryParams,
  rules: {
    platformVal: [{ required: true, message: "平台名称不能为空", trigger: "change" }],
    status: [{ required: true, message: "状态不能为空", trigger: "change" }]
  }
});

const { queryParams, form, rules } = toRefs(data);

function getList() {
  loading.value = true;
  listAiPlatform(queryParams.value).then((response) => {
    platformList.value = response.rows;
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
    platformId: undefined,
    platformVal: undefined,
    platformName: undefined,
    status: "0",
    remark: undefined
  };
  proxy.resetForm("platformRef");
}

function findPlatformDictItem(value?: string) {
  return (ai_platform_name.value || []).find((item: any) => item.value === value);
}

function resolvePlatformDictItem(record: AiPlatformRecord) {
  return (ai_platform_name.value || []).find((item: any) => item.value === record.platformVal || item.label === record.platformName);
}

function syncPlatformFields() {
  const item = findPlatformDictItem(form.value.platformVal);
  if (item) {
    form.value.platformName = item.label;
  }
}

function handlePlatformChange() {
  syncPlatformFields();
}

function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

function resetQuery() {
  proxy.resetForm("queryRef");
  handleQuery();
}

function handleSelectionChange(selection: AiPlatformRecord[]) {
  ids.value = selection.map((item) => item.platformId!);
  single.value = selection.length !== 1;
  multiple.value = !selection.length;
}

function handleAdd() {
  reset();
  open.value = true;
  title.value = "添加AI平台";
}

function handleUpdate(row?: AiPlatformRecord) {
  reset();
  const platformId = row?.platformId || ids.value[0];
  getAiPlatform(platformId).then((response) => {
    form.value = response.data || {};
    const item = resolvePlatformDictItem(form.value);
    if (item) {
      form.value.platformVal = item.value;
      form.value.platformName = item.label;
    }
    open.value = true;
    title.value = "修改AI平台";
  });
}

function handleStatusChange(row: AiPlatformRecord) {
  updateAiPlatform(row).then(() => {
    proxy.$modal.msgSuccess("修改成功");
  }).catch(() => {
    getList();
  });
}

function submitForm() {
  syncPlatformFields();
  proxy.$refs["platformRef"].validate((valid: boolean) => {
    if (valid) {
      const request = form.value.platformId !== undefined ? updateAiPlatform(form.value) : addAiPlatform(form.value);
      request.then(() => {
        proxy.$modal.msgSuccess(form.value.platformId !== undefined ? "修改成功" : "新增成功");
        open.value = false;
        getList();
      });
    }
  });
}

function handleDelete(row?: AiPlatformRecord) {
  const platformIds = row?.platformId || ids.value;
  proxy.$modal.confirm('是否确认删除AI平台编号为"' + platformIds + '"的数据项？').then(() => {
    return delAiPlatform(platformIds);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

getList();
</script>

<style scoped>
.platform-search-form {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
}

.platform-search-form :deep(.platform-search-control) {
  width: 200px;
}

.platform-toolbar {
  display: flex;
  align-items: center;
  width: 100%;
  gap: 10px;
}

.platform-toolbar-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  min-width: 0;
  gap: 10px;
}

.platform-toolbar-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.platform-toolbar :deep(.top-right-btn) {
  margin-left: auto;
}

@media (max-width: 768px) {
  .platform-search-form {
    width: 100%;
  }

  .platform-search-form :deep(.el-form-item) {
    flex: 1 0 100%;
    width: 100%;
    max-width: 100%;
    margin-right: 0;
    margin-bottom: 8px;
  }

  .platform-search-form :deep(.el-form-item__content) {
    flex: 1;
    min-width: 0;
  }

  .platform-search-form :deep(.platform-search-control) {
    width: 100%;
  }

  .platform-search-button-item :deep(.el-form-item__content) {
    display: flex;
    width: 100%;
    gap: 10px;
  }

  .platform-search-button-item :deep(.el-button) {
    flex: 1;
    min-width: 0;
  }

  .platform-search-button-item :deep(.el-button + .el-button) {
    margin-left: 0;
  }

  .platform-toolbar {
    flex-wrap: wrap;
  }

  .platform-toolbar-actions {
    width: 100%;
  }

  .platform-toolbar :deep(.top-right-btn) {
    width: 100%;
    justify-content: flex-end;
    margin-left: 0;
  }
}
</style>
