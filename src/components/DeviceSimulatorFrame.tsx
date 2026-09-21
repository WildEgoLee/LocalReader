import React from 'react';
import { DevicePreset, WindowWidthSizeClass } from '../types';
import { Smartphone, Tablet, Monitor, FoldVertical, Code } from 'lucide-react';

interface DeviceSimulatorFrameProps {
  preset: DevicePreset;
  onPresetChange: (preset: DevicePreset) => void;
  widthSizeClass: WindowWidthSizeClass;
  onOpenCodeInspector: () => void;
  children: React.ReactNode;
}

export const DeviceSimulatorFrame: React.FC<DeviceSimulatorFrameProps> = ({
  preset,
  onPresetChange,
  widthSizeClass,
  onOpenCodeInspector,
  children,
}) => {
  const sizeMap = {
    RESPONSIVE: 'w-full h-full max-w-full',
    PHONE: 'w-[412px] h-[892px] max-h-[92vh] rounded-[36px] shadow-2xl border-[10px] border-stone-800',
    FOLDABLE: 'w-[680px] h-[820px] max-h-[92vh] rounded-[28px] shadow-2xl border-[10px] border-stone-800',
    TABLET: 'w-[1060px] h-[720px] max-h-[92vh] rounded-[28px] shadow-2xl border-[10px] border-stone-800',
  }[preset];

  return (
    <div className="flex flex-col h-screen w-screen bg-stone-900 text-stone-100 overflow-hidden font-sans">
      {/* Top Simulator Control Bar */}
      <header className="flex-shrink-0 bg-stone-950 px-4 py-2 border-b border-stone-800 flex items-center justify-between text-xs">
        <div className="flex items-center space-x-3">
          <div className="flex items-center space-x-1.5">
            <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-pulse" />
            <span className="font-bold text-stone-100 tracking-wide">LeafReader</span>
            <span className="text-stone-500 font-mono text-[11px]">Android M3 Adaptive Simulator</span>
          </div>

          <div className="hidden md:flex items-center space-x-1.5 pl-3 border-l border-stone-800">
            <span className="text-stone-400">当前计算窗口:</span>
            <span
              className={`px-2 py-0.5 rounded-full font-mono font-bold text-[10px] ${
                widthSizeClass === 'COMPACT'
                  ? 'bg-amber-950/80 text-amber-300 border border-amber-800/60'
                  : widthSizeClass === 'MEDIUM'
                  ? 'bg-sky-950/80 text-sky-300 border border-sky-800/60'
                  : 'bg-emerald-950/80 text-emerald-300 border border-emerald-800/60'
              }`}
            >
              {widthSizeClass} (
              {widthSizeClass === 'COMPACT'
                ? '<600dp 手机布局'
                : widthSizeClass === 'MEDIUM'
                ? '600~840dp Rail导航'
                : '>=840dp 平板双栏展开'}
              )
            </span>
          </div>
        </div>

        {/* Device Mode Switcher */}
        <div className="flex items-center space-x-2">
          <div className="flex items-center bg-stone-800/80 p-0.5 rounded-lg border border-stone-700/80">
            <button
              id="device-preset-phone-btn"
              type="button"
              onClick={() => onPresetChange('PHONE')}
              className={`flex items-center gap-1 px-2.5 py-1 rounded-md transition ${
                preset === 'PHONE'
                  ? 'bg-emerald-700 text-white shadow-xs font-semibold'
                  : 'text-stone-400 hover:text-stone-200'
              }`}
              title="手机视口 (Compact <600dp)"
            >
              <Smartphone className="w-3.5 h-3.5" />
              <span className="hidden sm:inline">手机</span>
            </button>

            <button
              id="device-preset-foldable-btn"
              type="button"
              onClick={() => onPresetChange('FOLDABLE')}
              className={`flex items-center gap-1 px-2.5 py-1 rounded-md transition ${
                preset === 'FOLDABLE'
                  ? 'bg-emerald-700 text-white shadow-xs font-semibold'
                  : 'text-stone-400 hover:text-stone-200'
              }`}
              title="折叠屏展开 (Medium 600-840dp)"
            >
              <FoldVertical className="w-3.5 h-3.5" />
              <span className="hidden sm:inline">折叠屏</span>
            </button>

            <button
              id="device-preset-tablet-btn"
              type="button"
              onClick={() => onPresetChange('TABLET')}
              className={`flex items-center gap-1 px-2.5 py-1 rounded-md transition ${
                preset === 'TABLET'
                  ? 'bg-emerald-700 text-white shadow-xs font-semibold'
                  : 'text-stone-400 hover:text-stone-200'
              }`}
              title="横屏大平板 (Expanded >=840dp, 双栏书页效果)"
            >
              <Tablet className="w-3.5 h-3.5" />
              <span className="hidden sm:inline">平板双栏</span>
            </button>

            <button
              id="device-preset-responsive-btn"
              type="button"
              onClick={() => onPresetChange('RESPONSIVE')}
              className={`flex items-center gap-1 px-2.5 py-1 rounded-md transition ${
                preset === 'RESPONSIVE'
                  ? 'bg-emerald-700 text-white shadow-xs font-semibold'
                  : 'text-stone-400 hover:text-stone-200'
              }`}
              title="流式响应 (跟随浏览器窗口)"
            >
              <Monitor className="w-3.5 h-3.5" />
              <span className="hidden sm:inline">自由拉伸</span>
            </button>
          </div>

          <button
            id="view-android-code-btn"
            type="button"
            onClick={onOpenCodeInspector}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-emerald-900/60 hover:bg-emerald-800 text-emerald-200 border border-emerald-700/60 transition text-xs font-medium"
          >
            <Code className="w-3.5 h-3.5" />
            <span className="font-mono">Kotlin 源码</span>
          </button>
        </div>
      </header>

      {/* Simulator Staging Area */}
      <div className="flex-1 overflow-hidden p-0 sm:p-4 flex items-center justify-center bg-stone-950/60">
        <div className={`${sizeMap} transition-all duration-300 relative overflow-hidden flex flex-col bg-stone-50 text-stone-900`}>
          {children}
        </div>
      </div>
    </div>
  );
};
