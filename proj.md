# KLIPlayer 项目组织建议

## 概述

KLIPlayer 是一个终端歌词播放器项目。根据 README 中的功能需求，本文档提供完整的 Kotlin 项目结构建议。

---

## 项目结构

```
src/main/kotlin/com/lockedfog/kliplayer/
├── Main.kt                              # 主入口
├── core/                                # 核心业务逻辑
│   ├── parser/
│   │   ├── KlipParser.kt               # 脚本解析器主类
│   │   ├── Lexer.kt                    # 词法分析器
│   │   ├── TokenType.kt                # 令牌类型定义
│   │   ├── Token.kt                    # 令牌类
│   │   └── ParsingException.kt         # 解析异常
│   │
│   └── timeline/
│       ├── TimelineManager.kt          # 时间轴管理器
│       ├── TimelineEvent.kt            # 时间轴事件
│       ├── TimelinePosition.kt         # 时间位置（绝对/相对/节拍）
│       ├── BeatCalculator.kt           # 节拍计算器
│       └── EventScheduler.kt           # 事件调度器
│
├── terminal/                            # 终端相关
│   ├── cursor/
│   │   ├── CursorManager.kt            # 光标管理器
│   │   ├── Cursor.kt                   # 光标类
│   │   ├── CursorProperty.kt           # 光标属性
│   │   └── CursorLayer.kt              # 光标图层（用于优先级管理）
│   │
│   ├── render/
│   │   ├── TerminalRenderer.kt         # 终端渲染器主类
│   │   ├── RenderBuffer.kt             # 渲染缓冲区
│   │   ├── ScreenContent.kt            # 屏幕内容
│   │   ├── AnsiCode.kt                 # ANSI转义码生成
│   │   └── ProtectedArea.kt            # 保护区域管理
│   │
│   └── style/
│       ├── StyleManager.kt             # 样式管理器
│       ├── TextStyle.kt                # 文本样式
│       ├── Color.kt                    # 颜色类
│       └── StyleType.kt                # 样式类型（加粗、斜体等）
│
├── media/                               # 多媒体处理
│   ├── audio/
│   │   ├── MusicPlayer.kt              # 音乐播放器
│   │   ├── AudioFormat.kt              # 音频格式
│   │   └── PlaybackControl.kt          # 播放控制
│   │
│   └── image/
│       ├── ImageProcessor.kt           # 图像处理器
│       ├── ImageMagickAdapter.kt       # ImageMagick适配器
│       └── ImageRenderOption.kt        # 图像渲染选项
│
├── model/                               # 数据模型
│   ├── Statement.kt                    # 语句基类
│   ├── ContentStatement.kt             # 内容语句
│   ├── ControlStatement.kt             # 控制语句
│   ├── MetaInfo.kt                     # 元信息
│   ├── ScriptConfig.kt                 # 脚本配置
│   └── ExecutionContext.kt             # 执行上下文
│
├── utils/                               # 工具函数
│   ├── TimeFormat.kt                   # 时间格式处理
│   ├── StringUtils.kt                  # 字符串工具
│   └── Terminal.kt                      # 终端工具（清屏、光标控制等）
│
├── config/
│   ├── ProjectProperties.kt            # 项目配置
│   └── Constants.kt                    # 常量定义
│
└── exception/
    ├── KlipException.kt                # 基础异常
    ├── ParseException.kt               # 解析异常
    ├── ExecutionException.kt           # 执行异常
    └── RenderException.kt              # 渲染异常
```

---

## 模块架构详解

### 1. Parser Module - 脚本解析

**职责**：将 `.klip` 脚本文件解析成语句对象序列

**关键类**：
- `Lexer`：将脚本转换为令牌流，处理：
  - 时间表示法（绝对时间、相对时间、节拍时间）
  - 方括号语句 `[...]`
  - 注释和转义符
  - 宏定义

- `KlipParser`：将令牌流转换为语句对象，处理：
  - 元信息语句
  - 时间轴语句
  - 控制语句
  - 宏定义和宏调用

**输出**：`List<Statement>`

---

### 2. Timeline Module - 时间轴管理

**职责**：管理时间位置转换、事件调度

**关键类**：
- `TimelinePosition`：表示时间位置，支持：
  - 绝对时间格式：`mm:ss.sss`
  - 相对时间格式：`+123` (毫秒)
  - 节拍时间格式：`1b`, `+0.5b`
  - 分数节拍格式：`2b1` (分母2、分子1)

- `BeatCalculator`：节拍计算，需要：
  - 当前 BPM（节拍/分钟）
  - 基准时间点（设置 BPM 的时刻）

- `TimelineManager`：事件排序与调度
  - 按时间顺序管理事件
  - 检测时间逆序
  - 提供时间查询接口

---

### 3. Macro Module - 宏系统

**职责**：处理宏量、宏定义、宏展开

**关键类**：
- `MacroConstant`：不可变宏量
  - 格式：`[@valname value]`
  - 只读，不能修改

- `MacroDefinition`：宏定义模板
  - 格式：`[#macroname @val1,@@var2,...]`
  - 内部参数作用域局限于宏内部
  - 支持相对时间

- `MacroExpander`：宏展开引擎
  - 参数替换
  - 嵌套宏展开
  - 协程支持（用于并行执行宏）

---

### 4. Expression Module - 表达式求值

**职责**：计算宏表达式

**支持的运算**：
- 四则运算：`+`, `-`, `*`, `/`
- 括号：`()`
- 乘方：`^`（例：`2^3`）
- 对数：`log`（例：`8log2`，结果为3）
- 宏变量引用：`[varname]`
- 结果自动四舍五入

**示例**：`[=var1 [var2]+12-3^[var3]]`

---

### 5. Cursor Module - 光标管理

**职责**：管理多光标、优先级、保护模式

**关键特性**：
- 多光标支持：`cursor0`（初始）、`cursor1`、`cursor2` ...
- 坐标系统：`(x, y)` 表示第 x 行第 y 列（1-indexed）
- 光标属性：
  - 前景色/背景色（RGB 十六进制）
  - 文本样式（加粗、斜体、下划线、删除线）
  - 优先级（level）
  - 保护模式（protect）
  - 显示状态（show/hide）

- `CursorLayer`：优先级管理
  - 数字越小优先级越高
  - 低优先级光标无法覆盖高优先级内容（在保护模式下）

---

### 6. Terminal Renderer - 终端渲染

**职责**：将光标状态转换为 ANSI 转义码输出

**关键类**：
- `RenderBuffer`：维护屏幕缓冲区
  - 二维字符数组
  - 记录每个位置的样式信息

- `ScreenContent`：屏幕内容管理
  - 处理光标输出的内容
  - 处理全角字符（中文、Emoji 等）的自动间隔

- `AnsiCode`：ANSI 转义码生成
  - 光标移动：`\u001B[{x};{y}H`
  - 颜色设置：`\u001B[38;2;r;g;bm`（RGB 前景色）
  - 样式设置：`\u001B[1m`（加粗）等

- `ProtectedArea`：保护区域管理
  - 追踪高优先级内容位置
  - 防止低优先级覆盖

---

### 7. Style Module - 样式管理

**职责**：管理文本样式

**样式类型**：
- `bold`（加粗）
- `italic`（斜体）
- `underline`（下划线）
- `strikeline`（删除线）

**颜色表示**：
- RGB 十六进制：`ff0000`（红色）
- 特殊值：`default`（恢复默认）

---

### 8. Music Player - 音乐播放

**职责**：加载播放音乐，同步时间轴

**关键功能**：
- 加载音乐文件（MP3、WAV 等）
- 获取当前播放位置（毫秒）
- 播放控制（播放、暂停、停止、跳转）
- 与时间轴事件同步

**建议库**：
- `javazoom`（JLayer）用于 MP3 播放
- 或 `javax.sound.sampled`（仅支持 WAV）

---

### 9. Image Processor - 图像处理

**职责**：调用 ImageMagick 渲染图像到指定区域

**关键功能**：
- 调用系统 `convert` 或 `magick` 命令
- 缩放图像到指定区域
- 转换为字符艺术或色彩输出（根据终端能力）

---

### 10. Model - 数据模型

**关键类**：
- `Statement`：各类语句的基类
- `ContentStatement`：输出语句（文本、空格等）
- `ControlStatement`：控制语句（光标移动、清屏等）
- `MetaInfo`：元信息（音乐源、脚本引用、窗口尺寸）
- `ScriptConfig`：脚本配置
- `ExecutionContext`：执行上下文（当前光标、BPM、宏表等）

---

### 11. Utils - 工具函数

**关键模块**：
- `TimeFormat`：时间格式转换
  - `mm:ss.sss` ↔ 毫秒
  
- `MathUtils`：数学运算
  - 乘方、对数、四舍五入
  
- `StringUtils`：字符串处理
  - 转义符处理（`\n`, `\t`, `\\` 等）
  - UTF-8 字符处理
  
- `Terminal`：终端工具
  - 清屏命令
  - 光标隐藏/显示
  - 终端尺寸检测

---

## 开发阶段建议

### 第一阶段：基础（1-2 周）
1. **模块建立**
   - 创建所有目录结构
   - 定义异常类
   - 定义常量

2. **数据模型**
   - `model/` 下的所有类
   - 支持序列化/反序列化

3. **工具函数**
   - `utils/TimeFormat` - 时间转换
   - `utils/MathUtils` - 表达式数学运算
   - `utils/StringUtils` - 转义符处理

4. **单元测试**
   - 为工具函数编写测试

### 第二阶段：核心解析（2-3 周）
1. **Parser 实现**
   - `Lexer` - 词法分析
   - `KlipParser` - 语法分析
   - 处理所有时间格式

2. **Timeline 管理**
   - `TimelinePosition` 表示
   - `BeatCalculator` 节拍计算
   - `TimelineManager` 事件调度

3. **测试**
   - 编写 `.klip` 脚本样例
   - 验证解析结果

### 第三阶段：终端渲染（2-3 周）
1. **光标系统**
   - `Cursor` 类及属性
   - `CursorManager` 管理多光标
   - 优先级和保护模式

2. **渲染引擎**
   - `RenderBuffer` 缓冲区
   - `AnsiCode` 转义码生成
   - `TerminalRenderer` 整合

3. **样式支持**
   - 颜色、加粗等样式

4. **测试**
   - 在真实终端测试输出

### 第四阶段：宏系统（2 周）
1. **宏数据结构**
   - `MacroConstant`、`MacroVariable`
   - `MacroDefinition`

2. **表达式求值**
   - `ExpressionEvaluator` 实现

3. **宏展开**
   - `MacroExpander` 展开引擎
   - 支持嵌套和协程

4. **集成测试**
   - 测试复杂宏脚本

### 第五阶段：多媒体（2 周）
1. **音乐播放**
   - `MusicPlayer` 实现
   - 时间同步

2. **图像处理**
   - `ImageProcessor` 集成 ImageMagick

3. **完整集成**
   - 将所有模块整合到 `Main.kt`

### 第六阶段：打磨和优化（1 周）
1. 性能优化
2. 错误处理完善
3. 文档编写
4. 发布准备

---

## 技术栈

### 构建工具
- Gradle（已配置）
- Kotlin 标准库

### 依赖建议

```kotlin
dependencies {
    // Kotlin 标准库
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    
    // 音乐播放（选择其一）
    implementation("org.jaudiotagger:jaudiotagger:2.2.3") // 音频标签库
    // 或
    // 使用 javax.sound.sampled（仅 WAV）或 javazoom（MP3）
    
    // ImageMagick 包装库
    implementation("org.im4java:im4java:0.7.0")
    
    // 日志
    implementation("org.slf4j:slf4j-api:2.0.5")
    implementation("ch.qos.logback:logback-classic:1.4.5")
    
    // 测试
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}
```

### 系统要求
- Java 11+
- Kotlin 1.8+
- ImageMagick（用于图像处理）
- 支持 ANSI 转义码的终端

---

## 关键设计决策

### 1. 时间轴模型
- 采用事件驱动模式
- 支持多种时间表示（绝对、相对、节拍）
- 节拍计算基于 BPM 和基准时间点

### 2. 光标优先级
- 数字越小优先级越高
- 保护模式防止低优先级覆盖
- 仅支持清除优先级相同或更低的内容

### 3. 宏系统
- 区分常量和变量
- 表达式求值支持递归引用
- 宏展开在解析阶段进行

### 4. 终端渲染
- 使用二维缓冲区跟踪屏幕状态
- ANSI 转义码用于样式和位置
- 全角字符需要特殊处理（自动间隔）

---

## 扩展考虑

### 未来增强
1. **多线程支持** - 协程中的宏并行执行
2. **录制/回放** - 保存脚本执行状态
3. **交互模式** - 用户输入交互
4. **插件系统** - 自定义语句和处理器
5. **Web 界面** - 通过 SSE 流式输出到浏览器

---

## 文件编码规范

- 文件编码：UTF-8
- 包结构：`com.lockedfog.kliplayer.*`
- 命名规范：
  - 类名：PascalCase
  - 函数名：camelCase
  - 常量名：UPPER_SNAKE_CASE
  - 文件名：与类名相同

---

## 参考文档

- [ANSI 转义码参考](https://en.wikipedia.org/wiki/ANSI_escape_code)
- [Kotlin 官方文档](https://kotlinlang.org/docs/)
- [Gradle Kotlin DSL](https://docs.gradle.org/current/userguide/kotlin_dsl.html)
