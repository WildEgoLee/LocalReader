import React, { useState, useEffect } from 'react';
import {
  Book,
  BookFormat,
  DevicePreset,
  ReaderSettings,
  WindowWidthSizeClass,
} from './types';
import { initialMockBooks, initialReaderSettings } from './mockData';
import { BookshelfView } from './components/BookshelfView';
import { ReaderView } from './components/ReaderView';
import { SettingsView } from './components/SettingsView';
import { DeviceSimulatorFrame } from './components/DeviceSimulatorFrame';
import { CodeInspectorModal } from './components/CodeInspectorModal';
import { BookOpen, Settings, Library, CheckCircle2 } from 'lucide-react';

export function App() {
  const [books, setBooks] = useState<Book[]>(initialMockBooks);
  const [settings, setSettings] = useState<ReaderSettings>(initialReaderSettings);
  const [activeBookId, setActiveBookId] = useState<number | null>(null);
  const [currentTab, setCurrentTab] = useState<'bookshelf' | 'settings'>('bookshelf');
  const [devicePreset, setDevicePreset] = useState<DevicePreset>('RESPONSIVE');
  const [showCodeInspector, setShowCodeInspector] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  // Dynamic window width calculation based on simulator preset or window width
  const [windowWidth, setWindowWidth] = useState<number>(window.innerWidth);

  useEffect(() => {
    const handleResize = () => setWindowWidth(window.innerWidth);
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  // Determine widthSizeClass
  let widthSizeClass: WindowWidthSizeClass = 'COMPACT';
  let isLandscape = false;

  if (devicePreset === 'PHONE') {
    widthSizeClass = 'COMPACT';
    isLandscape = false;
  } else if (devicePreset === 'FOLDABLE') {
    widthSizeClass = 'MEDIUM';
    isLandscape = false;
  } else if (devicePreset === 'TABLET') {
    widthSizeClass = 'EXPANDED';
    isLandscape = true;
  } else {
    // Responsive
    if (windowWidth < 600) {
      widthSizeClass = 'COMPACT';
      isLandscape = false;
    } else if (windowWidth < 840) {
      widthSizeClass = 'MEDIUM';
      isLandscape = false;
    } else {
      widthSizeClass = 'EXPANDED';
      isLandscape = window.innerWidth > window.innerHeight;
    }
  }

  const activeBook = books.find((b) => b.id === activeBookId) || null;

  const handleSelectBook = (book: Book) => {
    setActiveBookId(book.id);
  };

  const handleBackToBookshelf = () => {
    setActiveBookId(null);
  };

  const handleAddBook = (newBook: Book) => {
    setBooks((prev) => [newBook, ...prev]);
    showToast(`成功导入《${newBook.title}》(${newBook.format}) · 共 ${newBook.chapters.length} 章`);
  };

  const handleUpdateBookProgress = (
    bookId: number,
    progress: number,
    locatorInfo: string,
    chapterTitle: string
  ) => {
    setBooks((prev) =>
      prev.map((b) => {
        if (b.id !== bookId) return b;
        return {
          ...b,
          readingProgress: progress,
          lastReadAt: Date.now(),
          lastChapterTitle: chapterTitle,
          currentLocator: {
            ...((b.currentLocator as any) || { type: b.format }),
            relativeProgress: progress,
          },
        };
      })
    );
  };

  const handleUpdateSettings = (updated: Partial<ReaderSettings>) => {
    setSettings((prev) => ({ ...prev, ...updated }));
  };

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3000);
  };

  return (
    <DeviceSimulatorFrame
      preset={devicePreset}
      onPresetChange={setDevicePreset}
      widthSizeClass={widthSizeClass}
      onOpenCodeInspector={() => setShowCodeInspector(true)}
    >
      {/* If reading a book, present ReaderView edge-to-edge */}
      {activeBook ? (
        <ReaderView
          book={activeBook}
          settings={settings}
          widthSizeClass={widthSizeClass}
          isLandscape={isLandscape}
          onBackToBookshelf={handleBackToBookshelf}
          onUpdateBookProgress={handleUpdateBookProgress}
          onUpdateSettings={handleUpdateSettings}
        />
      ) : (
        /* Otherwise, present adaptive main navigation suite (Bookshelf / Settings) */
        <div className="flex h-full w-full overflow-hidden bg-stone-50">
          {/* NavigationRail for Medium (Foldable/Small Tablet) and Expanded (Tablet/Desktop) */}
          {widthSizeClass !== 'COMPACT' && (
            <aside
              id="adaptive-navigation-rail"
              className="w-16 sm:w-20 flex-shrink-0 bg-stone-100/90 border-r border-stone-200/90 flex flex-col items-center py-5 justify-between select-none z-10"
            >
              <div className="flex flex-col items-center space-y-6 w-full">
                <div className="w-10 h-10 rounded-xl bg-emerald-800 text-white flex items-center justify-center font-serif font-bold text-base shadow-xs">
                  叶
                </div>

                <nav className="flex flex-col space-y-2 w-full px-2">
                  <button
                    id="nav-rail-bookshelf-btn"
                    type="button"
                    onClick={() => setCurrentTab('bookshelf')}
                    className={`flex flex-col items-center py-2.5 px-1 rounded-xl text-xs transition ${
                      currentTab === 'bookshelf'
                        ? 'bg-emerald-800/10 text-emerald-900 font-bold'
                        : 'text-stone-500 hover:text-stone-800 hover:bg-stone-200/50'
                    }`}
                  >
                    <Library className="w-5 h-5 mb-1" />
                    <span className="text-[11px]">书架</span>
                  </button>

                  <button
                    id="nav-rail-settings-btn"
                    type="button"
                    onClick={() => setCurrentTab('settings')}
                    className={`flex flex-col items-center py-2.5 px-1 rounded-xl text-xs transition ${
                      currentTab === 'settings'
                        ? 'bg-emerald-800/10 text-emerald-900 font-bold'
                        : 'text-stone-500 hover:text-stone-800 hover:bg-stone-200/50'
                    }`}
                  >
                    <Settings className="w-5 h-5 mb-1" />
                    <span className="text-[11px]">设置</span>
                  </button>
                </nav>
              </div>

              <div className="text-[10px] font-mono text-stone-400 rotate-180 [writing-mode:vertical-rl]">
                LEAF · M3
              </div>
            </aside>
          )}

          {/* Main Workspace Area */}
          <main className="flex-1 flex flex-col h-full overflow-hidden relative">
            {currentTab === 'bookshelf' ? (
              <BookshelfView
                books={books}
                widthSizeClass={widthSizeClass}
                onSelectBook={handleSelectBook}
                onAddBook={handleAddBook}
              />
            ) : (
              <SettingsView
                settings={settings}
                widthSizeClass={widthSizeClass}
                onUpdateSettings={handleUpdateSettings}
                onOpenCodeInspector={() => setShowCodeInspector(true)}
              />
            )}

            {/* BottomNavigationBar for Compact (Phone) width */}
            {widthSizeClass === 'COMPACT' && (
              <nav
                id="adaptive-bottom-navigation-bar"
                className="flex-shrink-0 bg-stone-50/95 backdrop-blur-md border-t border-stone-200/90 flex items-center justify-around py-2 select-none z-10"
              >
                <button
                  id="bottom-nav-bookshelf-btn"
                  type="button"
                  onClick={() => setCurrentTab('bookshelf')}
                  className={`flex flex-col items-center justify-center flex-1 py-1 transition ${
                    currentTab === 'bookshelf'
                      ? 'text-emerald-800 font-semibold'
                      : 'text-stone-400 hover:text-stone-600'
                  }`}
                >
                  <Library className="w-5 h-5" />
                  <span className="text-[10px] mt-1">我的书架</span>
                </button>

                <button
                  id="bottom-nav-settings-btn"
                  type="button"
                  onClick={() => setCurrentTab('settings')}
                  className={`flex flex-col items-center justify-center flex-1 py-1 transition ${
                    currentTab === 'settings'
                      ? 'text-emerald-800 font-semibold'
                      : 'text-stone-400 hover:text-stone-600'
                  }`}
                >
                  <Settings className="w-5 h-5" />
                  <span className="text-[10px] mt-1">系统设置</span>
                </button>
              </nav>
            )}
          </main>
        </div>
      )}

      {/* Code Inspector Modal */}
      {showCodeInspector && (
        <CodeInspectorModal onClose={() => setShowCodeInspector(false)} />
      )}

      {/* Toast Notification */}
      {toastMessage && (
        <div className="fixed bottom-6 left-1/2 -translate-x-1/2 z-50 bg-stone-900/90 text-white text-xs px-4 py-2.5 rounded-xl shadow-lg flex items-center gap-2 animate-in fade-in slide-in-from-bottom-2">
          <CheckCircle2 className="w-4 h-4 text-emerald-400" />
          <span>{toastMessage}</span>
        </div>
      )}
    </DeviceSimulatorFrame>
  );
}

export default App;
