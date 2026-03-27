# KLIPlayer

适用于终端的歌词（也许加上MV）播放器

**in developing...**

## 功能

加载指定的 `.klip` 脚本，然后在终端播放。

在脚本中可以指定音乐源和其他 `.kts` 脚本。

## 脚本语法

脚本按时间轴排列，不允许逆序。初始情况下操作主光标（ `cursor0` ），可以在脚本中定义更多光标实现多光标效果。

### 基础语法

```klip
// 用双斜线表示注释，这个符号后直到行的末尾将不会被读取

// 文件元信息，用于设定音乐源、脚本、最小表演大小
[meta music="relative/path/to/music.mp3"]
[meta script="relative/path/to/script.kts"]
[meta height=40]
[meta width=160]

// 除元信息和宏语句外，所有语句必须以[timeline]开头
// [timeline]语法如下
[00:00.000]绝对时间 //将在音频文件的00:00.000时刻使用cursor0输出"绝对时间"
[+123]相对时间 //将在音频文件的00:00.123时刻使用cursor0输出"相对时间"
[00:00.200][bpm 120] //将在音频文件的00:00.200时刻设定bpm为120，这一刻记为第0拍开始
[1b]绝对节拍时间 //将在音频文件的00:00.700时刻使用cursor0输出"绝对节拍时间" *注：所有用到了节拍时间的[timeline]必须在使用了[bpm]以后才能使用。绝对节拍时间可以使用小数
[+1b]相对节拍时间 //将在音频文件的00:01.200时刻使用cursor0输出"相对节拍时间" *注：可以使用小数
[+2b1]相对分数节拍时间 //将在音频文件的00:01.450时刻使用cursor0输出"相对分数节拍时间"。'b'前的数字是分母，'b'后的数字是分子。
[5b][bpm 60] //将在音频文件的00:02.700时刻将bpm设定为60，然后重置拍数计数，这一刻重新记为第0拍开始

// 光标控制语句
[newcursor <id>]//新建一个光标，将继承当前光标的所有属性
[cursor <id>]//切换到指定光标
[mv x,y]//光标移动到第x行，第y列 **注意**：竖x 横y，x从上到下增加，y从左到右增加。最小值均为1
[hide]//隐藏光标
[show]//显示光标
[color rrggbb]//设定光标输出颜色，十六进制整数，小写和大写都可以
[color default]//还原光标输出颜色
[background rrggbb]//设定光标输出背景颜色
[background default]//还原光标输出背景颜色
[style default]//清除格式信息
[style <stylename>=<on|off>]//设定格式信息，如 [style bold=on,italic=off]。stylename可用选项：bold,italic,strikeline,underline
[level <number>]//设定当前光标优先级，数字越小优先级越高，默认按照光标创建顺序
[protect <on|off>]//是否启用保护模式，如果启用，则优先级更低的光标将无法覆盖此光标的内容
[clean]//清屏，只能清理所有优先级小于等于自身的保护内容
[cleanline]//清除光标当前行
[newline]//换行，将会向终端键入回车键，并将所有光标的坐标向上移动一行。如果第1行有更高优先级的保护内容，则会拒绝执行
[delcursor <id>]//删除一个光标，不能删除自身。

// 内容控制语句
[space]//输出一个空格
[space n]//输出n个空格
[backspace]//实现退格效果
[utf-8 <code>]//输出单个utf-8字符
[img x1,y1,x2,y2,"path/to/img"]//在x1,y1到x2,y2中输出图像，使用imagemagick

// 转义符
[00:10.000]\[\]用于输出方括号
[00:10.000]\n\t\\... 其他转义符符合ANSI标准

// 宏语句
[@valname value]//创建一个名为valname，值为value的宏量。valname不允许使用以上预定义的名称，必须以字母开头，只能使用大小写字母、数字和下划线
[valname]//调用，将被解析为value
[@@varname <int>]//创建一个名为varname，值为<int>的宏变量，其数值可以发生变化
[varname]//调用
[=varname <express>]//修改varname的值，表达式允许基础四则运算、括号、乘方运算（<num1>^<num2>）、对数运算（<num>log<base>），允许使用其他宏变量（如 [=var1 [var2]+12-3^[var3]] ），最后计算结果四舍五入
[++varname]//自增
[--vatname]//自减

[#macroname @val1,@@var2,...]//创建一个宏，宏内部只允许使用相对时间，宏在被引用时将会继承引用者的指针。宏头部定义的宏量只允许在宏内部使用
[+123]...//宏内部语句
[+234][val1]...//调用内部宏量
[#endmacro]//宏结尾

[macroname value1,[var2]]//调用宏，将会被展开为：
// [+123]...
// [+234]value1...

[+macroname <cursorid>,value11,[var22]]//在一个协程中调用宏，必须传入一个非当前使用的光标
// 将会创建一个新的协程，并且展开为
// [+0][cursor <cursorid>]
// [+123]...
// [+234]value11...

[loop n]//循环n次，支持使用宏量和宏变量。循环体内只允许使用相对时间
[+111]do something
[+222]do something else
[endloop]//循环体结束标记

//案例：组合体和宏组合
[#rain @@time]
[loop [time]]
[+100][mv 1 20]|
[+100][mv 1 20][space][mv 2 20]|
[+100][mv 2 20][space][mv 3 20]|
[+100][mv 3 20][space][mv 4 20]|
[+100][mv 4 20][space][mv 5 20]|
[+100][mv 5 20][space][mv 6 20]|
[+100][mv 6 20][space][mv 7 20]|
[+100][mv 7 20][space][mv 8 20]|
[+100][mv 8 20][space][mv 9 20]|
[+100][mv 9 20][space]
[endloop]
[#endmacro]

//案例，在显示文字的同时调用宏
[00:01.123]do something...
[00:01.200][newcursor 2][protect on]
[00:01.500][+rain 2,10]
[00:01.600]do something
[00:02.200]do something//后面输出时，宏rain会一直执行直到其自动结束。这将被解析到两条时间线上因此不再造成冲突

```

## 开源协议

`MIT (C) 2026, Locked_Fog`

