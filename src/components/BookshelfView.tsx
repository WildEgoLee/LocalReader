import React, { useState, useRef } from 'react';
import { Book, BookFormat, WindowWidthSizeClass } from '../types';
import {
  Search,
  Grid2X2,
  List as ListIcon,
  Plus,
  BookOpen,
  Clock,
  X,
  FileText,
  FileCode2,
  UploadCloud,
  Sparkles,
  Layers,
  HardDrive,
  Cpu,
} from 'lucide-react';
import { decodeTextBuffer, parseTxtContent } from '../utils/txtParser';

interface BookshelfViewProps {
  books: Book[];
  widthSizeClass: WindowWidthSizeClass;
  onSelectBook: (book: Book) => void;
  onAddBook: (book: Book) => void;
}

export const BookshelfView: React.FC<BookshelfViewProps> = ({
  books,
  widthSizeClass,
  onSelectBook,
  onAddBook,
}) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');
  const [showAddModal, setShowAddModal] = useState(false);
  const [isDragging, setIsDragging] = useState(false);
  const [isProcessingFile, setIsProcessingFile] = useState(false);

  // Manual add state
  const [newTitle, setNewTitle] = useState('');
  const [newAuthor, setNewAuthor] = useState('');
  const [newFormat, setNewFormat] = useState<BookFormat>('TXT');

  const fileInputRef = useRef<HTMLInputElement>(null);

  const filteredBooks = books.filter((b) => {
    if (!searchQuery.trim()) return true;
    const query = searchQuery.toLowerCase();
    return (
      b.title.toLowerCase().includes(query) ||
      b.author.toLowerCase().includes(query)
    );
  });

  const recentBooks = books.filter((b) => b.lastReadAt !== undefined);

  const handleProcessFile = async (file: File) => {
    setIsProcessingFile(true);
    try {
      const buffer = await file.arrayBuffer();
      const { text, encoding } = await decodeTextBuffer(buffer);
      const parsed = parseTxtContent(text, file.name, file.size, encoding);

      const newBook: Book = {
        id: Date.now(),
        title: parsed.title,
        author: parsed.author,
        format: file.name.toLowerCase().endsWith('.epub') ? 'EPUB' : 'TXT',
        uri: `content://leafreader/storage/${file.name}`,
        fileSize: file.size,
        encoding: parsed.encoding,
        addedAt: Date.now(),
        readingProgress: 0,
        currentLocator: {
          type: 'TXT',
          chapterId: parsed.chapters[0]?.id || 'ch_1',
          charOffset: 0,
          paragraphIndex: 0,
          relativeProgress: 0,
        },
        lastChapterTitle: parsed.chapters[0]?.title || '第一章',
        chapters: parsed.chapters,
      };

      onAddBook(newBook);
      setShowAddModal(false);
    } catch (err) {
      alert('解析文件失败：' + (err instanceof Error ? err.message : String(err)));
    } finally {
      setIsProcessingFile(false);
    }
  };

  const handleFileInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      handleProcessFile(file);
    }
    e.target.value = '';
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    const file = e.dataTransfer.files?.[0];
    if (file) {
      handleProcessFile(file);
    }
  };

  const handleAddSampleNovel = () => {
    const sampleNovelRaw = `《大唐剑仙录》[全本校对精排]
作者：青莲居士

第一章 青莲剑歌动九霄
长安城三月的细雨带着丝丝凉意，朱雀大街两侧的杨柳抽出了嫩绿的细芽。
一名着白衣、腰悬墨青酒葫芦的剑客迎着濛濛细雨缓步前行。
他腰间的长剑没有剑鞘，只用粗布包裹，却隐隐透出一股教人骨头发寒的清冽锋芒。
“十年磨一剑，霜刃未曾试。”
他仰头痛饮了一口烈酒，双眸如寒星般掠过高耸的皇城城楼。
大唐天宝年间，魔教乱世，藩镇割据，江湖风云由此激荡！

第二章 醉宿黄鹤楼
江水汤汤，奔流东去。
黄鹤楼三层雅座，凭栏处凭吊古今。
酒过三巡，李白手提墨笔，在粉白石壁上挥毫写下两行大字：
“黄鹤楼中吹玉笛，江城五月落梅花。”
笔锋如走龙蛇，剑意更在墨迹中吞吐隐现。
忽然楼外传来一声尖啸，三枚淬毒透骨钉破空而至！
“何方宵小，敢扰本座雅兴？”

第三章 蜀道难于上青天
蚕丛及鱼凫，开国何茫然！
剑阁峥嵘而崔嵬，一夫当关，万夫莫开。
追杀李白的十二黑衣煞星紧追不舍，却不知早已步入青莲剑阵之中。
狂风卷起千层飞沙，漫天剑气化作银色狂澜！
“噫吁嚱，危乎高哉！”

尾声 天下谁人不识君
十年之后，天下重归太平。
孤舟渐远，长歌未歇，唯有一壶美酒与绝世青锋长伴于天地之间。`;

    const parsed = parseTxtContent(sampleNovelRaw, '大唐剑仙录[全本校对精排].txt', 1024 * 45, 'UTF-8');
    const sampleBook: Book = {
      id: Date.now(),
      title: parsed.title,
      author: '青莲居士',
      format: 'TXT',
      uri: 'content://leafreader/sample/datang.txt',
      fileSize: 46080,
      encoding: 'UTF-8',
      addedAt: Date.now(),
      readingProgress: 0,
      currentLocator: {
        type: 'TXT',
        chapterId: 'ch_1',
        charOffset: 0,
        paragraphIndex: 0,
        relativeProgress: 0,
      },
      lastChapterTitle: '第一章 青莲剑歌动九霄',
      chapters: parsed.chapters,
    };
    onAddBook(sampleBook);
    setShowAddModal(false);
  };

  const handleManualAddSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newTitle.trim()) return;

    const manualBook: Book = {
      id: Date.now(),
      title: newTitle.trim(),
      author: newAuthor.trim() || '佚名',
      format: newFormat,
      uri: `content://leafreader/custom/${Date.now()}.${newFormat.toLowerCase()}`,
      fileSize: 1250000,
      encoding: 'UTF-8',
      addedAt: Date.now(),
      readingProgress: 0,
      currentLocator: {
        type: newFormat === 'TXT' ? 'TXT' : 'EPUB',
        chapterId: 'ch_1',
        charOffset: 0,
        paragraphIndex: 0,
        relativeProgress: 0,
        href: 'ch1.xhtml',
      } as any,
      lastChapterTitle: '第一章 启程',
      chapters: [
        {
          id: 'ch_1',
          title: '第一章 启程',
          orderIndex: 1,
          characterCount: 1800,
          content: `${newTitle}\n作者：${newAuthor || '佚名'}\n\n晨曦划破薄雾，旅人整装待发。漫长而波澜壮阔的旅途，由此徐徐拉开帷幕……`,
          locator: {
            type: 'TXT',
            chapterId: 'ch_1',
            charOffset: 0,
            paragraphIndex: 0,
            relativeProgress: 0,
          },
        },
      ],
    };

    onAddBook(manualBook);
    setNewTitle('');
    setNewAuthor('');
    setShowAddModal(false);
  };

  const gridColsClass =
    widthSizeClass === 'COMPACT'
      ? 'grid-cols-2'
      : widthSizeClass === 'MEDIUM'
      ? 'grid-cols-3'
      : 'grid-cols-5';

  return (
    <div id="bookshelf-container" className="flex flex-col h-full overflow-y-auto bg-stone-50 text-stone-900">
      {/* Top Header */}
      <header id="bookshelf-header" className="sticky top-0 z-10 bg-stone-50/90 backdrop-blur-md px-4 py-3.5 border-b border-stone-200/80 flex items-center justify-between">
        <div className="flex items-center space-x-2">
          <BookOpen className="w-5 h-5 text-emerald-800" />
          <h1 className="text-lg font-bold tracking-tight text-stone-900 font-serif">
            我的书架
          </h1>
          <span className="text-xs px-2 py-0.5 rounded-full bg-emerald-100/70 text-emerald-800 font-medium">
            SAF + Room 本地持久化
          </span>
        </div>

        <div className="flex items-center space-x-2">
          <button
            id="toggle-view-mode-btn"
            type="button"
            onClick={() => setViewMode(viewMode === 'grid' ? 'list' : 'grid')}
            className="p-1.5 rounded-lg text-stone-600 hover:bg-stone-200/60 transition-colors"
            title={viewMode === 'grid' ? '切换为列表视图' : '切换为网格视图'}
          >
            {viewMode === 'grid' ? <ListIcon className="w-4 h-4" /> : <Grid2X2 className="w-4 h-4" />}
          </button>
          <button
            id="open-add-book-modal-btn"
            type="button"
            onClick={() => setShowAddModal(true)}
            className="flex items-center space-x-1 px-3 py-1.5 text-xs font-semibold rounded-lg bg-emerald-800 text-white hover:bg-emerald-900 shadow-xs transition-all"
          >
            <Plus className="w-3.5 h-3.5" />
            <span>导入 / 添加</span>
          </button>
        </div>
      </header>

      {/* Main Content Area */}
      <div className="p-4 sm:p-6 max-w-6xl mx-auto w-full space-y-6">
        {/* Search Bar */}
        <div className="relative">
          <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-stone-400" />
          <input
            id="bookshelf-search-input"
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="搜索本地小说、作者、格式..."
            className="w-full pl-9 pr-8 py-2 text-sm bg-stone-100 border border-stone-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-emerald-700/20 focus:border-emerald-700 transition"
          />
          {searchQuery && (
            <button
              onClick={() => setSearchQuery('')}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-stone-400 hover:text-stone-600"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          )}
        </div>

        {/* Recent Reading Carousel */}
        {!searchQuery && recentBooks.length > 0 && (
          <section id="recent-reading-section" className="space-y-3">
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-1.5 text-xs font-semibold text-stone-600 uppercase tracking-wider">
                <Clock className="w-3.5 h-3.5 text-emerald-700" />
                <span>最近阅读</span>
              </div>
              <span className="text-[11px] text-stone-400 font-mono">
                {recentBooks.length} 本在读
              </span>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
              {recentBooks.slice(0, 3).map((book) => (
                <div
                  key={`recent-${book.id}`}
                  id={`recent-card-${book.id}`}
                  onClick={() => onSelectBook(book)}
                  className="group relative p-3.5 rounded-xl bg-white border border-stone-200/90 shadow-xs hover:shadow-md hover:border-emerald-700/40 transition-all cursor-pointer flex items-center space-x-3.5"
                >
                  <BookCoverPlaceholder title={book.title} format={book.format} size="sm" />

                  <div className="flex-1 min-w-0 space-y-1">
                    <h3 className="text-sm font-semibold text-stone-900 truncate group-hover:text-emerald-800 transition-colors">
                      {book.title}
                    </h3>
                    <p className="text-xs text-stone-500 truncate">
                      {book.lastChapterTitle || '第一章'}
                    </p>
                    <div className="flex items-center space-x-2 pt-0.5">
                      <div className="flex-1 h-1.5 bg-stone-100 rounded-full overflow-hidden">
                        <div
                          className="h-full bg-emerald-700 rounded-full transition-all duration-300"
                          style={{ width: `${Math.round(book.readingProgress * 100)}%` }}
                        />
                      </div>
                      <span className="text-[10px] font-mono font-medium text-stone-500">
                        {Math.round(book.readingProgress * 100)}%
                      </span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </section>
        )}

        {/* All Books Header */}
        <div className="flex items-center justify-between pt-2">
          <div className="flex items-center space-x-2">
            <h2 className="text-sm font-bold text-stone-900 font-serif">
              所有藏书
            </h2>
            <span className="text-xs px-2 py-0.5 rounded bg-stone-200 text-stone-700 font-mono">
              {filteredBooks.length}
            </span>
          </div>
        </div>

        {/* Book List / Grid View */}
        {filteredBooks.length === 0 ? (
          <div className="py-16 text-center space-y-3">
            <BookOpen className="w-10 h-10 text-stone-300 mx-auto stroke-1" />
            <p className="text-sm text-stone-500">
              {searchQuery ? '没有找到匹配的书籍' : '书架空空如也，立即导入一本吧'}
            </p>
            <button
              type="button"
              onClick={() => setShowAddModal(true)}
              className="text-xs font-medium text-emerald-800 hover:underline inline-flex items-center gap-1"
            >
              <Plus className="w-3.5 h-3.5" /> 导入本地小说
            </button>
          </div>
        ) : viewMode === 'grid' ? (
          <div className={`grid ${gridColsClass} gap-4 sm:gap-6`}>
            {filteredBooks.map((book) => (
              <div
                key={book.id}
                id={`book-grid-card-${book.id}`}
                onClick={() => onSelectBook(book)}
                className="group flex flex-col cursor-pointer transition-transform hover:-translate-y-1 duration-200"
              >
                <div className="relative mb-2.5">
                  <BookCoverPlaceholder title={book.title} format={book.format} size="lg" />
                  <div className="absolute top-2 right-2 flex gap-1">
                    {book.encoding && (
                      <span className="px-1.5 py-0.5 rounded bg-black/60 backdrop-blur-xs text-[9px] font-mono text-emerald-300 font-medium">
                        {book.encoding}
                      </span>
                    )}
                  </div>
                  {book.readingProgress > 0 && (
                    <div className="absolute bottom-2 left-2 right-2 px-2 py-1 rounded bg-black/60 backdrop-blur-xs flex items-center justify-between text-[10px] text-white/90 font-mono">
                      <span>进度</span>
                      <span>{Math.round(book.readingProgress * 100)}%</span>
                    </div>
                  )}
                </div>

                <h3 className="text-xs sm:text-sm font-semibold text-stone-900 truncate group-hover:text-emerald-800 transition-colors">
                  {book.title}
                </h3>
                <div className="flex items-center justify-between text-[11px] text-stone-400 mt-0.5">
                  <span className="truncate max-w-[65%]">{book.author}</span>
                  <span className="font-mono">{book.chapters.length}章</span>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="bg-white rounded-xl border border-stone-200 divide-y divide-stone-100 overflow-hidden shadow-xs">
            {filteredBooks.map((book) => (
              <div
                key={book.id}
                id={`book-list-row-${book.id}`}
                onClick={() => onSelectBook(book)}
                className="p-3 sm:p-4 hover:bg-stone-50/80 transition-colors cursor-pointer flex items-center justify-between"
              >
                <div className="flex items-center space-x-3 min-w-0">
                  <BookCoverPlaceholder title={book.title} format={book.format} size="sm" />
                  <div className="min-w-0 space-y-0.5">
                    <div className="flex items-center gap-2">
                      <h3 className="text-sm font-semibold text-stone-900 truncate">
                        {book.title}
                      </h3>
                      {book.encoding && (
                        <span className="text-[10px] font-mono px-1.5 py-0.2 rounded bg-stone-100 text-stone-600">
                          {book.encoding}
                        </span>
                      )}
                    </div>
                    <p className="text-xs text-stone-500">
                      {book.author} · {book.chapters.length} 章节 · {(book.fileSize / 1024 / 1024).toFixed(1)} MB
                    </p>
                  </div>
                </div>

                <div className="flex items-center space-x-4 flex-shrink-0 text-right">
                  <div className="hidden sm:block">
                    <span className="text-xs font-mono text-stone-500 block">
                      {book.readingProgress > 0 ? `${Math.round(book.readingProgress * 100)}%` : '未读'}
                    </span>
                    <span className="text-[10px] text-stone-400">
                      {book.lastChapterTitle ? book.lastChapterTitle.slice(0, 10) : '未开始'}
                    </span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* SAF File Import & Add Modal */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 bg-stone-950/40 backdrop-blur-xs flex items-center justify-center p-4">
          <div
            id="add-book-dialog"
            className="bg-white rounded-2xl max-w-md w-full p-5 shadow-2xl border border-stone-200 animate-in fade-in zoom-in-95 duration-200 space-y-4"
          >
            <div className="flex items-center justify-between border-b border-stone-100 pb-3">
              <div className="flex items-center space-x-2">
                <HardDrive className="w-4 h-4 text-emerald-800" />
                <h3 className="text-sm font-bold text-stone-900 font-serif">
                  导入本地图书 (SAF & TXT 引擎)
                </h3>
              </div>
              <button
                type="button"
                onClick={() => setShowAddModal(false)}
                className="p-1 rounded-lg text-stone-400 hover:text-stone-700 hover:bg-stone-100 transition"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Hidden real file input */}
            <input
              ref={fileInputRef}
              type="file"
              accept=".txt,.epub,text/plain"
              onChange={handleFileInputChange}
              className="hidden"
            />

            {/* Drag and drop zone */}
            <div
              onDragOver={(e) => {
                e.preventDefault();
                setIsDragging(true);
              }}
              onDragLeave={() => setIsDragging(false)}
              onDrop={handleDrop}
              onClick={() => fileInputRef.current?.click()}
              className={`border-2 border-dashed rounded-xl p-5 text-center cursor-pointer transition-all ${
                isDragging
                  ? 'border-emerald-600 bg-emerald-50/50'
                  : 'border-stone-200 hover:border-emerald-700/50 hover:bg-stone-50'
              }`}
            >
              <UploadCloud className="w-8 h-8 text-emerald-700 mx-auto mb-2" />
              <p className="text-xs font-semibold text-stone-800">
                {isProcessingFile ? '正在流式解析章节与检测编码...' : '点击选择本地 TXT / EPUB 文件'}
              </p>
              <p className="text-[11px] text-stone-400 mt-1">
                支持拖拽上传 · 自动检测 UTF-8 / GBK 编码 · 多正则分章
              </p>
            </div>

            {/* Quick Demo Import Button */}
            <div className="p-3 bg-stone-50 rounded-xl border border-stone-200/80 flex items-center justify-between">
              <div>
                <span className="text-xs font-semibold text-stone-800 block">
                  快速体验：大唐剑仙录
                </span>
                <span className="text-[10px] text-stone-500">
                  真实多章节 TXT 文本，测试分章正则与目录
                </span>
              </div>
              <button
                type="button"
                onClick={handleAddSampleNovel}
                className="px-2.5 py-1.5 rounded-lg bg-emerald-800 text-white text-xs font-medium hover:bg-emerald-900 transition flex items-center gap-1 shadow-xs"
              >
                <Sparkles className="w-3.5 h-3.5" />
                <span>一键导入样例</span>
              </button>
            </div>

            {/* Manual Form Accordion */}
            <form onSubmit={handleManualAddSubmit} className="space-y-3 pt-2 border-t border-stone-100">
              <span className="text-xs font-medium text-stone-500 block">
                或手动登记书名：
              </span>
              <div>
                <input
                  id="new-book-title-input"
                  type="text"
                  value={newTitle}
                  onChange={(e) => setNewTitle(e.target.value)}
                  placeholder="书籍名称 (例如：雪中悍刀行)"
                  className="w-full px-3 py-1.5 text-xs bg-stone-50 border border-stone-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-emerald-700/20 focus:border-emerald-700"
                />
              </div>

              <div>
                <input
                  id="new-book-author-input"
                  type="text"
                  value={newAuthor}
                  onChange={(e) => setNewAuthor(e.target.value)}
                  placeholder="作者 (选填)"
                  className="w-full px-3 py-1.5 text-xs bg-stone-50 border border-stone-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-emerald-700/20 focus:border-emerald-700"
                />
              </div>

              <div className="flex justify-end gap-2 pt-1">
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="px-3 py-1.5 text-xs text-stone-600 hover:bg-stone-100 rounded-lg transition"
                >
                  取消
                </button>
                <button
                  id="confirm-add-book-btn"
                  type="submit"
                  disabled={!newTitle.trim()}
                  className="px-4 py-1.5 text-xs font-medium text-white bg-emerald-800 hover:bg-emerald-900 disabled:opacity-50 rounded-lg shadow-xs transition"
                >
                  手动添加
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export const BookCoverPlaceholder: React.FC<{
  title: string;
  format: BookFormat;
  size: 'sm' | 'lg';
}> = ({ title, format, size }) => {
  const isTxt = format === 'TXT';
  const bgClass = isTxt
    ? 'from-slate-700 to-slate-800 text-amber-100'
    : 'from-stone-800 to-zinc-900 text-emerald-200';

  if (size === 'sm') {
    return (
      <div
        className={`w-11 h-14 rounded-md bg-gradient-to-br ${bgClass} p-1.5 flex flex-col justify-between shadow-xs flex-shrink-0 border border-black/10`}
      >
        <span className="text-[9px] font-mono tracking-widest opacity-60">
          {format}
        </span>
        <span className="text-[10px] font-serif font-bold leading-tight line-clamp-2">
          {title.slice(0, 4)}
        </span>
      </div>
    );
  }

  return (
    <div
      className={`w-full aspect-[3/4] rounded-lg bg-gradient-to-br ${bgClass} p-3 flex flex-col justify-between shadow-sm border border-black/10 relative overflow-hidden`}
    >
      <div className="flex justify-between items-center text-[10px] font-mono opacity-70">
        <span>LEAF</span>
        <span className="px-1 py-0.2 rounded bg-white/15 text-white/90">
          {format}
        </span>
      </div>
      <div className="my-auto py-2 text-center">
        <span className="text-sm sm:text-base font-serif font-bold tracking-wide leading-tight line-clamp-3">
          {title}
        </span>
      </div>
      <div className="w-full h-0.5 bg-white/20 rounded-full" />
    </div>
  );
};
