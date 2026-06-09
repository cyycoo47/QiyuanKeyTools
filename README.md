# 启源按键调试工具 (QiyuanKeyTools)

> 基于启源 A06 OpenSDK 的物理按键扩展附件调试与自定义映射工具  
> 适用车型：长安启源 A06（Qiyuan A06）

---

## 功能概览

| Tab | 功能 |
|-----|------|
| **检测** | 实时捕获 ROTY_BOX / MFS / HW_KEY 三路属性事件，显示来源、属性ID、码值、动作类型；支持暂停/清空/导出 TXT |
| **映射** | 对已检测码值配置功能动作（发送广播 / 启动应用 / 模拟系统按键 / 执行 Shell）；支持单条编辑/删除/全部重置 |
| **设置** | 显示 SDK 连接状态；开关后台监听服务；独立控制三路属性监听；导入/导出映射配置 JSON；查看版本及属性 ID 参考 |

---

## 项目结构

```
QiyuanKeyTools/
├── app/
│   ├── libs/                          ← ★ 放置 opensdk-client_V1.0.0.0.aar
│   ├── proguard-rules.pro
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/com/qiyuan/keytools/
│           ├── App.kt                 ← Application（初始化SDK）
│           ├── model/
│           │   ├── KeyModels.kt       ← KeyEventRecord, KeySource, KeyAction, KeyPropIds, KeyId
│           │   └── KeyMapping.kt      ← KeyMapping, ActionType, SystemKeyOption
│           ├── data/
│           │   └── KeyPreferences.kt  ← SharedPreferences 持久化
│           ├── sdk/
│           │   └── KeySdkManager.kt   ← 封装三个属性监听，对外暴露 Flow
│           ├── service/
│           │   ├── KeyListenerService.kt ← 前台Service，执行映射动作
│           │   └── BootReceiver.kt    ← 开机自启
│           ├── viewmodel/
│           │   ├── DetectViewModel.kt
│           │   ├── MappingViewModel.kt
│           │   └── SettingsViewModel.kt
│           └── ui/
│               ├── MainActivity.kt
│               ├── detect/
│               │   ├── DetectFragment.kt
│               │   └── KeyEventAdapter.kt
│               ├── mapping/
│               │   ├── MappingFragment.kt
│               │   ├── MappingAdapter.kt
│               │   └── MappingEditDialog.kt
│               └── settings/
│                   └── SettingsFragment.kt
├── gradle/libs.versions.toml
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## 构建前准备（必读）

### 1. 放置 AAR 文件

将启源 OpenSDK 客户端 AAR 复制到以下路径：

```
app/libs/opensdk-client_V1.0.0.0.aar
```

> **获取方式**：从长安汽车车机开发平台下载 OpenSDK 开发包，解压后取得 AAR 文件。

### 2. 环境要求

| 工具 | 要求 |
|------|------|
| Android Studio | Hedgehog 或更高 |
| JDK | 17 |
| Gradle | 8.x（Wrapper 自动下载） |
| compileSdk | 33 |
| minSdk | 28（Android 9.0） |

### 3. 车机签名配置（可选）

如需在车机系统中运行并获取 `android.car.*` 权限，APK 需要使用**平台签名**或**车机厂商签名**。

在 `app/build.gradle.kts` 中添加签名配置：

```kotlin
signingConfigs {
    create("platform") {
        storeFile = file("path/to/platform.keystore")
        storePassword = "your_store_password"
        keyAlias = "platform"
        keyPassword = "your_key_password"
    }
}
buildTypes {
    release {
        signingConfig = signingConfigs.getByName("platform")
    }
}
```

---

## 构建 & 安装

```bash
# Debug APK（用于调试）
./gradlew assembleDebug

# Release APK（需要签名配置）
./gradlew assembleRelease

# 直接安装到已连接的车机
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 监听的属性 ID

| 属性名称 | 属性 ID（十六进制） | 属性 ID（十进制） | 说明 |
|----------|-------------------|-----------------|------|
| `ID_OTHER_ROTY_BOX_SWT_STATUS` | `0x4131EC00` | `1093894144` | 旋转盒子物理按键 |
| `ID_OTHER_MFS_SWT_STATUS` | `0x4131EE00` | `1093894656` | MFS 多功能方向盘按键 |
| `HW_KEY_INPUT` | `0x11410A10` | `289473040` | 硬件按键输入（VHAL HW_KEY_INPUT）|

---

## 支持的映射动作

| 动作类型 | 说明 | 参数示例 |
|----------|------|----------|
| 无操作 | 不执行任何操作 | — |
| 发送广播 | `sendBroadcast(Intent(action))` | `com.example.MY_ACTION` |
| 启动应用 | 按包名启动已安装应用 | `com.gaode.map` |
| 模拟系统按键 | 通过 AudioManager 发送媒体按键事件 | 音量+、播放/暂停、下一曲… |
| 执行 Shell | `Runtime.exec(command)` | `am start -n com.xxx/.Activity` |

> **注意**：Shell 命令执行需要应用具备系统权限，普通 APK 无效。

---

## 动作解析规则

```
HW_KEY_INPUT 码值布局：
  bit[23:16] = action（0=DOWN, 1=UP, 2=LONG_PRESS）
  bit[15:0]  = keyCode（Android KeyEvent keycode）

ROTY_BOX / MFS 码值：
  目前统一标记为 ACTION_DOWN，需实车测试后在
  KeySdkManager.parseAction() 中修正位域定义
```

---

## 配置导入/导出

映射配置以 JSON 格式存储，可在**设置页面**导出/导入，便于备份和迁移：

```json
[
  {
    "source": "ROTY_BOX",
    "rawValue": 1,
    "actionType": "LAUNCH_APP",
    "launchPackage": "com.gaode.map",
    "launchAppName": "高德地图"
  },
  {
    "source": "MFS",
    "rawValue": 2,
    "actionType": "KEYEVENT",
    "keyEventLabel": "播放/暂停"
  }
]
```

---

## 常见问题

**Q：安装后看不到任何按键事件**  
A：确认 AAR 已正确放置，并且 APK 已使用平台签名或车机厂商授权。  
   可在设置页查看"SDK 状态"是否显示"已连接"。

**Q：发送广播后目标应用没有响应**  
A：确认目标应用声明了对应的 `<receiver>` 且 `exported="true"`，或改用显式 Intent。

**Q：后台服务在车机系统 kill 后不重启**  
A：如车机系统限制了前台服务自重启，可尝试申请系统级持久化权限，或让用户手动在设置中将应用加入白名单。

**Q：码值动作类型显示为 DOWN，但实际是长按**  
A：需在实车上观察 ROTY_BOX / MFS 属性原始值的变化规律，对应修改 `KeySdkManager.parseAction()` 中的位域解析逻辑。

---

## 版本历史

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0.0 | 2026-05-17 | 初始版本：检测/映射/设置三Tab，支持四种映射动作 |

---

## 许可

本工具仅供车机开发调试使用，不得用于生产车辆或未授权设备。  
启源 OpenSDK 版权归长安汽车所有。
