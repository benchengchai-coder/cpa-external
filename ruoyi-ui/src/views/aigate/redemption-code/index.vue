<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px" class="redemption-search-form">
      <el-form-item label="名称" prop="codeName">
        <el-input v-model="queryParams.codeName" placeholder="请输入名称" clearable class="redemption-search-control" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="兑换码" prop="redemptionKey">
        <el-input v-model="queryParams.redemptionKey" placeholder="请输入兑换码" clearable class="redemption-search-control" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable class="redemption-search-control">
          <el-option v-for="item in redemptionCodeStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item class="redemption-search-button-item">
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="mb8 redemption-toolbar">
      <div class="redemption-toolbar-actions">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['aigate:redemptionCode:add']">兑换码</el-button>
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate()" v-hasPermi="['aigate:redemptionCode:edit']">修改</el-button>
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()" v-hasPermi="['aigate:redemptionCode:remove']">删除</el-button>
      </div>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </div>

    <el-table v-loading="loading" :data="codeList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" align="center" prop="codeId" width="80" />
      <el-table-column label="名称" align="center" prop="codeName" :show-overflow-tooltip="true" width="120"/>
      <el-table-column label="额度" align="center" prop="quota" width="120">
        <template #default="scope">{{ formatCurrency(scope.row.quota, 0) }}</template>
      </el-table-column>
      <el-table-column label="兑换码" align="center" prop="redemptionKey" >
        <template #default="scope">
          <span style="font-family: monospace; letter-spacing: 0.5px">{{ scope.row.redemptionKey }}</span>
          <el-button link type="primary" icon="DocumentCopy" @click="handleCopy(scope.row.redemptionKey)" style="margin-left: 6px" />
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template #default="scope">
          <el-tag :type="statusTagType(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="过期时间" align="center" prop="expiredTime" width="170">
        <template #default="scope">{{ scope.row.expiredTime ? parseTime(scope.row.expiredTime) : '永不过期' }}</template>
      </el-table-column>
      <el-table-column label="兑换人" align="center" prop="usedUsername" width="100">
        <template #default="scope">{{ scope.row.usedUsername || '-' }}</template>
      </el-table-column>
      <el-table-column label="兑换时间" align="center" prop="redeemedTime" width="170">
        <template #default="scope">{{ parseTime(scope.row.redeemedTime) }}</template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="170">
        <template #default="scope">{{ parseTime(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="160" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['aigate:redemptionCode:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['aigate:redemptionCode:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-drawer :title="title" v-model="open" direction="rtl" :size="drawerSize" append-to-body>
      <el-form ref="codeRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="名称" prop="codeName">
          <el-input v-model="form.codeName" placeholder="请输入名称" />
        </el-form-item>
        <el-form-item label="额度" prop="quota">
          <el-input-number v-model="form.quota" :min="0" :precision="0" :step="1" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="过期时间" prop="expiredTime">
          <el-date-picker v-model="form.expiredTime" type="datetime" placeholder="留空表示永不过期" clearable value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item v-if="form.codeId === undefined" label="生成数量" prop="batchCount">
          <el-input-number v-model="form.batchCount" :min="1" :max="100" :step="1" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item v-if="form.codeId !== undefined" label="状态" prop="status">
          <el-radio-group v-model="form.status" :disabled="form.status === '1'">
            <el-radio v-for="item in editableStatusOptions" :key="item.value" :value="item.value">{{ item.label }}</el-radio>
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

<script setup lang="ts" name="AiRedemptionCode">
import { batchAddAiRedemptionCode, delAiRedemptionCode, getAiRedemptionCode, listAiRedemptionCode, updateAiRedemptionCode } from "@/api/aigate/redemptionCode";
import type { AiRedemptionCode, AiRedemptionCodeQueryParams } from "@/types";
import { formatCurrency, redemptionCodeStatusOptions, useAiDrawerSize } from "../common";

const { proxy } = getCurrentInstance() as any;

const drawerSize = useAiDrawerSize();
const codeList = ref<AiRedemptionCode[]>([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref<number[]>([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");

const editableStatusOptions = redemptionCodeStatusOptions.filter(item => item.value !== "1");

const data = reactive({
  form: {} as AiRedemptionCode,
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    codeName: undefined,
    redemptionKey: undefined,
    status: undefined
  } as AiRedemptionCodeQueryParams,
  rules: {
    codeName: [{ required: true, message: "名称不能为空", trigger: "blur" }],
    quota: [{ required: true, message: "额度不能为空", trigger: "blur" }]
  }
});

const { queryParams, form, rules } = toRefs(data);

function statusTagType(status: string | undefined)
{
  if (status === "0") return "success";
  if (status === "1") return "info";
  if (status === "2") return "danger";
  return "info";
}

function statusLabel(status: string | undefined)
{
  const item = redemptionCodeStatusOptions.find(o => o.value === status);
  return item ? item.label : "-";
}

function normalizeQuota(value: number | string | undefined)
{
  const quota = Number(value);
  return Number.isFinite(quota) ? Math.max(0, Math.round(quota)) : 0;
}

function handleCopy(text: string | undefined)
{
  if (!text) return;
  navigator.clipboard.writeText(text).then(() => {
    proxy.$modal.msgSuccess("复制成功");
  }).catch(() => {
    proxy.$modal.msgError("复制失败");
  });
}

function getList()
{
  loading.value = true;
  listAiRedemptionCode(queryParams.value).then((response) => {
    codeList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
}

function cancel()
{
  open.value = false;
  reset();
}

function reset()
{
  form.value = {
    codeId: undefined,
    codeName: undefined,
    quota: 1,
    redemptionKey: undefined,
    status: "0",
    expiredTime: undefined,
    usedUserId: undefined,
    usedUsername: undefined,
    redeemedTime: undefined,
    batchCount: 1,
    remark: undefined
  };
  proxy.resetForm("codeRef");
}

function handleQuery()
{
  queryParams.value.pageNum = 1;
  getList();
}

function resetQuery()
{
  proxy.resetForm("queryRef");
  handleQuery();
}

function handleSelectionChange(selection: AiRedemptionCode[])
{
  ids.value = selection.map((item) => item.codeId!);
  single.value = selection.length !== 1;
  multiple.value = !selection.length;
}

function handleAdd()
{
  reset();
  open.value = true;
  title.value = "生成兑换码";
}

function handleUpdate(row?: AiRedemptionCode)
{
  reset();
  const codeId = row?.codeId || ids.value[0];
  getAiRedemptionCode(codeId).then((response) => {
    form.value = response.data || {};
    form.value.quota = normalizeQuota(form.value.quota);
    open.value = true;
    title.value = "修改兑换码";
  });
}

function submitForm()
{
  proxy.$refs["codeRef"].validate((valid: boolean) => {
    if (valid)
    {
      form.value.quota = normalizeQuota(form.value.quota);
      if (form.value.codeId !== undefined)
      {
        updateAiRedemptionCode(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      }
      else
      {
        batchAddAiRedemptionCode(form.value).then(() => {
          proxy.$modal.msgSuccess("生成成功");
          open.value = false;
          getList();
        });
      }
    }
  });
}

function handleDelete(row?: AiRedemptionCode)
{
  const codeIds = row?.codeId || ids.value;
  proxy.$modal.confirm('是否确认删除兑换码编号为"' + codeIds + '"的数据项？').then(() => {
    return delAiRedemptionCode(codeIds);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

getList();
</script>

<style scoped>
.redemption-search-form {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
}

.redemption-search-form :deep(.redemption-search-control) {
  width: 200px;
}

.redemption-toolbar {
  display: flex;
  align-items: center;
  width: 100%;
  gap: 10px;
}

.redemption-toolbar-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  min-width: 0;
  gap: 10px;
}

.redemption-toolbar-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.redemption-toolbar :deep(.top-right-btn) {
  margin-left: auto;
}

@media (max-width: 768px) {
  .redemption-search-form {
    width: 100%;
  }

  .redemption-search-form :deep(.el-form-item) {
    flex: 1 0 100%;
    width: 100%;
    max-width: 100%;
    margin-right: 0;
    margin-bottom: 8px;
  }

  .redemption-search-form :deep(.el-form-item__content) {
    flex: 1;
    min-width: 0;
  }

  .redemption-search-form :deep(.redemption-search-control) {
    width: 100%;
  }

  .redemption-search-button-item :deep(.el-form-item__content) {
    display: flex;
    width: 100%;
    gap: 10px;
  }

  .redemption-search-button-item :deep(.el-button) {
    flex: 1;
    min-width: 0;
  }

  .redemption-search-button-item :deep(.el-button + .el-button) {
    margin-left: 0;
  }

  .redemption-toolbar {
    flex-wrap: wrap;
  }

  .redemption-toolbar-actions {
    width: 100%;
  }

  .redemption-toolbar :deep(.top-right-btn) {
    width: 100%;
    justify-content: flex-end;
    margin-left: 0;
  }
}
</style>
