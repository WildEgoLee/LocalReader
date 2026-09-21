export type BookFormat = 'TXT' | 'EPUB';

export interface TxtLocator {
  type: 'TXT';
  chapterId: string;
  charOffset: number;
  paragraphIndex: number;
  relativeProgress: number;
}

export interface EpubLocator {
  type: 'EPUB';
  href: string;
  title?: string;
  cfi?: string;
  relativeProgress: number;
}

export type BookLocator = TxtLocator | EpubLocator;

export interface Chapter {
  id: string;
  title: string;
  orderIndex: number;
  locator: BookLocator;
  characterCount: number;
  content: string;
}

export interface SearchResult {
  chapterId: string;
  chapterTitle: string;
  charOffset: number;
  snippet: string;
  relativeProgress: number;
}

export interface Book {
  id: number;
  title: string;
  author: string;
  format: BookFormat;
  uri: string;
  fileSize: number;
  encoding?: string;
  addedAt: number;
  lastReadAt?: number;
  readingProgress: number;
  currentLocator?: BookLocator;
  lastChapterTitle?: string;
  chapters: Chapter[];
}

export type ReaderThemePalette = 'PAPER' | 'LIGHT' | 'DARK' | 'AMOLED';
export type ReadingPageMode = 'PAGED' | 'SCROLL';
export type ReaderColumnMode = 'AUTO' | 'SINGLE' | 'DUAL';

export interface ReaderSettings {
  themePalette: ReaderThemePalette;
  fontSizeSp: number;
  lineSpacingMultiplier: number;
  paragraphSpacingDp: number;
  horizontalMarginDp: number;
  fontFamily: 'serif' | 'sans';
  pageMode: ReadingPageMode;
  columnMode: ReaderColumnMode;
  volumeKeyNavigation: boolean;
  keepScreenOn: boolean;
}

export type ReaderOverlay = 'NONE' | 'CONTROLS' | 'TOC' | 'SETTINGS' | 'SEARCH';

export type WindowWidthSizeClass = 'COMPACT' | 'MEDIUM' | 'EXPANDED';

export type DevicePreset = 'RESPONSIVE' | 'PHONE' | 'FOLDABLE' | 'TABLET';
