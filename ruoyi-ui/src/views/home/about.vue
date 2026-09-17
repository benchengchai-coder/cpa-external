<template>
  <div class="about-page">
    <div class="page-inner">
      <h1 class="page-title">关于这个项目</h1>

      <el-row :gutter="48">
        <el-col :xs="24" :sm="24">
          <div class="about-content">
            <h2>为什么会有这个项目？</h2>
            <p>
              本人在职coder，自己订阅的官方美区正价套餐，大多数时候额度用不完，所以才有这个分发项目。
            </p>

            <h2>解决您的担心</h2>
            <p>1.域名是我一直持有的白金域名，价格高昂。 </p>
            <p>2.美国固定IP服务器，快捷高速，非市面自己搭建的科学上网机。</p>

            <p><b>所以，您不必担心使用的问题，如果出现故障无法使用，决定不再使用，会按情况退款。</b></p>

            <h2>联系方式</h2>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="官方网站">https://cbc.icu</el-descriptions-item>
              <el-descriptions-item label="邮箱">cbcicu@qq.com</el-descriptions-item>
            </el-descriptions>
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup lang="ts">

</script>

<style lang="scss" scoped>
.about-page {
  width: 100%;
  background: #fff;
}

.page-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 60px 24px;
}

.page-title {
  font-size: 32px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 40px;
}

.about-content {
  h2 {
    font-size: 22px;
    color: #303133;
    margin-top: 32px;
    margin-bottom: 16px;
  }

  p {
    font-size: 15px;
    color: #606266;
    line-height: 1.8;
    margin-bottom: 16px;
  }
}

.about-card {
  margin-bottom: 24px;

  .tech-tag {
    margin: 4px;
  }
}

.stat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;

  &:last-child {
    border-bottom: none;
  }

  .stat-value {
    font-size: 18px;
    font-weight: 600;
    color: #667eea;
  }

  .stat-label {
    font-size: 14px;
    color: #909399;
  }
}
</style>
