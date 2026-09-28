import { describe, expect, it } from 'vitest'
import { consumeSse, query } from './http'

describe('流式协议', () => {
  it('跨网络分块还原中文、空格、换行和完成事件', async () => {
    const input = new TextEncoder().encode('data:  hello\r\n\r\ndata: 你好\ndata: 第二行\n\ndata: [DONE]:conversation-1')
    const stream = new ReadableStream<Uint8Array>({ start(controller) { for (let i = 0; i < input.length; i += 3) controller.enqueue(input.slice(i, i + 3)); controller.close() } })
    const events: string[] = []
    await consumeSse(stream, data => events.push(data))
    expect(events).toEqual([' hello', '你好\n第二行', '[DONE]:conversation-1'])
  })
  it('忽略心跳与事件名，保留空 data 行', async () => {
    const stream = new ReadableStream<Uint8Array>({ start(controller) { controller.enqueue(new TextEncoder().encode(': heartbeat\n\nevent: message\ndata: a\ndata:\ndata: b\n\n')); controller.close() } })
    const events: string[] = []
    await consumeSse(stream, data => events.push(data))
    expect(events).toEqual(['a\n\nb'])
  })
  it('保留雪花 ID 的字符串精度', () => { expect(query({ id: '9223372036854775807', empty: '', zero: 0 })).toBe('id=9223372036854775807&zero=0') })
})
