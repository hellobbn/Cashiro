# Cashiro Debug：独立包名与固定签名

## 已配置的行为

| 项目 | Debug | Release |
|---|---|---|
| applicationId | `com.ritesh.cashiro.debug` | `com.ritesh.cashiro`（不变） |
| 桌面名称 | Cashiro Debug | 原来的应用名称 |
| 签名 | 专用、持久化 debug key | 原有 release 签名（不变） |
| 数据 | 独立的数据库、偏好与权限 | 原数据不受影响 |

桌面快捷方式指向 debug 包；FileProvider 和 Startup authority 随 applicationId 隔离；图标切换使用 namespace 中的类名，而不是拼接 applicationId。

## 1. 一次性创建并备份独立密钥

需要 Python 3 和 JDK 17（`keytool` 在 PATH 中）。在仓库根目录执行：

```sh
python3 scripts/signing/debug_identity.py init .debug-signing
```

本次准备工作已在本地仓库创建 `.debug-signing`，无需再次生成。命令重复运行只验证旧密钥，绝不重新生成覆盖。

- `.debug-signing/debug.keystore`：独立 debug 私钥。
- `.debug-signing/identity.json`：密码、alias、公开证书指纹。
- 目录权限 700，文件权限 600，已被 Git 忽略。
- 两个文件都应加密备份到密码管理器或其他可靠的私有存储。不要提交 Git、上传到 Actions artifacts，或放进公开网页。
- 不使用正式版密钥，也不要复制通用、公开的 Android 默认 debug 密钥。

## 2. 配置 GitHub 的五个 Repository Secrets

目标：`hellobbn/Cashiro` → Settings → Secrets and variables → Actions → Repository secrets。

| 名称 | 内容 |
|---|---|
| `DEBUG_KEYSTORE_BASE64` | `debug.keystore` 文件的 base64 内容 |
| `DEBUG_STORE_PASSWORD` | identity.json 中的同名字段 |
| `DEBUG_KEY_ALIAS` | identity.json 中的同名字段，初始为 cashiro-debug |
| `DEBUG_KEY_PASSWORD` | identity.json 中的同名字段 |
| `DEBUG_CERT_SHA256` | identity.json 中的同名字段，证书 SHA-256，不是 APK 文件 SHA-256 |

最省事的方式是登录 GitHub CLI 后，让脚本通过标准输入上传上述五项，避免密码进入命令参数、终端输出和 shell 历史：

```sh
# 如未安装 GitHub CLI，macOS/Homebrew 用户可先执行 brew install gh。
gh auth login --hostname github.com
python3 scripts/signing/debug_identity.py upload .debug-signing --repo hellobbn/Cashiro
```

SSH 密钥只处理 git 操作；写入 Actions Secrets 仍需具备该仓库 Secrets 管理权限的 GitHub CLI 登录。脚本仅操作 `DEBUG_*` 五项，不修改已有正式版 Secrets。上传脚本重跑会使用本地同一身份重新设置五项；先确认不要拿别的机器新生成的密钥替换正在使用的身份。

上传脚本只设置 Secrets，不会推送代码或触发构建。完成 Secrets 后再推送修改、运行 Debug APK。若 Secrets 缺失、keystore 不合法或证书指纹不符，workflow 会失败，不会发布新的随机签名 APK。旧 debug-latest 在签名校验通过之前不会被清理。

## 3. 本地构建与 CI 保持同一签名

```sh
python3 scripts/signing/debug_identity.py build .debug-signing :app:assembleStandardDebug --no-configuration-cache
```

上传构建采用相同身份及 `-PslimDebug -PrequireDebugSigning`，保留原有 arm64、压缩及代码缩减行为。

直接在 IDE 构建而不提供四个 `DEBUG_STORE_*` / `DEBUG_KEY_*` 环境变量时，仍使用该电脑自己的默认 debug 签名：它与 CI 固定签名不同。如需覆盖 CI 安装，请使用上述 helper，而不是默认 IDE 构建。

CI 会校验实际 APK 包名、名称、debuggable 标志和签名证书，并发布 `SIGNING-CERTIFICATE.json`。APK 本身的 SHA256SUMS 随构建变化是正常的。

## 4. 安装与迁移

- 新 debug 包与旧的 `com.ritesh.cashiro` 并存，不必卸载旧 App。
- 新 debug 首次启动没有旧数据：需要时从旧 App 手动导出备份，在新 debug 的启动页导入。
- 两个 App 之后不会自动同步。通知监听、SMS 和其他权限也分别配置，测试时注意不要同时启用重复采集。
- 后续同一签名、相容版本号的 debug 包可以覆盖升级；证书与包名一致不代表自动支持版本降级或数据库降级。
- 旧包签名问题不会被更换 applicationId 修复；这里是独立安装与数据迁移，不是签名绕过。

## 官方参考

- https://developer.android.com/build/build-variants
- https://developer.android.com/studio/publish/app-signing
- https://docs.github.com/en/actions/how-tos/write-workflows/choose-what-workflows-do/use-secrets

## 排错：上传时报 Java runtime not found

`upload` 会先调用 keytool 校验本机密钥，再写入 GitHub。如果终端没有加载 JDK，即使 GitHub 已登录也会在上传前失败。

在当前终端配置已有 JDK 的 JAVA_HOME，并将其 bin 目录加入 PATH，然后重复原来的 upload 命令。此工作区已有 `../toolchain/env.sh`，从仓库根目录执行 `source ../toolchain/env.sh` 即可加载。不要删除 `.debug-signing` 或重新生成密钥。更新后的脚本会标明失败的是 keytool 还是 gh，并对敏感值脱敏。
