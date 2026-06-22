import { ApiError } from './client';
import { authHeaders } from './client';
import type { ChatRequest, ChatResponse } from './types';

interface ChatDeltaEvent {
  text: string;
}

interface ChatErrorEvent {
  error: string;
  message: string;
  status: number;
}

export interface ChatStreamHandlers {
  onDelta: (text: string) => void;
  onDone: (response: ChatResponse) => void;
}

async function parseError(res: Response): Promise<ApiError> {
  try {
    const body = (await res.json()) as { error: string; message: string };
    return new ApiError(res.status, body.error, body.message);
  } catch {
    return new ApiError(res.status, 'UNKNOWN', res.statusText);
  }
}

function parseSseEvents(buffer: string): { events: SseEvent[]; rest: string } {
  const events: SseEvent[] = [];
  const blocks = buffer.split('\n\n');
  const rest = blocks.pop() ?? '';

  for (const block of blocks) {
    if (!block.trim()) continue;
    let eventName = 'message';
    const dataLines: string[] = [];
    for (const line of block.split('\n')) {
      if (line.startsWith('event:')) {
        eventName = line.slice(6).trim();
      } else if (line.startsWith('data:')) {
        dataLines.push(line.slice(5).trimStart());
      }
    }
    if (dataLines.length > 0) {
      events.push({ event: eventName, data: dataLines.join('\n') });
    }
  }

  return { events, rest };
}

interface SseEvent {
  event: string;
  data: string;
}

function handleSseEvent(event: SseEvent, handlers: ChatStreamHandlers): boolean {
  if (event.event === 'delta') {
    const payload = JSON.parse(event.data) as ChatDeltaEvent;
    handlers.onDelta(payload.text);
    return false;
  }
  if (event.event === 'done') {
    handlers.onDone(JSON.parse(event.data) as ChatResponse);
    return true;
  }
  if (event.event === 'error') {
    const payload = JSON.parse(event.data) as ChatErrorEvent;
    throw new ApiError(
      payload.status ?? 500,
      payload.error ?? 'UNKNOWN',
      payload.message ?? '流式响应失败',
    );
  }
  return false;
}

export async function sendChatStream(
  req: ChatRequest,
  handlers: ChatStreamHandlers,
  signal?: AbortSignal,
): Promise<void> {
  const res = await fetch('/api/chat/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream, application/json',
      ...authHeaders(),
    },
    body: JSON.stringify(req),
    signal,
  });

  if (!res.ok) {
    throw await parseError(res);
  }

  const reader = res.body?.getReader();
  if (!reader) {
    throw new Error('浏览器不支持流式响应');
  }

  const decoder = new TextDecoder();
  let buffer = '';

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;

    buffer += decoder.decode(value, { stream: true });
    const parsed = parseSseEvents(buffer);
    buffer = parsed.rest;

    for (const event of parsed.events) {
      if (handleSseEvent(event, handlers)) {
        return;
      }
    }
  }

  if (buffer.trim()) {
    const parsed = parseSseEvents(buffer + '\n\n');
    for (const event of parsed.events) {
      if (handleSseEvent(event, handlers)) {
        return;
      }
    }
  }
}
