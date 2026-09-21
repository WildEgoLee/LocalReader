import React, { useState, useEffect } from 'react';
import {
  Book,
  Chapter,
  ReaderOverlay,
  ReaderSettings,
  SearchResult,
  WindowWidthSizeClass,
} from '../types';
import { SupportingPaneToc } from './SupportingPaneToc';
import { ReaderSettingsSheet } from './ReaderSettingsSheet';
import {
  ArrowLeft,
  List,
  Sliders,
  ChevronLeft,
  ChevronRight,
  BookMarked,
  Columns2,
  Search,
  X,
  FileCode,
} from 'lucide-react';

interface ReaderViewProps {
  book: Book;
  settings: ReaderSettings;
  widthSizeClass: WindowWidthSizeClass;
  isLandscape: boolean;
  onBackToBookshelf: () => void;
  onUpdateBookProgress: (bookId: number, progress: number, locatorInfo: string, chapterTitle: string) => void;
  onUpdateSettings: (updated: Partial<ReaderSettings>) => void;
}

export const ReaderView: React.FC<ReaderViewProps> = ({
  book,
  settings,
  widthSizeClass,
  isLandscape,
  onBackToBookshelf,
  onUpdateBookProgress,
  onUpdateSettings,
}) => {
  const [currentChapterIndex, setCurrentChapterIndex] = useState(0);
  const [overlay, setOverlay] = useState<ReaderOverlay>('NONE');
  const [progress, setProgress] = useState(book.readingProgress || 0.05);

  // Search state
  const [searchKeyword, setSearchKeyword] = useState('');
  const [searchResults, setSearchResults] = useState<SearchResult[]>([]);

  const currentChapter = book.chapters[currentChapterIndex] || book.chapters[0] || {
    id: 'ch_1',
    title: '第一章',
    content: '暂无内容',
  };

  // Theme style mapping
  const themeStyles = {
    PAPER: { bg: '#F6F1E7', text: '#332F29', subtext: '#7A7365', border: '#E4DDD0' },
    LIGHT: { bg: '#FAFAFA', text: '#202124', subtext: '#5F6368', border: '#E8EAED' },
    DARK: { bg: '#202124', text: '#E6E1E5', subtext: '#9AA0A6', border: '#303134' },
    AMOLED: { bg: '#000000', text: '#DCDCDC', subtext: '#757575', border: '#1E1E1E' },
  }[settings.themePalette];

  // Two-column layout trigger: Expanded window + landscape, or forced DUAL
  const isTwoColumn =
    settings.columnMode === 'DUAL' ||
    (settings.columnMode === 'AUTO' && widthSizeClass === 'EXPANDED' && isLandscape);

  const handlePrevious = () => {
    if (currentChapterIndex > 0) {
      const prevIdx = currentChapterIndex - 1;
      setCurrentChapterIndex(prevIdx);
      const newProgress = Math.max(0, prevIdx / Math.max(1, book.chapters.length - 1));
      setProgress(newProgress);
      onUpdateBookProgress(book.id, newProgress, `chapter_${prevIdx}`, book.chapters[prevIdx].title);
    } else {
      setProgress((p) => Math.max(0, p - 0.05));
    }
  };

  const handleNext = () => {
    if (currentChapterIndex < book.chapters.length - 1) {
      const nextIdx = currentChapterIndex + 1;
      setCurrentChapterIndex(nextIdx);
      const newProgress = Math.min(1, nextIdx / Math.max(1, book.chapters.length - 1));
      setProgress(newProgress);
      onUpdateBookProgress(book.id, newProgress, `chapter_${nextIdx}`, book.chapters[nextIdx].title);
    } else {
      setProgress((p) => Math.min(1, p + 0.05));
    }
  };

  const handleSelectChapter = (chapter: Chapter) => {
    const idx = book.chapters.findIndex((c) => c.id === chapter.id);
    if (idx !== -1) {
      setCurrentChapterIndex(idx);
      const newProgress = Math.min(1, idx / Math.max(1, book.chapters.length - 1));
      setProgress(newProgress);
      onUpdateBookProgress(book.id, newProgress, chapter.id, chapter.title);
      setOverlay('NONE');
    }
  };

  // Run in-book search across chapters
  const handlePerformSearch = (kw: string) => {
    setSearchKeyword(kw);
    if (!kw.trim()) {
      setSearchResults([]);
      return;
    }
    const results: SearchResult[] = [];
    const query = kw.toLowerCase();

    for (let i = 0; i < book.chapters.length; i++) {
      const ch = book.chapters[i];
      const text = ch.content || '';
      let pos = 0;
      while (pos < text.length) {
        const found = text.toLowerCase().indexOf(query, pos);
        if (found === -1) break;

        const start = Math.max(0, found - 25);
        const end = Math.min(text.length, found + kw.length + 35);
        const snippet = (start > 0 ? '...' : '') + text.substring(start, end).replace(/\n/g, ' ') + (end < text.length ? '...' : '');

        results.push({
          chapterId: ch.id,
          chapterTitle: ch.title,
          charOffset: found,
          snippet,
          relativeProgress: (i + (found / Math.max(1, text.length))) / Math.max(1, book.chapters.length),
        });

        if (results.length >= 30) break;
        pos = found + kw.length;
      }
      if (results.length >= 30) break;
    }

    setSearchResults(results);
  };

  const handleSelectSearchResult = (result: SearchResult) => {
    const idx = book.chapters.findIndex((c) => c.id === result.chapterId);
    if (idx !== -1) {
      setCurrentChapterIndex(idx);
      setProgress(result.relativeProgress);
      onUpdateBookProgress(book.id, result.relativeProgress, result.chapterId, result.chapterTitle);
      setOverlay('NONE');
    }
  };

  // 3-Zone tap handler
  const handleSurfaceClick = (e: React.MouseEvent<HTMLDivElement>) => {
    if (
      (e.target as HTMLElement).closest(
        '#reader-top-bar, #reader-bottom-bar, #reader-settings-dialog, #toc-supporting-pane, #toc-bottom-sheet, #search-panel'
      )
    ) {
      return;
    }

    const rect = e.currentTarget.getBoundingClientRect();
    const clickX = e.clientX - rect.left;
    const width = rect.width;

    if (clickX < width * 0.25) {
      handlePrevious();
    } else if (clickX > width * 0.75) {
      handleNext();
    } else {
      setOverlay(overlay === 'NONE' ? 'CONTROLS' : 'NONE');
    }
  };

  // Keyboard navigation
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'ArrowLeft') handlePrevious();
      if (e.key === 'ArrowRight' || e.key === ' ') handleNext();
      if (e.key === 'Escape') setOverlay('NONE');
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [currentChapterIndex, book]);

  // Split text for two-column book layout
  const paragraphs = (currentChapter.content || '').split('\n').filter((p) => p.trim());
  const half = Math.ceil(paragraphs.length / 2);
  const leftParagraphs = paragraphs.slice(0, half);
  const rightParagraphs = paragraphs.slice(half);

  return (
    <div
      id="reader-screen"
      style={{ backgroundColor: themeStyles.bg, color: themeStyles.text }}
      className="relative flex h-full w-full overflow-hidden select-none"
    >
      {/* Supporting Pane TOC on Tablet (Expanded) */}
      {overlay === 'TOC' && widthSizeClass !== 'COMPACT' && (
        <SupportingPaneToc
          chapters={book.chapters}
          currentChapterId={currentChapter.id}
          widthSizeClass={widthSizeClass}
          onSelectChapter={handleSelectChapter}
          onClose={() => setOverlay('NONE')}
        />
      )}

      {/* Main Reading Surface */}
      <div
        id="reading-surface"
        onClick={handleSurfaceClick}
        className="flex-1 flex flex-col h-full overflow-hidden relative cursor-default"
      >
        {/* Subtle Reading Header */}
        <header
          className="flex-shrink-0 px-6 sm:px-10 pt-4 pb-2 flex justify-between items-center text-xs tracking-wide"
          style={{ color: themeStyles.subtext }}
        >
          <span className="truncate max-w-[65%] font-medium">
            {currentChapter.title}
          </span>
          <div className="flex items-center space-x-2">
            {book.encoding && (
              <span className="font-mono text-[10px] opacity-70">
                [{book.encoding}]
              </span>
            )}
            <span className="font-mono text-[11px]">
              {Math.round(progress * 100)}%
            </span>
          </div>
        </header>

        {/* Text Content Area */}
        <main
          className={`flex-1 overflow-y-auto px-6 sm:px-12 py-4 ${
            settings.pageMode === 'PAGED' ? 'overflow-y-hidden' : ''
          }`}
          style={{
            paddingLeft: `${Math.max(20, settings.horizontalMarginDp)}px`,
            paddingRight: `${Math.max(20, settings.horizontalMarginDp)}px`,
          }}
        >
          {isTwoColumn ? (
            <div className="grid grid-cols-2 gap-10 h-full max-w-5xl mx-auto items-start">
              {/* Left Column */}
              <div
                className="space-y-4 font-serif text-justify"
                style={{
                  fontSize: `${settings.fontSizeSp}px`,
                  lineHeight: settings.lineSpacingMultiplier,
                }}
              >
                {leftParagraphs.map((p, i) => (
                  <p key={i} className="indent-8 tracking-normal">
                    {p}
                  </p>
                ))}
              </div>

              {/* Center Divider mimicking book spine fold */}
              <div
                className="space-y-4 font-serif text-justify border-l pl-10 h-full"
                style={{
                  borderColor: themeStyles.border,
                  fontSize: `${settings.fontSizeSp}px`,
                  lineHeight: settings.lineSpacingMultiplier,
                }}
              >
                {rightParagraphs.map((p, i) => (
                  <p key={i} className="indent-8 tracking-normal">
                    {p}
                  </p>
                ))}
              </div>
            </div>
          ) : (
            <div
              className="max-w-2xl mx-auto space-y-4 font-serif text-justify"
              style={{
                fontSize: `${settings.fontSizeSp}px`,
                lineHeight: settings.lineSpacingMultiplier,
              }}
            >
              {paragraphs.map((p, i) => (
                <p key={i} className="indent-8 tracking-normal">
                  {p}
                </p>
              ))}
            </div>
          )}
        </main>

        {/* Subtle Reading Footer (Logical Locator) */}
        <footer
          className="flex-shrink-0 px-6 sm:px-10 py-2.5 flex justify-between items-center text-[11px] font-mono border-t"
          style={{ color: themeStyles.subtext, borderColor: `${themeStyles.border}80` }}
        >
          <div className="flex items-center space-x-2 truncate">
            <span className="font-semibold text-emerald-800 dark:text-emerald-400">
              LeafReader
            </span>
            <span>·</span>
            <span className="truncate">
              {book.format} 逻辑定位 [ID: {currentChapter.id}, 进度: {Math.round(progress * 1000) / 10}%]
            </span>
          </div>
          <div className="flex items-center space-x-2 flex-shrink-0">
            {isTwoColumn && (
              <span className="flex items-center gap-1 text-[10px] px-1.5 py-0.5 rounded bg-black/5 dark:bg-white/10">
                <Columns2 className="w-3 h-3" /> 双栏书页
              </span>
            )}
            <span className="hidden sm:inline text-stone-400">轻触两侧翻页 · 中央呼出控制</span>
          </div>
        </footer>
      </div>

      {/* Top Controls Overlay */}
      {overlay === 'CONTROLS' && (
        <div
          id="reader-top-bar"
          className="absolute top-0 inset-x-0 z-30 bg-stone-900/95 text-white backdrop-blur-md px-4 py-3 border-b border-stone-800 flex items-center justify-between animate-in slide-in-from-top duration-200"
        >
          <div className="flex items-center space-x-3 min-w-0">
            <button
              id="reader-back-btn"
              type="button"
              onClick={onBackToBookshelf}
              className="p-1.5 rounded-lg text-stone-300 hover:text-white hover:bg-stone-800 transition"
              title="返回书架"
            >
              <ArrowLeft className="w-5 h-5" />
            </button>
            <div className="min-w-0">
              <h2 className="text-sm font-semibold truncate max-w-xs">{book.title}</h2>
              <p className="text-[10px] text-stone-400 truncate">{currentChapter.title}</p>
            </div>
          </div>

          <div className="flex items-center space-x-2 flex-shrink-0">
            <button
              id="reader-search-btn"
              type="button"
              onClick={() => setOverlay('SEARCH')}
              className="flex items-center space-x-1 px-2.5 py-1.5 rounded-lg bg-stone-800 hover:bg-stone-700 text-xs font-medium transition"
            >
              <Search className="w-3.5 h-3.5" />
              <span className="hidden sm:inline">搜索</span>
            </button>
            <button
              id="reader-toc-btn"
              type="button"
              onClick={() => setOverlay('TOC')}
              className="flex items-center space-x-1 px-2.5 py-1.5 rounded-lg bg-stone-800 hover:bg-stone-700 text-xs font-medium transition"
            >
              <List className="w-3.5 h-3.5" />
              <span>目录</span>
            </button>
            <button
              id="reader-settings-btn"
              type="button"
              onClick={() => setOverlay('SETTINGS')}
              className="flex items-center space-x-1 px-2.5 py-1.5 rounded-lg bg-stone-800 hover:bg-stone-700 text-xs font-medium transition"
            >
              <Sliders className="w-3.5 h-3.5" />
              <span>排版</span>
            </button>
          </div>
        </div>
      )}

      {/* Bottom Controls Overlay */}
      {overlay === 'CONTROLS' && (
        <div
          id="reader-bottom-bar"
          className="absolute bottom-0 inset-x-0 z-30 bg-stone-900/95 text-white backdrop-blur-md p-4 border-t border-stone-800 space-y-3 animate-in slide-in-from-bottom duration-200"
        >
          <div className="flex justify-between items-center text-xs text-stone-300">
            <span className="truncate max-w-[70%]">{currentChapter.title}</span>
            <span className="font-mono text-emerald-400 font-semibold">
              {Math.round(progress * 100)}%
            </span>
          </div>

          <div className="flex items-center space-x-3">
            <button
              id="reader-prev-chapter-btn"
              type="button"
              onClick={handlePrevious}
              className="p-1.5 rounded-lg text-stone-300 hover:text-white hover:bg-stone-800 transition"
              title="上一页/上一章"
            >
              <ChevronLeft className="w-5 h-5" />
            </button>
            <input
              id="reader-progress-slider"
              type="range"
              min="0"
              max="1"
              step="0.01"
              value={progress}
              onChange={(e) => {
                const val = parseFloat(e.target.value);
                setProgress(val);
                onUpdateBookProgress(book.id, val, currentChapter.id, currentChapter.title);
              }}
              className="flex-1 accent-emerald-500 h-1.5 bg-stone-700 rounded-lg cursor-pointer"
            />
            <button
              id="reader-next-chapter-btn"
              type="button"
              onClick={handleNext}
              className="p-1.5 rounded-lg text-stone-300 hover:text-white hover:bg-stone-800 transition"
              title="下一页/下一章"
            >
              <ChevronRight className="w-5 h-5" />
            </button>
          </div>
        </div>
      )}

      {/* In-Book Full-Text Search Panel */}
      {overlay === 'SEARCH' && (
        <div
          id="search-panel"
          className="fixed inset-0 z-40 bg-stone-950/50 backdrop-blur-xs flex items-center justify-center p-4"
        >
          <div className="bg-white rounded-2xl max-w-lg w-full max-h-[80vh] flex flex-col shadow-2xl border border-stone-200 animate-in zoom-in-95 duration-200">
            <div className="p-4 border-b border-stone-200 flex items-center justify-between">
              <div className="flex items-center space-x-2">
                <Search className="w-4 h-4 text-emerald-800" />
                <h3 className="text-sm font-bold text-stone-900 font-serif">
                  全文流式检索 ({book.title})
                </h3>
              </div>
              <button
                type="button"
                onClick={() => setOverlay('NONE')}
                className="p-1 rounded-lg text-stone-400 hover:text-stone-700 hover:bg-stone-100 transition"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="p-3 border-b border-stone-100">
              <input
                type="text"
                value={searchKeyword}
                onChange={(e) => handlePerformSearch(e.target.value)}
                placeholder="输入关键字检索（例如：克莱恩、剑气、长安）..."
                className="w-full px-3 py-2 text-xs bg-stone-100 border border-stone-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-emerald-700/20 focus:border-emerald-700"
                autoFocus
              />
            </div>

            <div className="flex-1 overflow-y-auto p-3 space-y-2">
              {searchKeyword && searchResults.length === 0 ? (
                <div className="py-8 text-center text-xs text-stone-400">
                  未匹配到相关内容
                </div>
              ) : (
                searchResults.map((res, i) => (
                  <button
                    key={i}
                    onClick={() => handleSelectSearchResult(res)}
                    className="w-full text-left p-2.5 rounded-xl hover:bg-emerald-50/70 border border-transparent hover:border-emerald-200 transition space-y-1"
                  >
                    <div className="flex justify-between items-center text-[11px] font-semibold text-emerald-900">
                      <span>{res.chapterTitle}</span>
                      <span className="font-mono text-stone-400 text-[10px]">
                        偏移: {res.charOffset}
                      </span>
                    </div>
                    <p className="text-xs text-stone-600 line-clamp-2 text-justify">
                      {res.snippet}
                    </p>
                  </button>
                ))
              )}
            </div>
          </div>
        </div>
      )}

      {/* Supporting Pane TOC on Phone (Compact) */}
      {overlay === 'TOC' && widthSizeClass === 'COMPACT' && (
        <SupportingPaneToc
          chapters={book.chapters}
          currentChapterId={currentChapter.id}
          widthSizeClass={widthSizeClass}
          onSelectChapter={handleSelectChapter}
          onClose={() => setOverlay('NONE')}
        />
      )}

      {/* Reader Settings Bottom Sheet / Modal */}
      {overlay === 'SETTINGS' && (
        <ReaderSettingsSheet
          settings={settings}
          onChange={onUpdateSettings}
          onClose={() => setOverlay('NONE')}
        />
      )}
    </div>
  );
};
