import { Book, Chapter, BookFormat } from '../types';

export interface ParseTxtResult {
  title: string;
  author: string;
  encoding: string;
  fileSize: number;
  totalChars: number;
  chapters: Chapter[];
}

// Regex rules for chapter detection mirroring Kotlin ChapterDetector.kt
const CHAPTER_REGEX_RULES = [
  /^[ \t]*(第[0-9一二两三四五六七八九十百千万]+[章回节卷部篇集]\s*([^\n\r]{0,35}))/m,
  /^[ \t]*(序[章言]?|前言|楔子|引子|尾声|后记|番外(?:篇)?\s*([^\n\r]{0,35}))/m,
  /^[ \t]*(Chapter\s+\d+([^\n\r]{0,35}))/im,
  /^[ \t]*(\d{1,4}[.、\s]+[^\n\r]{1,35})/m,
];

const INVALID_STARTS = new Set(['“', '"', '‘', '\'', '（', '(', '【']);
const INVALID_ENDINGS = new Set(['，', '。', '；', '！', '？', ',', ';', '!']);

export function matchChapterTitle(line: string): string | null {
  const trimmed = line.trim();
  if (trimmed.length < 2 || trimmed.length > 45) return null;
  if (INVALID_STARTS.has(trimmed[0])) return null;
  if (trimmed.length > 10 && INVALID_ENDINGS.has(trimmed[trimmed.length - 1])) return null;

  for (const regex of CHAPTER_REGEX_RULES) {
    const match = trimmed.match(regex);
    if (match && match[0]) {
      return match[0].trim();
    }
  }
  return null;
}

/**
 * Automatically decodes buffer with UTF-8 or GBK fallback
 */
export async function decodeTextBuffer(buffer: ArrayBuffer): Promise<{ text: string; encoding: string }> {
  // Check BOM
  const bytes = new Uint8Array(buffer);
  if (bytes.length >= 3 && bytes[0] === 0xEF && bytes[1] === 0xBB && bytes[2] === 0xBF) {
    return { text: new TextDecoder('utf-8').decode(buffer), encoding: 'UTF-8 (BOM)' };
  }

  // Try UTF-8 with fatal=true
  try {
    const utf8Decoder = new TextDecoder('utf-8', { fatal: true });
    const text = utf8Decoder.decode(buffer);
    return { text, encoding: 'UTF-8' };
  } catch {
    // Fallback to GBK / GB18030
    try {
      const gbkDecoder = new TextDecoder('gbk', { fatal: false });
      const text = gbkDecoder.decode(buffer);
      return { text, encoding: 'GBK' };
    } catch {
      return { text: new TextDecoder('utf-8').decode(buffer), encoding: 'UTF-8 (Fallback)' };
    }
  }
}

/**
 * Parses raw text into structured chapters using streaming-like line scan
 */
export function parseTxtContent(
  rawText: string,
  fileName: string,
  fileSize: number,
  encoding: string
): ParseTxtResult {
  const lines = rawText.split(/\r?\n/);
  const cleanTitle = fileName
    .replace(/\.(txt|epub)$/i, '')
    .replace(/\[(精校版?|全本|全集|校对|精编|完本|TXT无错版?)\]/gi, '')
    .replace(/\((精校版?|全本|全集|校对|精编|完本|TXT无错版?)\)/gi, '')
    .replace(/[_]+/g, ' ')
    .trim();

  const chapters: Chapter[] = [];
  let currentTitle = '序言';
  let currentLines: string[] = [];
  let chapterIndex = 1;
  let accumulatedChars = 0;

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    const detectedTitle = matchChapterTitle(line);

    if (detectedTitle && currentLines.length > 0) {
      const content = currentLines.join('\n').trim();
      const charCount = content.length;
      chapters.push({
        id: `ch_${chapterIndex}`,
        title: currentTitle,
        orderIndex: chapterIndex,
        characterCount: charCount,
        content,
        locator: {
          type: 'TXT',
          chapterId: `ch_${chapterIndex}`,
          charOffset: 0,
          paragraphIndex: 0,
          relativeProgress: 0,
        },
      });
      chapterIndex++;
      currentTitle = detectedTitle;
      currentLines = [];
    } else if (detectedTitle && currentLines.length === 0) {
      currentTitle = detectedTitle;
    } else {
      currentLines.push(line);
    }
    accumulatedChars += line.length + 1;
  }

  // Push the final chapter
  if (currentLines.length > 0 || chapters.length === 0) {
    const content = currentLines.join('\n').trim() || `${cleanTitle}\n暂无内容`;
    chapters.push({
      id: `ch_${chapterIndex}`,
      title: currentTitle,
      orderIndex: chapterIndex,
      characterCount: content.length,
      content,
      locator: {
        type: 'TXT',
        chapterId: `ch_${chapterIndex}`,
        charOffset: 0,
        paragraphIndex: 0,
        relativeProgress: 0,
      },
    });
  }

  // Calculate relative progress for each chapter
  const totalChars = chapters.reduce((sum, ch) => sum + ch.characterCount, 0);
  let runningChars = 0;
  for (const ch of chapters) {
    const progress = totalChars > 0 ? runningChars / totalChars : 0;
    ch.locator.relativeProgress = progress;
    runningChars += ch.characterCount;
  }

  return {
    title: cleanTitle || '未命名小说',
    author: '本地导入',
    encoding,
    fileSize,
    totalChars,
    chapters,
  };
}
