import { createApp } from "vue";
import App from "./App.vue";
import router from "./router";
import ElementPlus from "element-plus";
import "element-plus/dist/index.css";

// 导入全局样式
import "./styles/common.css";
import "./styles/element-variables.css";
import zhCn from "element-plus/es/locale/lang/zh-cn";

const app = createApp(App);
app.use(router);
app.mount("#app");
app.use(ElementPlus, {
  locale: zhCn,
});
