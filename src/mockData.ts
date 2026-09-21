import { Book, ReaderSettings } from './types';

export const initialReaderSettings: ReaderSettings = {
  themePalette: 'PAPER',
  fontSizeSp: 18,
  lineSpacingMultiplier: 1.7,
  paragraphSpacingDp: 14,
  horizontalMarginDp: 28,
  fontFamily: 'serif',
  pageMode: 'PAGED',
  columnMode: 'AUTO',
  volumeKeyNavigation: false,
  keepScreenOn: false,
};

export const initialMockBooks: Book[] = [
  {
    id: 1,
    title: '诡秘之主',
    author: '爱潜水的乌贼',
    format: 'TXT',
    uri: 'content://library/1.txt',
    fileSize: 14250000,
    addedAt: Date.now() - 86400000 * 2,
    lastReadAt: Date.now() - 3600000 * 2,
    readingProgress: 0.67,
    lastChapterTitle: '第 736 章 门后的叹息',
    currentLocator: {
      type: 'TXT',
      chapterId: 'c1',
      charOffset: 1240,
      paragraphIndex: 4,
      relativeProgress: 0.67,
    },
    chapters: [
      {
        id: 'c1',
        title: '第一章 绯红之月',
        orderIndex: 1,
        locator: { type: 'TXT', chapterId: 'c1', charOffset: 0, paragraphIndex: 0, relativeProgress: 0.05 },
        characterCount: 3200,
        content: `痛！
好痛！
头好痛！

绚烂疯狂的呓语与虚幻的重叠光影在脑海深处翻涌，周明瑞感觉自己的脑袋就像被一柄生锈的钝刀狠狠劈开，又塞进了一团滚烫的铁水。

他下意识想要抬手按住额头，却发现四肢沉重如铅，冰冷的麻木感顺着脊椎一路攀爬。

空气中弥漫着一股刺鼻的气味，既像煤气泄漏，又带着淡淡的水银和劣质酒精的辛辣。

“我不是正准备睡觉吗？怎么会……”

周明瑞艰难地睁开双眼，视野由模糊逐渐变得清晰。首先映入眼帘的不是熟悉的现代天花板，而是一片泛着水渍的粗糙灰白石灰顶，以及一盏摇曳欲灭的黄铜油灯。

窗外夜色深沉，一轮巨大而妖异的绯红月亮悬挂在天际，静静注视着整座寂静的维多利亚式街道。

桌面上凌乱地摊开着几本厚重泛黄的古籍，羊皮纸边缘卷曲，中间还散落着半截熄灭的黑细蜡烛、一个装有透明液体的玻璃小瓶，以及……一把泛着冷硬金属光泽的左轮手枪。

记忆如同决堤的洪水，汹涌而至。

克莱恩·莫雷蒂。
鲁恩王国，阿霍瓦郡，廷根市。
霍伊大学历史系应届毕业生。

周明瑞怔怔地看着自己的双手，修长、苍白，指腹带着长年握笔留下的老茧。

镜子。
右侧的穿衣镜内，倒映出一张年轻而略显消瘦的黑发面孔，深棕色的眼眸深处隐藏着尚未散去的震惊与茫然。最触目惊心的是，在他的太阳穴位置，赫然有着一个焦黑凹陷的孔洞！

然而此刻，伤口边缘正有细微的肉芽在不可思议地交织生长。

“我……穿越了？”`,
      },
      {
        id: 'c2',
        title: '第二章 窥秘之人',
        orderIndex: 2,
        locator: { type: 'TXT', chapterId: 'c2', charOffset: 0, paragraphIndex: 0, relativeProgress: 0.18 },
        characterCount: 3600,
        content: `廷根市的清晨笼罩在薄薄的煤烟雾霭中。

克莱恩站在洗手池前，用刺骨的冷水狠狠泼在脸上，水珠顺着下颌滴落在泛黄的木地板上。

太阳穴处的伤口已经完全结痂脱落，只留下一道极淡的粉色印记，若非特意拨开黑发仔细端详，根本看不出曾有一颗黄铜子弹贯穿了这里。

“魔术？神迹？还是……神秘学仪式？”

桌上的自制占卜硬币静静躺在斑驳的羊皮纸正中央。克莱恩深吸一口气，伸出右手食指与拇指捏住那枚面值为半便士的钱币。

闭目，凝神。
默念七遍语句。
“廷根市治安署值夜者正在接近。”

随着铮的一声脆响，钱币被指甲高高弹起，在昏暗的煤油灯光下划出一道微弱的金线，随即啪嗒落在掌心。

正面向上——
象征肯定！`,
      },
      {
        id: 'c3',
        title: '第三章 灰雾之上的宫殿',
        orderIndex: 3,
        locator: { type: 'TXT', chapterId: 'c3', charOffset: 0, paragraphIndex: 0, relativeProgress: 0.35 },
        characterCount: 4200,
        content: `克莱恩顺着无形的引力缓缓上升。

四周所有的光影与声音逐渐退去，取而代之的是无边无际、近乎凝固的灰色浓雾。

灰雾仿佛永恒不变的海洋，沉淀着岁月与文明的残屑。在浓雾的正中心，矗立着一座宏伟磅礴得令人心神震颤的深黑神殿。

那不是凡人所能构筑的建筑，每一根石柱都需要数十人合抱，柱身上雕刻着无数早已失落的古老星轨与奇异符文。

克莱恩踏上斑驳的青石台阶，推开沉重的大门。

神殿正厅中摆放着一张极长极宽的古老青铜长桌，左右两侧各自排列着十余张高背椅。在长桌最上首的首席位置，椅背上刻画着奇异的“愚者”星座图腾。

他不由自主地走上前，拉开那张沉重的座椅缓缓坐下。

随着他落座的瞬间，整座灰雾神殿轰然震颤，无数深红色的星光自虚空中亮起，宛如静待神明唤醒的亿万生灵。`,
      },
    ],
  },
  {
    id: 2,
    title: '三体全集',
    author: '刘慈欣',
    format: 'EPUB',
    uri: 'content://library/2.epub',
    fileSize: 4500000,
    addedAt: Date.now() - 86400000 * 5,
    lastReadAt: Date.now() - 86400000,
    readingProgress: 0.42,
    lastChapterTitle: '第二部 黑暗森林 · 猜疑链',
    currentLocator: {
      type: 'EPUB',
      href: 'part1_chapter1.xhtml',
      relativeProgress: 0.42,
    },
    chapters: [
      {
        id: 'e1',
        title: '第一部 乱纪元 · 科学边界',
        orderIndex: 1,
        locator: { type: 'EPUB', href: 'part1_chapter1.xhtml', relativeProgress: 0.1 },
        characterCount: 5200,
        content: `在文化大革命的狂潮中，红岸基地在偏远险峻的大兴安岭雷达峰秘密落成。

巨大的抛物面天线仿佛一只冷漠的巨眼，恒久注视着浩瀚无垠的星空。冰冷的电波穿透对流层，向着深邃的银河系边缘激射而去。

叶文洁静静站在天线基座下的寒风中，双手插在厚重的军大衣口袋里。

她仰望着头顶那片在零下三十度严寒中闪烁得格外清晰的繁星。那些光芒跨越了数十甚至数百个光年才抵达她的眼眸，古老、静谧，却又隐隐透着令人窒息的残酷法则。

“宇宙这么大，真的只有我们吗？”

身后的控制室内，监听仪表盘的指示灯规律闪烁，绿色的荧光在波形显示器上划出单调的水平线条。那是一片永恒的死寂。

直到那一天，红色警戒灯无声地亮起。

红岸基地的超级计算机正在以超乎寻常的算力，解译着一段来自太阳方向的三体恒星系统信号。

那是三条反复出现的警告：
“不要回答！不要回答！！不要回答！！！”`,
      },
      {
        id: 'e2',
        title: '第二部 黑暗森林 · 面壁计划',
        orderIndex: 2,
        locator: { type: 'EPUB', href: 'part2_chapter1.xhtml', relativeProgress: 0.45 },
        characterCount: 6800,
        content: `“宇宙就是一座黑暗森林，每个文明都是带枪的猎人，像幽灵般潜行于林间，轻轻拨开挡路的树枝，竭力不让脚步发出一点儿声音，连呼吸都必须小心翼翼……”

罗辑站在冰冻的湖面上，脚下是厚达两米的坚冰，深处隐隐透着幽蓝的光芒。

寒风呼啸着掠过旷野，吹起他单薄的风衣下摆。

“他必须十分小心，因为林中到处都潜行着和他一样隐秘的猎人。如果他发现了别的生命，能做的只有一件事：开枪消灭之。在这片森林中，他人就是地狱，就是永恒的威胁，任何暴露自己存在的生命都将很快被消灭。”

罗辑抬起头，仰望那片没有任何回音的夜空。

“这就是宇宙文明图景，这就是对费米悖论的解释。”`,
      },
    ],
  },
  {
    id: 3,
    title: '道诡异仙',
    author: '狐尾的笔',
    format: 'TXT',
    uri: 'content://library/3.txt',
    fileSize: 9800000,
    addedAt: Date.now() - 86400000 * 8,
    lastReadAt: Date.now() - 86400000 * 2,
    readingProgress: 0.28,
    lastChapterTitle: '第 120 章 迷惘之境',
    currentLocator: {
      type: 'TXT',
      chapterId: 'd1',
      charOffset: 890,
      paragraphIndex: 3,
      relativeProgress: 0.28,
    },
    chapters: [
      {
        id: 'd1',
        title: '第一章 清旺来与白灵淼',
        orderIndex: 1,
        locator: { type: 'TXT', chapterId: 'd1', charOffset: 0, paragraphIndex: 0, relativeProgress: 0.1 },
        characterCount: 3400,
        content: `“李火旺，该吃药了。”

病房白色的天花板晃得人眼花，消毒水的气味钻入鼻孔，刺得脑仁隐隐发疼。

李火旺费力地睁开眼睛，看到母亲正端着一杯温水和几粒红白相间的胶囊站在床边，眼角带着浓浓的疲惫与心疼。

“妈，我刚才又做梦了……”李火旺喘着粗气，“我梦见我被绑在一间阴暗潮湿的炼丹房里，一个长着三张脸的老道士正拿着尖刀要剜我的心，说我是天生的药引子……”

“傻孩子，那都是幻觉，医生说了，只要按时吃药，精神分裂症是能控制的。”

李火旺接过水杯，仰头将药片咽下。

然而就在咽下药片的瞬间，视线突然疯狂扭曲！

雪白的病床变成了挂满人油灯盏的阴暗地窟，母亲慈祥的面庞骤然拉长，化作一张狰狞怪笑的青铜傩面！

“徒儿啊……你终于醒了，师尊今日要炼九转长生丹，借你的心肝肺脾肾一用！”`,
      },
    ],
  },
  {
    id: 4,
    title: '雪中悍刀行',
    author: '烽火戏诸侯',
    format: 'TXT',
    uri: 'content://library/4.txt',
    fileSize: 16800000,
    addedAt: Date.now() - 86400000 * 14,
    lastReadAt: undefined,
    readingProgress: 0.05,
    lastChapterTitle: '第 12 章 凉刀出鞘',
    currentLocator: {
      type: 'TXT',
      chapterId: 'x1',
      charOffset: 0,
      paragraphIndex: 0,
      relativeProgress: 0.05,
    },
    chapters: [
      {
        id: 'x1',
        title: '第一章 小二，上酒！',
        orderIndex: 1,
        locator: { type: 'TXT', chapterId: 'x1', charOffset: 0, paragraphIndex: 0, relativeProgress: 0.05 },
        characterCount: 3900,
        content: `北凉边境，黄沙漫道。

一匹骨瘦嶙峋的老马慢悠悠地踢踏着蹄子，马背上伏着一个衣衫褴褛的年轻人，蓬头垢面，嘴唇干裂，瞧模样活脱脱像是个逃难的叫花子。

在老马身旁，跟着一个背着破布行囊的缺门牙老仆，手里拎着一根老旧的竹马鞭，一步三晃，边走边抱怨世道艰辛。

路旁有一间用茅草和粗木简陋搭起来的茶肆野店，酒幌子在西北烈风中呼啦啦作响。

年轻人晃晃悠悠翻身下马，一脚踏进茶肆门槛，咧嘴一笑露出白花花的牙齿，声音沙哑却带着一股骨子里的慵懒不羁：

“小二，先切二斤熟牛肉，再来一坛最烈的烧刀子！”

茶肆掌柜瞥了他一眼，没好气道：“客官，店小利薄，概不赊账！”

年轻人拍了拍空空如也的腰囊，转头看向身后的缺门牙老仆，老仆无奈地翻了个白眼，默默从靴子里摸出两枚油腻腻的铜钱。

北凉世子徐凤年，游历江湖三千里，今日终于归家。`,
      },
    ],
  },
];
