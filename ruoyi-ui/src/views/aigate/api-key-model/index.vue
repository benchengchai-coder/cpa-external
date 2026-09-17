<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="API Key" prop="keyId">
        <el-select v-model="queryParams.keyId" placeholder="请选择API Key" clearable filterable style="width: 220px">
          <el-option v-for="item in apiKeyOptions" :key="item.keyId" :label="item.keyName" :value="item.keyId" />
        </el-select>
      </el-form-item>
      <el-form-item label="模型" prop="modelId">
        <el-select v-model="queryParams.modelId" placeholder="请选择模型" clearable filterable style="width: 220px">
          <el-option v-for="item in modelOptions" :key="item.modelId" :label="item.modelName" :value="item.modelId" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['aigate:apiKeyModel:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()" v-hasPermi="['aigate:apiKeyModel:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="apiKeyModelList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" align="center" prop="id" width="80" />
      <el-table-column label="API Key" align="center" prop="keyId">
        <template #default="scope">{{ optionLabel(apiKeyOptions, scope.row.keyId, "keyId", "keyName") }}</template>
      </el-table-column>
      <el-table-column label="模型" align="center" prop="modelId">
        <template #default="scope">{{ optionLabel(modelOptions, scope.row.modelId, "modelId", "modelName") }}</template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope">{{ parseTime(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="100" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['aigate:apiKeyModel:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-drawer :title="title" v-model="open" direction="rtl" :size="drawerSize" append-to-body>
      <el-form ref="apiKeyModelRef" :model="form" :rules="rules" label-width="92px">
        <el-form-item label="API Key" prop="keyId">
          <el-select v-model="form.keyId" placeholder="请选择API Key" filterable style="width: 100%">
            <el-option v-for="item in apiKeyOptions" :key="item.keyId" :label="item.keyName" :value="item.keyId" />
          </el-select>
        </el-form-item>
        <el-form-item label="模型" prop="modelId">
          <el-select v-model="form.modelId" placeholder="请选择模型" filterable style="width: 100%">
            <el-option v-for="item in modelOptions" :key="item.modelId" :label="item.modelName" :value="item.modelId" />
          </el-select>
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

<script setup lang="ts" name="AiApiKeyModel">
import { listAiApiKey } from "@/api/aigate/apiKey";
import { addAiApiKeyModel, delAiApiKeyModel, listAiApiKeyModel } from "@/api/aigate/apiKeyModel";
import { optionselectAiModel } from "@/api/aigate/model";
import type { AiApiKey, AiApiKeyModel, AiApiKeyModelQueryParams, AiModel } from "@/types";
import { optionLabel, useAiDrawerSize } from "../common";

const { proxy } = getCurrentInstance() as any;

const drawerSize = useAiDrawerSize();
const apiKeyOptions = ref<AiApiKey[]>([]);
const modelOptions = ref<AiModel[]>([]);
const apiKeyModelList = ref<AiApiKeyModel[]>([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref<number[]>([]);
const multiple = ref(true);
const total = ref(0);
const title = ref("");

const data = reactive({
  form: {} as AiApiKeyModel,
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    keyId: undefined,
    modelId: undefined
  } as AiApiKeyModelQueryParams,
  rules: {
    keyId: [{ required: true, message: "API Key不能为空", trigger: "change" }],
    modelId: [{ required: true, message: "模型不能为空", trigger: "change" }]
  }
});

const { queryParams, form, rules } = toRefs(data);

function loadOptions() {
  listAiApiKey({ pageNum: 1, pageSize: 1000, status: "0" }).then((response) => {
    apiKeyOptions.value = response.rows;
  });
  optionselectAiModel().then((response) => {
    modelOptions.value = response.data;
  });
}

function getList() {
  loading.value = true;
  listAiApiKeyModel(queryParams.value).then((response) => {
    apiKeyModelList.value = response.rows;
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
    id: undefined,
    keyId: undefined,
    modelId: undefined,
    remark: undefined
  };
  proxy.resetForm("apiKeyModelRef");
}

function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

function resetQuery() {
  proxy.resetForm("queryRef");
  handleQuery();
}

function handleSelectionChange(selection: AiApiKeyModel[]) {
  ids.value = selection.map((item) => item.id!);
  multiple.value = !selection.length;
}

function handleAdd() {
  reset();
  open.value = true;
  title.value = "添加Key模型限制";
}

function submitForm() {
  proxy.$refs["apiKeyModelRef"].validate((valid: boolean) => {
    if (valid) {
      addAiApiKeyModel(form.value).then(() => {
        proxy.$modal.msgSuccess("新增成功");
        open.value = false;
        getList();
      });
    }
  });
}

function handleDelete(row?: AiApiKeyModel) {
  const idsValue = row?.id || ids.value;
  proxy.$modal.confirm('是否确认删除Key模型限制编号为"' + idsValue + '"的数据项？').then(() => {
    return delAiApiKeyModel(idsValue);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

loadOptions();
getList();
</script>
