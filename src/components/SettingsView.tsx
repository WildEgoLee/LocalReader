import React from 'react';
import { ReaderSettings, WindowWidthSizeClass } from '../types';
import {
  Settings,
  Palette,
  Type,
  Layout,
  ShieldCheck,
  Cpu,
  Layers,
  Sparkles,
} from 'lucide-react';

interface SettingsViewProps {
  settings: ReaderSettings;
  widthSizeClass: WindowWidthSizeClass;
  onUpdateSettings: (updated: Partial<ReaderSettings>) => void;
  onOpenCodeInspector: () => void;
}

export const SettingsView: React.FC<SettingsViewProps> = ({
  settings,
  widthSizeClass,
  onUpdateSettings,
  onOpenCodeInspector,
}) => {
  return (
    <div id="settings-container" className="flex flex-col h-full overflow-y-auto bg-stone-50 text-stone-900">
      {/* Top Header */}
      <header className="sticky top-0 z-10 bg-stone-50/90 backdrop-blur-md px-4 py-3.5 border-b border-stone-200/80 flex items-center justify-between">
        <div className="flex items-center space-x-2">
          <Settings className="w-5 h-5 text-emerald-800" />
          <h1 className="text-lg font-bold tracking-tight text-stone-900 font-serif">
            阅读与系统设置
          </h1>
        </div>
        <button
          type="button"
          onClick={onOpenCodeInspector}
          className="flex items-center space-x-1 px-3 py-1.5 rounded-lg bg-stone-200/70 hover:bg-stone-300/80 text-xs font-medium text-stone-800 transition"
        >
          <Layers className="w-3.5 h-3.5" />
          <span>查看 Kotlin 架构源码</span>
        </button>
      </header>

      {/* Main Settings List */}
      <div className="p-4 sm:p-6 max-w-3xl mx-auto w-full space-y-6">
        {/* Appearance & Palette */}
        <section className="bg-white rounded-2xl p-5 border border-stone-200/90 shadow-xs space-y-4">
          <h2 className="text-sm font-bold text-stone-900 flex items-center gap-2">
            <Palette className="w-4 h-4 text-emerald-800" />
            默认色彩主题
          </h2>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            {[
              { id: 'PAPER', name: '羊皮纸', bg: '#F6F1E7', text: '#332F29', border: '#E2DAC8' },
              { id: 'LIGHT', name: '浅白', bg: '#FAFAFA', text: '#202124', border: '#E5E7EB' },
              { id: 'DARK', name: '夜间', bg: '#202124', text: '#E6E1E5', border: '#374151' },
              { id: 'AMOLED', name: 'AMOLED', bg: '#000000', text: '#DCDCDC', border: '#262626' },
            ].map((theme) => (
              <button
                key={theme.id}
                type="button"
                onClick={() => onUpdateSettings({ themePalette: theme.id as any })}
                className={`p-3 rounded-xl border flex flex-col items-center gap-2 transition ${
                  settings.themePalette === theme.id
                    ? 'border-emerald-700 ring-2 ring-emerald-700/20 shadow-xs'
                    : 'border-stone-200 hover:border-stone-300'
                }`}
                style={{ backgroundColor: theme.bg, color: theme.text }}
              >
                <div
                  className="w-6 h-6 rounded-full border"
                  style={{ backgroundColor: theme.bg, borderColor: theme.border }}
                />
                <span className="text-xs font-semibold">{theme.name}</span>
              </button>
            ))}
          </div>
        </section>

        {/* Typography & Spacing */}
        <section className="bg-white rounded-2xl p-5 border border-stone-200/90 shadow-xs space-y-5">
          <h2 className="text-sm font-bold text-stone-900 flex items-center gap-2">
            <Type className="w-4 h-4 text-emerald-800" />
            排版与排版参数
          </h2>

          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <span className="text-sm font-medium text-stone-800">基准字号</span>
                <p className="text-xs text-stone-400">调整正文阅读字号大小</p>
              </div>
              <div className="flex items-center gap-3">
                <button
                  type="button"
                  onClick={() => onUpdateSettings({ fontSizeSp: Math.max(13, settings.fontSizeSp - 1) })}
                  className="w-8 h-8 rounded-lg border border-stone-300 text-stone-700 hover:bg-stone-50 flex items-center justify-center font-bold"
                >
                  -
                </button>
                <span className="w-10 text-center font-mono font-bold text-sm text-stone-800">
                  {settings.fontSizeSp}sp
                </span>
                <button
                  type="button"
                  onClick={() => onUpdateSettings({ fontSizeSp: Math.min(28, settings.fontSizeSp + 1) })}
                  className="w-8 h-8 rounded-lg border border-stone-300 text-stone-700 hover:bg-stone-50 flex items-center justify-center font-bold"
                >
                  +
                </button>
              </div>
            </div>

            <div className="space-y-1.5 pt-2 border-t border-stone-100">
              <div className="flex justify-between text-xs">
                <span className="font-medium text-stone-800">行间距</span>
                <span className="font-mono text-stone-500">{settings.lineSpacingMultiplier.toFixed(1)}x</span>
              </div>
              <input
                type="range"
                min="1.3"
                max="2.3"
                step="0.1"
                value={settings.lineSpacingMultiplier}
                onChange={(e) => onUpdateSettings({ lineSpacingMultiplier: parseFloat(e.target.value) })}
                className="w-full accent-emerald-700 h-1.5 bg-stone-200 rounded-lg cursor-pointer"
              />
            </div>

            <div className="space-y-1.5 pt-2 border-t border-stone-100">
              <div className="flex justify-between text-xs">
                <span className="font-medium text-stone-800">左右页边距</span>
                <span className="font-mono text-stone-500">{settings.horizontalMarginDp}dp</span>
              </div>
              <input
                type="range"
                min="16"
                max="48"
                step="4"
                value={settings.horizontalMarginDp}
                onChange={(e) => onUpdateSettings({ horizontalMarginDp: parseInt(e.target.value, 10) })}
                className="w-full accent-emerald-700 h-1.5 bg-stone-200 rounded-lg cursor-pointer"
              />
            </div>
          </div>
        </section>

        {/* Layout & Behavior */}
        <section className="bg-white rounded-2xl p-5 border border-stone-200/90 shadow-xs space-y-4">
          <h2 className="text-sm font-bold text-stone-900 flex items-center gap-2">
            <Layout className="w-4 h-4 text-emerald-800" />
            翻页与大屏分栏
          </h2>

          <div className="space-y-3">
            <div>
              <label className="text-xs font-medium text-stone-700 block mb-1.5">阅读翻页方式</label>
              <div className="grid grid-cols-2 gap-2">
                <button
                  type="button"
                  onClick={() => onUpdateSettings({ pageMode: 'PAGED' })}
                  className={`py-2 px-3 rounded-xl border text-xs font-medium transition text-center ${
                    settings.pageMode === 'PAGED'
                      ? 'border-emerald-700 bg-emerald-50 text-emerald-900'
                      : 'border-stone-200 text-stone-600 hover:bg-stone-50'
                  }`}
                >
                  左右分页翻页
                </button>
                <button
                  type="button"
                  onClick={() => onUpdateSettings({ pageMode: 'SCROLL' })}
                  className={`py-2 px-3 rounded-xl border text-xs font-medium transition text-center ${
                    settings.pageMode === 'SCROLL'
                      ? 'border-emerald-700 bg-emerald-50 text-emerald-900'
                      : 'border-stone-200 text-stone-600 hover:bg-stone-50'
                  }`}
                >
                  平滑上下滚动
                </button>
              </div>
            </div>

            <div className="pt-2 border-t border-stone-100">
              <label className="text-xs font-medium text-stone-700 block mb-1.5">平板双栏排版策略</label>
              <div className="grid grid-cols-3 gap-2">
                {(['AUTO', 'SINGLE', 'DUAL'] as const).map((mode) => (
                  <button
                    key={mode}
                    type="button"
                    onClick={() => onUpdateSettings({ columnMode: mode })}
                    className={`py-2 px-2 rounded-xl border text-xs font-medium transition text-center ${
                      settings.columnMode === mode
                        ? 'border-emerald-700 bg-emerald-50 text-emerald-900'
                        : 'border-stone-200 text-stone-600 hover:bg-stone-50'
                    }`}
                  >
                    {mode === 'AUTO' ? '自适应宽屏' : mode === 'SINGLE' ? '强制单栏' : '强制双栏'}
                  </button>
                ))}
              </div>
            </div>
          </div>
        </section>

        {/* Local First Architecture Overview */}
        <section className="bg-stone-100/80 rounded-2xl p-5 border border-stone-200/90 space-y-3">
          <div className="flex items-center space-x-2 text-stone-800">
            <ShieldCheck className="w-4 h-4 text-emerald-800" />
            <h3 className="text-xs font-bold uppercase tracking-wider">
              Local-First 架构准则 (Phase 0)
            </h3>
          </div>
          <ul className="text-xs text-stone-600 space-y-1.5 leading-relaxed">
            <li className="flex items-start gap-1.5">
              <span className="text-emerald-700 font-bold">•</span>
              <span><strong>逻辑 Locator：</strong>进度与物理页码解耦，字号缩放或旋转屏幕不丢失位置。</span>
            </li>
            <li className="flex items-start gap-1.5">
              <span className="text-emerald-700 font-bold">•</span>
              <span><strong>统一 ReaderEngine：</strong>抽象接口，TXT 与 EPUB (Readium) 具有一致的 TOC、进度与搜索契约。</span>
            </li>
            <li className="flex items-start gap-1.5">
              <span className="text-emerald-700 font-bold">•</span>
              <span><strong>Material 3 Adaptive：</strong>基于当前 WindowSizeClass 响应式切换 Phone BottomNav 与 Tablet NavigationRail。</span>
            </li>
            <li className="flex items-start gap-1.5">
              <span className="text-emerald-700 font-bold">•</span>
              <span><strong>零云端依赖：</strong>纯离线小说阅读器，书架保存在本地 SQLite，绝不泄露用户数据。</span>
            </li>
          </ul>
        </section>
      </div>
    </div>
  );
};
