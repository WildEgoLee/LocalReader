import React, { useState } from 'react';
import { Chapter, WindowWidthSizeClass } from '../types';
import { X, BookOpenCheck, Search } from 'lucide-react';

interface SupportingPaneTocProps {
  chapters: Chapter[];
  currentChapterId: string;
  widthSizeClass: WindowWidthSizeClass;
  onSelectChapter: (chapter: Chapter) => void;
  onClose: () => void;
}

export const SupportingPaneToc: React.FC<SupportingPaneTocProps> = ({
  chapters,
  currentChapterId,
  widthSizeClass,
  onSelectChapter,
  onClose,
}) => {
  const [filterQuery, setFilterQuery] = useState('');
  const isCompact = widthSizeClass === 'COMPACT';

  const filteredChapters = chapters.filter((c) =>
    !filterQuery.trim() || c.title.toLowerCase().includes(filterQuery.toLowerCase())
  );

  const content = (
    <div className="flex flex-col h-full bg-white">
      {/* Pane Header */}
      <div className="p-3.5 border-b border-stone-200/80 flex items-center justify-between bg-stone-50/70">
        <div className="flex items-center space-x-2 min-w-0">
          <BookOpenCheck className="w-4 h-4 text-emerald-800 flex-shrink-0" />
          <h3 className="text-sm font-bold text-stone-900 font-serif truncate">
            目录 ({chapters.length} 章)
          </h3>
        </div>
        <button
          id="close-toc-btn"
          type="button"
          onClick={onClose}
          className="p-1 rounded-lg text-stone-400 hover:text-stone-700 hover:bg-stone-200/50 transition flex-shrink-0"
        >
          <X className="w-4 h-4" />
        </button>
      </div>

      {/* Chapter Search / Filter */}
      <div className="p-2 border-b border-stone-100 bg-white">
        <div className="relative">
          <Search className="w-3.5 h-3.5 absolute left-2.5 top-1/2 -translate-y-1/2 text-stone-400" />
          <input
            type="text"
            value={filterQuery}
            onChange={(e) => setFilterQuery(e.target.value)}
            placeholder="筛选章节名称..."
            className="w-full pl-8 pr-2 py-1.5 text-xs bg-stone-100 border border-stone-200 rounded-lg focus:outline-none focus:ring-1 focus:ring-emerald-700"
          />
        </div>
      </div>

      {/* Chapter List */}
      <div className="flex-1 overflow-y-auto p-2 space-y-1 divide-y divide-stone-100">
        {filteredChapters.map((chapter) => {
          const isSelected = chapter.id === currentChapterId;
          return (
            <button
              key={chapter.id}
              id={`toc-item-${chapter.id}`}
              type="button"
              onClick={() => onSelectChapter(chapter)}
              className={`w-full text-left px-3 py-2.5 rounded-lg text-xs transition-colors flex items-center justify-between ${
                isSelected
                  ? 'bg-emerald-50 font-semibold text-emerald-900 border-l-2 border-emerald-700'
                  : 'text-stone-700 hover:bg-stone-100/70 font-normal'
              }`}
            >
              <span className="truncate pr-2">{chapter.title}</span>
              <div className="flex items-center space-x-1.5 flex-shrink-0">
                {chapter.characterCount > 0 && (
                  <span className="text-[10px] text-stone-400 font-mono">
                    {chapter.characterCount}字
                  </span>
                )}
                {isSelected && (
                  <span className="text-[10px] text-emerald-700 font-medium px-1.5 py-0.5 rounded bg-emerald-100/80">
                    当前
                  </span>
                )}
              </div>
            </button>
          );
        })}
      </div>
    </div>
  );

  // Phone / Compact mode: rendered as a slide-up bottom sheet
  if (isCompact) {
    return (
      <div className="fixed inset-0 z-40 bg-stone-950/40 backdrop-blur-xs flex flex-col justify-end">
        <div
          id="toc-bottom-sheet"
          className="bg-white rounded-t-2xl shadow-2xl max-h-[75vh] flex flex-col border-t border-stone-200"
        >
          {content}
        </div>
      </div>
    );
  }

  // Tablet / Expanded mode: rendered as a side Supporting Pane
  return (
    <aside
      id="toc-supporting-pane"
      className="w-72 sm:w-80 h-full bg-white border-r border-stone-200 shadow-sm flex-shrink-0 z-20 animate-in slide-in-from-left duration-200"
    >
      {content}
    </aside>
  );
};
