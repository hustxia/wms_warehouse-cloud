<template>
  <div class="login-wrap">
    <div class="login-deco">
      <span></span><span></span><span></span>
    </div>
    <div class="login-card">
      <div class="login-brand">
        <div class="login-logo">w</div>
        <h2>wms_warehouse</h2>
        <p>仓储管理系统</p>
      </div>
      <el-form @submit.native.prevent="doLogin">
        <el-form-item>
          <el-input v-model="form.username" prefix-icon="el-icon-user" placeholder="用户名" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" prefix-icon="el-icon-lock" type="password" placeholder="密码" show-password />
        </el-form-item>
        <el-form-item>
          <div class="captcha-row">
            <el-input v-model="form.captchaAnswer" prefix-icon="el-icon-key" placeholder="计算结果" @keyup.enter.native="doLogin" />
            <img v-if="captchaImage" :src="captchaImage" class="captcha-img" title="点击刷新" @click="loadCaptcha" />
          </div>
        </el-form-item>
        <el-button type="primary" :loading="loading" native-type="submit" class="login-btn">登 录</el-button>
      </el-form>
      <p class="tip">默认账号 admin / admin123</p>
    </div>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'Login',
  data() {
    return {
      form: { username: 'admin', password: '', captchaId: '', captchaAnswer: '' },
      captchaImage: '',
      loading: false
    }
  },
  created() {
    this.loadCaptcha()
  },
  methods: {
    async loadCaptcha() {
      const res = await request.get('/auth/captcha')
      this.captchaImage = res.data.image
      this.form.captchaId = res.data.captchaId
      this.form.captchaAnswer = ''
    },
    async doLogin() {
      if (!this.form.username || !this.form.password) return
      if (!this.form.captchaAnswer) {
        this.$message.warning('请输入验证码')
        return
      }
      this.loading = true
      try {
        const res = await request.post('/auth/login', this.form)
        localStorage.setItem('token', res.data.token)
        localStorage.setItem('username', res.data.username)
        // V6.4：perms 落地供 v-hasPermi 指令与后端权限校验对齐
        localStorage.setItem('perms', JSON.stringify(res.data.perms || []))
        this.$router.push('/')
      } catch (e) {
        // 验证码一次性：失败（含密码错）后必须换新码，否则重放必错
        this.loadCaptcha()
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.login-wrap {
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background: linear-gradient(135deg, #141b2e 0%, #1c2745 60%, #2a3558 100%);
}
/* 背景装饰光斑 */
.login-deco span {
  position: absolute;
  border-radius: 50%;
  filter: blur(2px);
}
.login-deco span:nth-child(1) {
  width: 420px;
  height: 420px;
  left: -120px;
  top: -140px;
  background: radial-gradient(circle, rgba(225, 37, 27, 0.28) 0%, transparent 70%);
}
.login-deco span:nth-child(2) {
  width: 340px;
  height: 340px;
  right: -80px;
  bottom: -100px;
  background: radial-gradient(circle, rgba(77, 124, 254, 0.22) 0%, transparent 70%);
}
.login-deco span:nth-child(3) {
  width: 200px;
  height: 200px;
  right: 18%;
  top: 12%;
  background: radial-gradient(circle, rgba(255, 138, 61, 0.16) 0%, transparent 70%);
}

.login-card {
  position: relative;
  z-index: 1;
  width: 380px;
  padding: 34px 36px 26px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.97);
  box-shadow: 0 24px 60px rgba(8, 12, 24, 0.45);
}
.login-brand {
  text-align: center;
  margin-bottom: 22px;
}
.login-logo {
  width: 52px;
  height: 52px;
  line-height: 52px;
  margin: 0 auto 10px;
  border-radius: 15px;
  background: linear-gradient(135deg, var(--app-primary) 0%, #ff6a3d 100%);
  color: #fff;
  font-weight: 700;
  font-size: 22px;
  box-shadow: 0 6px 16px rgba(225, 37, 27, 0.35);
}
.login-brand h2 {
  margin: 0;
  font-size: 20px;
  color: #1f2937;
  letter-spacing: 0.5px;
}
.login-brand p {
  margin: 6px 0 0;
  font-size: 12px;
  color: #98a2b8;
  letter-spacing: 3px;
}
.captcha-row {
  display: flex;
  gap: 8px;
  width: 100%;
}
.captcha-img {
  height: 40px;
  cursor: pointer;
  border-radius: 8px;
}
.login-btn {
  width: 100%;
  height: 42px;
  font-size: 15px;
  letter-spacing: 6px;
}
.tip {
  margin: 14px 0 0;
  text-align: center;
  color: #a5aec4;
  font-size: 12px;
}
</style>
