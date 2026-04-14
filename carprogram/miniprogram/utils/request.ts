const baseURL = "http://10.14.205.21:8089/rescue_admin";

// 保留原有 RequestOptions 接口，新增泛型 R（返回数据类型）
interface RequestOptions extends WechatMiniprogram.RequestOption {
  url: string;
  method?: "GET" | "POST" | "PUT" | "DELETE";
  data?: any;
  header?: Record<string, string>;
  showErrorToast?: boolean;
  // 新增：是否强制要求JSON格式（地址接口设为false）
  requireJsonResponse?: boolean;
  // 新增：是否忽略code判断（下单接口设为true）
  ignoreCodeCheck?: boolean;
}

// 核心修改：兼容无code的JSON返回，支持忽略code判断
export const request = <R = any>(options: RequestOptions) => {
  const {
    url,
    method = "GET",
    data = {},
    header = {},
    showErrorToast = true,
    requireJsonResponse = true, // 默认要求JSON，地址接口传false
    ignoreCodeCheck = false,    // 默认检查code，下单接口传true
    ...restOptions
  } = options;

  const token = wx.getStorageSync("token");
  const requestHeader: Record<string, string> = {
    "Content-Type": "application/json",
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...header,
  };

  // 返回值指定为 Promise<R>
  return new Promise<R>((resolve, reject) => {
    wx.request({
      url: baseURL + url,
      method,
      data,
      header: requestHeader,
      ...restOptions,
      success: (res) => {
        // 1. HTTP状态码非200：直接失败
        if (res.statusCode < 200 || res.statusCode >= 300) {
          showErrorToast && wx.showToast({
            title: `请求失败(${res.statusCode})`,
            icon: "none",
          });
          reject(res);
          return;
        }

        const responseData = res.data;

        // 2. 兼容纯文本返回（地址接口用这个逻辑）
        if (!requireJsonResponse) {
          resolve(responseData as R);
          return;
        }

        // 3. JSON格式：支持忽略code判断（下单接口用这个）
        try {
          const jsonData = typeof responseData === "string" 
            ? JSON.parse(responseData) 
            : responseData;
          
          // ✅ 核心修复：如果忽略code检查，直接resolve原始数据
          if (ignoreCodeCheck) {
            resolve(jsonData as R);
            return;
          }

          // 原有逻辑：检查code（其他接口用）
          if (jsonData.code !== 200) {
            showErrorToast && wx.showToast({
              title: jsonData.msg || "业务请求失败",
              icon: "none",
            });
            reject(jsonData);
            return;
          }
          // 优先返回data，没有则返回原始数据（兜底）
          resolve(jsonData.data ? jsonData.data as R : jsonData as R);
        } catch (err) {
          // JSON解析失败：直接resolve原始数据（兜底）
          resolve(responseData as R);
        }
      },
      fail: (err) => {
        showErrorToast && wx.showToast({
          title: "网络错误",
          icon: "none",
        });
        reject(err);
      },
    });
  });
};