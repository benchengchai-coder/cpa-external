<template>
   <div class="app-container profile-page">
      <el-row :gutter="20" class="profile-layout">
         <el-col :xs="24" :sm="24" :md="8" :lg="6">
            <el-card class="box-card profile-card">
               <template v-slot:header>
                 <div class="clearfix">
                   <span>个人信息</span>
                 </div>
               </template>
               <div>
                  <div class="text-center">
                     <userAvatar />
                  </div>
                  <ul class="list-group list-group-striped">
                     <li class="list-group-item profile-list-item">
                        <span class="profile-list-label"><svg-icon icon-class="user" />用户名称</span>
                        <span class="profile-list-value">{{ state.user.userName }}</span>
                     </li>
                     <li class="list-group-item profile-list-item">
                        <span class="profile-list-label"><svg-icon icon-class="phone" />手机号码</span>
                        <span class="profile-list-value">{{ state.user.phonenumber }}</span>
                     </li>
                     <li class="list-group-item profile-list-item">
                        <span class="profile-list-label"><svg-icon icon-class="email" />用户邮箱</span>
                        <span class="profile-list-value">{{ state.user.email }}</span>
                     </li>
                     <li class="list-group-item profile-list-item">
                        <span class="profile-list-label"><svg-icon icon-class="tree" />所属部门</span>
                        <span class="profile-list-value" v-if="state.user.dept">{{ state.user.dept.deptName }} / {{ state.postGroup }}</span>
                     </li>
                     <li class="list-group-item profile-list-item">
                        <span class="profile-list-label"><svg-icon icon-class="peoples" />所属角色</span>
                        <span class="profile-list-value">{{ state.roleGroup }}</span>
                     </li>
                     <li class="list-group-item profile-list-item">
                        <span class="profile-list-label"><svg-icon icon-class="date" />创建日期</span>
                        <span class="profile-list-value">{{ state.user.createTime }}</span>
                     </li>
                  </ul>
               </div>
            </el-card>
         </el-col>
         <el-col :xs="24" :sm="24" :md="16" :lg="18">
            <el-card class="profile-card">
               <template v-slot:header>
                 <div class="clearfix">
                   <span>基本资料</span>
                 </div>
               </template>
               <el-tabs v-model="selectedTab" class="profile-tabs">
                  <el-tab-pane label="基本资料" name="userinfo">
                     <userInfo :user="state.user" />
                  </el-tab-pane>
                  <el-tab-pane label="修改密码" name="resetPwd">
                     <resetPwd />
                  </el-tab-pane>
               </el-tabs>
            </el-card>
         </el-col>
      </el-row>
   </div>
</template>

<script setup lang="ts" name="Profile">
import userAvatar from "./userAvatar.vue"
import userInfo from "./userInfo.vue"
import resetPwd from "./resetPwd.vue"
import { getUserProfile } from "@/api/system/user"
import type { SysUser } from '@/types/api/system/user'

const route = useRoute()
const selectedTab = ref<string>("userinfo")

interface UserProfileState {
  user: SysUser
  roleGroup: string
  postGroup: string
}

const state = reactive<UserProfileState>({
  user: {} as SysUser,
  roleGroup: '',
  postGroup: ''
})

function getUser() {
  getUserProfile().then(response => {
    state.user = response.data
    state.roleGroup = response.roleGroup
    state.postGroup = response.postGroup
  })
}

onMounted(() => {
  const activeTab = route.params && route.params.activeTab
  if (activeTab) {
    selectedTab.value = activeTab as string
  }
  getUser()
})
</script>

<style lang="scss" scoped>
.profile-page {
  .profile-layout {
    row-gap: 20px;
  }

  .profile-card {
    height: 100%;
  }

  .profile-list-item {
    display: flex;
    min-width: 0;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    line-height: 1.5;
    white-space: nowrap;
  }

  .profile-list-label {
    display: inline-flex;
    flex: 0 0 auto;
    align-items: center;
    gap: 4px;
    color: var(--el-text-color-primary);
    white-space: nowrap;
  }

  .profile-list-value {
    min-width: 0;
    flex: 1;
    color: var(--el-text-color-regular);
    text-align: right;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

@media (max-width: 575px) {
  .profile-page {
    padding: 12px;

    :deep(.el-card__body) {
      padding: 14px !important;
    }

    .profile-layout {
      row-gap: 12px;
    }

    .profile-list-item {
      gap: 8px;
    }
  }
}
</style>
