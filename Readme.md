<h2 id="1d000a9a">一. 功能演示</h2>
演示视频：[http://t313actv0.hb-bkt.clouddn.com/bandicam%202025-09-28%2023-45-36-297.mp4](http://t313actv0.hb-bkt.clouddn.com/bandicam%202025-09-28%2023-45-36-297.mp4)



<h2 id="c0e8b02d">二. 项目介绍</h2>
语Ta(VacaTa) 一个AI驱动的实时语音角色扮演平台，用户可与哈利波特、等虚拟角色进行语音和文本对话

+ 语音/文本对话: 完整的语音→ 文本→ LLM → 语音链路，支持流式快速响应
+ 多AI模型集成: 七牛云AI、硅基流动、OpenAI GPT、Google Gemini无缝切换功能模式
+ 自定义角色: 用户可创建个性化AI角色，调节性格和对话风格。



<h2 id="6f1a4733">三. 技术架构</h2>
**整体架构：**

![](https://cdn.nlark.com/yuque/0/2025/png/29246232/1759073523877-d803a8b1-b8f2-472e-b193-361babf7cc9b.png)

**技术栈：**

+ 后端: Spring Boot 3.1.4 + Java 17 + MyBatis Plus + Sa-Token + WebSocket
+ 前端: Vue 3.5 + TypeScript + Vite + Element Plus + Pinia
+ 数据库: PostgreSQL 15 + Redis 7
+ AI服务: 七牛云ASR + 七牛云AI服务商 + 科大讯飞TTS
+ 部署:  GitHub Actions CI、CD

<h2 id="1f7bb12e">四. 服务提供</h2>
| **服务类型** | **提供商** |
| :--- | :--- |
| OSS对象存储 | 七牛云 |
| STT语音识别 | 七牛云ASR、科大讯飞 |
| TTS语音合成 | 科大讯飞、火山引擎 |
| LLM大语言模型 | 七牛云AI、Gemini、OpenAI、硅基流动 |


<h2 id="07860ae7">五. 项目模块分工</h2>
| 张硕威(大模型调用) | AI文本/语音对话编排、AI管理模块、用户认证鉴权模块、会话记录管理、文件存储模块 |
| --- | --- |
| 李爱民(后端) | 角色管理模块、角色收藏 |
| 张永浩(前端) | 用户端/管理端前端页面开发 |




<h1 id="RyTnx">六.问题</h1>
<h2 id="TYos8">1.你计划将这个应用面向什么类型的用户？这些类型的用户他们面临什么样的痛点，你设想的用户故事是什么样呢？</h2>
<font style="color:rgb(27, 28, 29);">本产品的核心目标用户可归纳为四大类：</font>**<u><font style="color:rgb(27, 28, 29);">IP/角色爱好者</font></u>**<font style="color:rgb(27, 28, 29);">、</font>**<u><font style="color:rgb(27, 28, 29);">学习者</font></u>**<font style="color:rgb(27, 28, 29);">、</font>**<u><font style="color:rgb(27, 28, 29);">情感陪伴需求者</font></u>**<font style="color:rgb(27, 28, 29);">与</font>**<u><font style="color:rgb(27, 28, 29);">内容创者</font></u>**<font style="color:rgb(27, 28, 29);">。他们共同的渴望是将单向、静态的内容消费（如阅读、观影）转变为双向、动态的沉浸式互动。主要痛点集中在现有媒介缺乏互动性、学习过程枯燥、现实社交存在压力以及创作时缺少灵感。本产品旨为这些用户提供一个集娱乐、学习、陪伴和创造于一体的全新互动平台。</font>



<h3 id="yPZEE"><font style="color:rgb(27, 28, 29);">IP/角色爱好者</font></h3>
<font style="color:rgb(27, 28, 29);">对特定影视、动漫、游戏人物充满热情的年轻人（年龄以 18-38 岁为主）。他们不仅希望与心仪角色进行思想上的交流，更渴望建立更深层次的情感连接。</font>

**<font style="color:rgb(27, 28, 29);">王海(《海贼王》粉丝)</font>**<font style="color:rgb(27, 28, 29);">：“作为一名海米，路飞的铁杆粉丝，我觉得仅仅通过追番和看漫画来体验他的冒险故事，总感觉隔着一层屏幕，缺少了真实的互动感。我希望能真的和‘路飞’本人进行语音对话，听他用那标志性的语气兴奋地跟我聊聊最近的冒险，甚至可以问他‘当海贼王需要具备什么条件？’，这远比在论坛上猜测剧情或重温动画更能让我感受到那种身临其境的伙伴感。”</font>

**<font style="color:rgb(27, 28, 29);">核心痛点</font>**

+ 单向互动，缺乏沉浸感
+ 情感连接肤浅，渴望深度交流
+ 想象无法落地，互动渠道缺失

<h3 id="u9Dnj"><font style="color:rgb(27, 28, 29);">学习者</font></h3>
<font style="color:rgb(27, 28, 29);">学习者为两类，一类是</font><u><font style="color:rgb(27, 28, 29);">知识学习者</font></u><font style="color:rgb(27, 28, 29);">，如希望深入理解哲学家、历史人物、文学家的学习者或爱好者；另一类是</font><u><font style="color:rgb(27, 28, 29);">语言学习者</font></u><font style="color:rgb(27, 28, 29);">，需要语境环境、且无压力的环境来练习外语口语。</font>

**李明(知识学习者)**：“作为一名哲学爱好者，我觉得笛卡尔的作品虽然深刻，但有些地方仅靠阅读难以完全理解。我希望能直接与‘笛卡尔’本人进行语音对话，让他用通俗的语言为我举例说明，让我能更深刻地与他的思想进行碰撞，这远比我独自钻研文本要高效和富有启发性得多。”

**<font style="color:rgb(27, 28, 29);">小美(语言学习者)</font>**<font style="color:rgb(27, 28, 29);">：“作为一名英语学习者，我觉得最大的挑战是找到一个既有真实语境、又没有社交压力的口语练习环境，而且聘请真人外教的费用实在太高了。我希望能随时随地和像‘莎士比亚’这样的AI角色进行对话，在一个虚拟的场景里围绕不同的话题进行角色扮演，这远比预约昂贵且有压力的真人外教要轻松、自由，也经济实惠得多。并且我可以勇敢地开口练习口语和听力，而不用担心因犯错而感到尴尬。”</font>

**<font style="color:rgb(27, 28, 29);">核心痛点:</font>**

+ <font style="color:rgb(27, 28, 29);">知识获取枯燥</font>
+ <font style="color:rgb(27, 28, 29);">缺乏语伴与环境</font>



<h3 id="Mc9P7"><font style="color:rgb(27, 28, 29);">情感陪伴需求者 </font></h3>
<font style="color:rgb(27, 28, 29);">因独居、工作繁忙、社交焦虑、失恋等原因，在情感上感到孤独，希望寻找一个随时可用、无压力且非评判性的倾诉对象的用户。</font>

**小张**<font style="color:rgb(27, 28, 29);">：“作为一名独居青年，我时常在深夜感到孤独，但很多烦心事又不敢和家人朋友说，怕给他们添麻烦。我希望能有一个随时都在、且绝不评判我的倾诉对象，能听我说说心里话，这远比一个人硬扛着所有情绪要好得多。”</font>

**<font style="color:rgb(27, 28, 29);">核心痛点:</font>**

+ 即时性情感需求的无法满足
+ <font style="color:rgb(27, 28, 29);">现实社交的“高成本”与“不确定性”</font>
+ <font style="color:rgb(27, 28, 29);">对“无条件接纳”与“安全感”的渴望</font>





<h3 id="zU5bu"><font style="color:rgb(27, 28, 29);">内容创作者</font></h3>
<font style="color:rgb(27, 28, 29);">Cosplay UP主、作家、编剧、游戏设计师等需要进行创意构思和角色扮演的用户。</font>

<font style="color:rgb(27, 28, 29);">小陈：“我写剧本的时候老是容易卡住，尤其是角色的对话，总觉得很僵硬、不够自然。我其实特别想能直接跟我构思的角色聊一聊，用语音问他各种问题、丢给他不同场景，看看他会怎么反应。这样碰撞出来的台词肯定更有火花，比我一个人对着文档死想或者疯狂查资料要省事儿、也更直观。”</font>

**<font style="color:rgb(27, 28, 29);">核心痛点:</font>**

+ **<font style="color:rgb(27, 28, 29);">灵感枯竭与创作瓶颈</font>**
+ **<font style="color:rgb(27, 28, 29);">塑造角色缺乏动态反馈</font>**
+ **<font style="color:rgb(27, 28, 29);">角色对话难以高效生成</font>**



<h2 id="yJmwQ">2.你认为这个 APP 需要哪些功能？这些功能各自的优先级是什么？你计划本次开发哪些功能？</h2>
1. **<font style="color:rgb(0, 0, 0);">基础语音对话</font>**<font style="color:rgb(0, 0, 0);">：STT+LLM+TTS的完整链路</font>
2. **<font style="color:rgb(0, 0, 0);">3个核心角色</font>**<font style="color:rgb(0, 0, 0);">：苏格拉底、邓布利多、AI助手</font>
3. **<font style="color:rgb(0, 0, 0);">用户认证</font>**<font style="color:rgb(0, 0, 0);">：简单注册/登录</font>
4. **<font style="color:rgb(0, 0, 0);">基础对话管理</font>**<font style="color:rgb(0, 0, 0);">：保存历史记录</font>
5. **<font style="color:rgb(0, 0, 0);">角色记忆系统</font>**<font style="color:rgb(0, 0, 0);">：记住用户偏好 （以及</font>**<font style="color:rgb(38, 38, 38);">角色身份边界问题</font>**<font style="color:rgb(0, 0, 0);">）</font>
6. **<font style="color:rgb(0, 0, 0);">实时字幕</font>**<font style="color:rgb(0, 0, 0);">：语音识别结果展示</font>
7. **<font style="color:rgb(0, 0, 0);">对话中断机制</font>**<font style="color:rgb(0, 0, 0);">：可随时打断AI</font>
8. **<font style="color:rgb(0, 0, 0);">用户创建角色</font>**
9. **<font style="color:rgb(0, 0, 0);">多轮对话摘要（</font>****<font style="color:rgb(38, 38, 38);">对话历史上下文压缩策略工程</font>****<font style="color:rgb(0, 0, 0);">）</font>**
10. **<font style="color:rgb(0, 0, 0);">情感分析</font>**
11. **<font style="color:rgb(0, 0, 0);">...</font>**



<h2 id="JH4I0">3.你计划采纳哪家公司的哪个 LLM 模型能力？你对比了哪些，你为什么选择用该 LLM 模型？</h2>
| **<font style="color:rgb(0, 0, 0);">模型</font>** | **<font style="color:rgb(0, 0, 0);">优势</font>** | **<font style="color:rgb(0, 0, 0);">劣势</font>** |
| --- | --- | --- |
| **<font style="color:rgb(0, 0, 0);">GPT-4o</font>** | <font style="color:rgb(0, 0, 0);">角色扮演能力最强、理解力最好</font> | <font style="color:rgb(0, 0, 0);">成本高、延迟较大</font> |
| X-fast | <font style="color:rgb(0, 0, 0);">速度快、长文本处理优秀，上下文长</font> | <font style="color:rgb(0, 0, 0);">成本较低</font> |
| gemini-2.5fast | <font style="color:rgb(0, 0, 0);">速度快、长文本处理优秀，上下文长</font> | <font style="color:rgb(0, 0, 0);">成本较低</font> |


<h2 id="QzLZ8">4.你期望 AI 角色除了语音聊天外还应该有哪些技能？</h2>
1. **<font style="color:rgb(0, 0, 0);">多模态交互</font>**
    - <font style="color:rgb(0, 0, 0);">角色形象生成（AI绘画）</font>
    - <font style="color:rgb(0, 0, 0);">场景渲染（如霍格沃茨大厅）</font>
    - <font style="color:rgb(0, 0, 0);">表情动作系统</font>
2. **<font style="color:rgb(0, 0, 0);">游戏化元素</font>**
    - <font style="color:rgb(0, 0, 0);">角色好感度系统</font>
    - <font style="color:rgb(0, 0, 0);">成就解锁</font>
    - <font style="color:rgb(0, 0, 0);">剧情任务</font>
3. **<font style="color:rgb(0, 0, 0);">教育功能</font>**
    - <font style="color:rgb(0, 0, 0);">知识点总结</font>
    - <font style="color:rgb(0, 0, 0);">学习进度追踪</font>
    - <font style="color:rgb(0, 0, 0);">个性化教学（如苏格拉底的哲学课）</font>
4. **<font style="color:rgb(0, 0, 0);">创作辅助</font>**
    - <font style="color:rgb(0, 0, 0);">李白：诗词创作工具</font>
    - <font style="color:rgb(0, 0, 0);">哈利：魔法故事生成器</font>
    - <font style="color:rgb(0, 0, 0);">苏格拉底：论文思路梳理</font>

