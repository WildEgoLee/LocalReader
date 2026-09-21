import React from 'react';
import {
  ReaderColumnMode,
  ReaderSettings,
  ReaderThemePalette,
  ReadingPageMode,
} from '../types';
import { X, Minus, Plus, Columns2, Square } from 'lucide-react';

interface ReaderSettingsSheetProps {
  settings: ReaderSettings;
  onChange: (updated: Partial<ReaderSettings>) => void;
  onClose: () => void;
}

export const ReaderSettingsSheet: React.FC<ReaderSettingsSheetProps> = ({
  settings,
  onChange,
  onClose,
}) => {
  return (
    <div className="fixed inset-0 z-50 bg-stone-950/40 backdrop-blur-xs flex flex-col justify-end sm:items-center sm:justify-center p-0 sm:p-4">
      <div
        id="reader-settings-dialog"
        className="bg-white text-stone-900 rounded-t-2xl sm:rounded-2xl w-full max-w-md p-5 shadow-2xl border border-stone-200/90 space-y-5 animate-in slide-in-from-bottom duration-200"
      >
        {/* Header */}
        <div className="flex items-center justify-between border-b border-stone-100 pb-3">
          <h3 className="text-sm font-bold text-stone-900">阅读排版与外观</h3>
          <button
            id="close-reader-settings-btn"
            type="button"
            onClick={onClose}
            className="p-1 text-stone-400 hover:text-stone-700 rounded-lg hover:bg-stone-100"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Themes Palette */}
        <div className="space-y-2">
          <label className="text-xs font-semibold text-stone-600">背景色调</label>
          <div className="grid grid-cols-4 gap-2.5">
            <button
              id="theme-btn-paper"
              type="button"
              onClick={() => onChange({ themePalette: 'PAPER' })}
              className={`p-2.5 rounded-xl border flex flex-col items-center gap-1.5 transition ${
                settings.themePalette === 'PAPER'
                  ? 'border-emerald-700 ring-2 ring-emerald-700/20'
                  : 'border-stone-200 hover:border-stone-300'
              }`}
              style={{ backgroundColor: '#F6F1E7', color: '#332F29' }}
            >
              <div className="w-5 h-5 rounded-full border border-stone-300 bg-[#F6F1E7]" />
              <span className="text-[11px] font-medium">羊皮纸</span>
            </button>

            <button
              id="theme-btn-light"
              type="button"
              onClick={() => onChange({ themePalette: 'LIGHT' })}
              className={`p-2.5 rounded-xl border flex flex-col items-center gap-1.5 transition ${
                settings.themePalette === 'LIGHT'
                  ? 'border-emerald-700 ring-2 ring-emerald-700/20'
                  : 'border-stone-200 hover:border-stone-300'
              }`}
              style={{ backgroundColor: '#FAFAFA', color: '#202124' }}
            >
              <div className="w-5 h-5 rounded-full border border-stone-300 bg-[#FAFAFA]" />
              <span className="text-[11px] font-medium">浅白</span>
            </button>

            <button
              id="theme-btn-dark"
              type="button"
              onClick={() => onChange({ themePalette: 'DARK' })}
              className={`p-2.5 rounded-xl border flex flex-col items-center gap-1.5 transition ${
                settings.themePalette === 'DARK'
                  ? 'border-emerald-500 ring-2 ring-emerald-500/20'
                  : 'border-stone-700 hover:border-stone-600'
              }`}
              style={{ backgroundColor: '#202124', color: '#E6E1E5' }}
            >
              <div className="w-5 h-5 rounded-full border border-stone-600 bg-[#202124]" />
              <span className="text-[11px] font-medium">夜间</span>
            </button>

            <button
              id="theme-btn-amoled"
              type="button"
              onClick={() => onChange({ themePalette: 'AMOLED' })}
              className={`p-2.5 rounded-xl border flex flex-col items-center gap-1.5 transition ${
                settings.themePalette === 'AMOLED'
                  ? 'border-emerald-500 ring-2 ring-emerald-500/20'
                  : 'border-stone-800 hover:border-stone-700'
              }`}
              style={{ backgroundColor: '#000000', color: '#DCDCDC' }}
            >
              <div className="w-5 h-5 rounded-full border border-stone-700 bg-[#000000]" />
              <span className="text-[11px] font-medium">AMOLED</span>
            </button>
          </div>
        </div>

        {/* Font Size & Line Spacing */}
        <div className="space-y-3 pt-1">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-stone-600">正文字号</span>
            <div className="flex items-center gap-2 bg-stone-100 p-1 rounded-xl">
              <button
                id="font-size-dec-btn"
                type="button"
                onClick={() => onChange({ fontSizeSp: Math.max(13, settings.fontSizeSp - 1) })}
                className="w-7 h-7 rounded-lg bg-white text-stone-700 shadow-xs flex items-center justify-center hover:bg-stone-50"
              >
                <Minus className="w-3.5 h-3.5" />
              </button>
              <span className="w-10 text-center font-mono text-xs font-bold text-stone-800">
                {settings.fontSizeSp}sp
              </span>
              <button
                id="font-size-inc-btn"
                type="button"
                onClick={() => onChange({ fontSizeSp: Math.min(28, settings.fontSizeSp + 1) })}
                className="w-7 h-7 rounded-lg bg-white text-stone-700 shadow-xs flex items-center justify-center hover:bg-stone-50"
              >
                <Plus className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>

          <div className="space-y-1">
            <div className="flex justify-between text-xs">
              <span className="font-semibold text-stone-600">行距</span>
              <span className="font-mono text-stone-500">{settings.lineSpacingMultiplier.toFixed(1)}x</span>
            </div>
            <input
              type="range"
              min="1.3"
              max="2.3"
              step="0.1"
              value={settings.lineSpacingMultiplier}
              onChange={(e) => onChange({ lineSpacingMultiplier: parseFloat(e.target.value) })}
              className="w-full accent-emerald-700 cursor-pointer h-1.5 bg-stone-200 rounded-lg"
            />
          </div>
        </div>

        {/* Reading Page Mode */}
        <div className="space-y-2 pt-1 border-t border-stone-100">
          <label className="text-xs font-semibold text-stone-600">翻页体验</label>
          <div className="grid grid-cols-2 gap-2">
            <button
              id="pagemode-paged-btn"
              type="button"
              onClick={() => onChange({ pageMode: 'PAGED' })}
              className={`py-2 px-3 rounded-lg border text-xs font-medium transition text-center ${
                settings.pageMode === 'PAGED'
                  ? 'border-emerald-700 bg-emerald-50 text-emerald-800'
                  : 'border-stone-200 text-stone-600 hover:bg-stone-50'
              }`}
            >
              左右分页翻页
            </button>
            <button
              id="pagemode-scroll-btn"
              type="button"
              onClick={() => onChange({ pageMode: 'SCROLL' })}
              className={`py-2 px-3 rounded-lg border text-xs font-medium transition text-center ${
                settings.pageMode === 'SCROLL'
                  ? 'border-emerald-700 bg-emerald-50 text-emerald-800'
                  : 'border-stone-200 text-stone-600 hover:bg-stone-50'
              }`}
            >
              平滑上下滚动
            </button>
          </div>
        </div>

        {/* Column Mode for Tablet / Adaptive */}
        <div className="space-y-2 pt-1">
          <label className="text-xs font-semibold text-stone-600 flex items-center justify-between">
            <span>平板/宽屏分栏</span>
            <span className="text-[10px] text-stone-400 font-normal">展开书卷效果</span>
          </label>
          <div className="grid grid-cols-3 gap-2">
            {(['AUTO', 'SINGLE', 'DUAL'] as ReaderColumnMode[]).map((mode) => (
              <button
                key={mode}
                type="button"
                onClick={() => onChange({ columnMode: mode })}
                className={`py-1.5 px-2 rounded-lg border text-xs font-medium transition text-center ${
                  settings.columnMode === mode
                    ? 'border-emerald-700 bg-emerald-50 text-emerald-800'
                    : 'border-stone-200 text-stone-600 hover:bg-stone-50'
                }`}
              >
                {mode === 'AUTO' ? '自适应' : mode === 'SINGLE' ? '单栏' : '双栏'}
              </button>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};
