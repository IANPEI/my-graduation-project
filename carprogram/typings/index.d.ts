// typings/index.d.ts
/// <reference path="./types/index.d.ts" /> // 保留你的原有引用

declare namespace WechatMiniprogram {
  namespace App {
    // 扩展官方 Instance 类型
    interface Instance<T = any> {
      globalData: T; // 支持任意结构的 globalData
      // 你的自定义方法
      initLoginStatus(options: LaunchShowOption): void;
      checkLoginStatus(): void;
      getHomeUrlByIdentity(identity: string): string;
      // 补充你原来的 userInfo 相关属性
      userInfoReadyCallback?: GetUserInfoSuccessCallback;
    }
  }
}