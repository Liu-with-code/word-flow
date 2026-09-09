// =====================================================================
// WordFlow 本地“假大模型”SSE 测试服务器（开发调试工具）
//
// 用途：
//   - 在没有真实 API Key、或不想消耗配额时，验证前端流式展示与后端
//     OpenAI 流式解析（stream=true / SSE）是否正常工作。
//
// 启动：
//   node tools/mock-llm-server.mjs 9199
//
// 让后端指向它（以 openai 模式运行）：
//   mvn spring-boot:run "-Dspring-boot.run.arguments=--ai.provider=openai
//       --ai.base-url=http://127.0.0.1:9199 --ai.api-key=test-key"
//
// 说明：
//   - 非流式请求（生成练习句/短文）返回固定 JSON；
//   - 流式请求（批改翻译）按 100ms 间隔逐字吐出 JSON 内容，模拟打字机效果。
// =====================================================================

import http from 'node:http'

const port = Number(process.argv[2] || 9199)

const server = http.createServer((req, res) => {
  let body = ''
  req.on('data', (chunk) => {
    body += chunk
  })
  req.on('end', () => {
    let parsed = {}
    try {
      parsed = JSON.parse(body)
    } catch {
      // 忽略解析失败，按默认非流式处理
    }

    if (parsed.stream) {
      // SSE 流式：把一段 JSON 拆成多个 delta 逐个推送
      res.writeHead(200, {
        'Content-Type': 'text/event-stream',
        'Cache-Control': 'no-cache',
        Connection: 'keep-alive',
      })
      const chunks = [
        '{"passed":false,"score":60,"comment":"',
        '模拟流式',
        '批改：你的翻译基本达意，但语序可以更自然。","standardAnswer":"标准译文","errors":[]}',
      ]
      let index = 0
      const timer = setInterval(() => {
        if (index < chunks.length) {
          const delta = { choices: [{ delta: { content: chunks[index] } }] }
          res.write(`data: ${JSON.stringify(delta)}\n\n`)
          index++
        } else {
          res.write('data: [DONE]\n\n')
          clearInterval(timer)
          res.end()
        }
      }, 100)
    } else {
      // 非流式：固定返回一个合法的 JSON 内容
      res.writeHead(200, { 'Content-Type': 'application/json' })
      const content =
        '{"sentenceEn":"A fake sentence for practice.","sentenceZh":"练习用的假句子。"}'
      res.end(JSON.stringify({ choices: [{ message: { content } }] }))
    }
  })
})

server.listen(port, () => {
  console.log(`mock-llm-server listening on http://127.0.0.1:${port}`)
})
