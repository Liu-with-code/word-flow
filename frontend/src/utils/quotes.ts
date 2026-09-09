/**
 * 随机文艺短句池（用于侧边栏标语、首页副标题、登录/注册页 slogan）。
 * 来源：经典诗文与原创鼓励短句，随机展示增加趣味与鼓励。
 */

export interface Quote {
  text: string
  source?: string
}

/** 侧边栏等小空间使用的短句 */
export const shortQuotes: Quote[] = [
  { text: '厚积薄发' },
  { text: '学而时习之' },
  { text: '温故而知新' },
  { text: '水滴石穿' },
  { text: '博观约取' },
  { text: '日拱一卒' },
  { text: '功不唐捐' },
  { text: '行稳致远' },
  { text: '宁静致远' },
  { text: '久久为功' },
]

/** 首页 / 登录 / 注册等大空间使用的完整短句 */
export const fullQuotes: Quote[] = [
  { text: '学而时习之，不亦说乎？', source: '《论语》' },
  { text: '温故而知新，可以为师矣。', source: '《论语》' },
  { text: '知之者不如好之者，好之者不如乐之者。', source: '《论语》' },
  { text: '博观而约取，厚积而薄发。', source: '苏轼' },
  { text: '不积跬步，无以至千里；不积小流，无以成江海。', source: '《荀子》' },
  { text: '锲而不舍，金石可镂。', source: '《荀子》' },
  { text: '千淘万漉虽辛苦，吹尽狂沙始到金。', source: '刘禹锡' },
  { text: '路漫漫其修远兮，吾将上下而求索。', source: '屈原' },
  { text: '纸上得来终觉浅，绝知此事要躬行。', source: '陆游' },
  { text: '长风破浪会有时，直挂云帆济沧海。', source: '李白' },
  { text: '山重水复疑无路，柳暗花明又一村。', source: '陆游' },
  { text: '宝剑锋从磨砺出，梅花香自苦寒来。', source: '《警世贤文》' },
  { text: '书山有路勤为径，学海无涯苦作舟。', source: '韩愈' },
  { text: '天行健，君子以自强不息。', source: '《周易》' },
  { text: '问渠那得清如许，为有源头活水来。', source: '朱熹' },
  { text: '博学之，审问之，慎思之，明辨之，笃行之。', source: '《礼记》' },
  { text: '千里之行，始于足下。', source: '《道德经》' },
  { text: '少壮不努力，老大徒伤悲。', source: '《乐府诗集》' },
  { text: '你背过的每个单词，都会在某个不经意的瞬间派上用场。' },
  { text: '今天多认识一个词，明天就多一种看世界的角度。' },
  { text: '语言是世界的窗户，每打开一扇，就多一束光。' },
  { text: '记忆会走远，但每一次重复，都是回家的路。' },
  { text: '与其羡慕别人的词典，不如慢慢翻厚自己的那一本。' },
  { text: '慢一点没关系，只要一直在走。' },
]

export function randomShortQuote(): Quote {
  return shortQuotes[Math.floor(Math.random() * shortQuotes.length)]
}

export function randomQuote(): Quote {
  return fullQuotes[Math.floor(Math.random() * fullQuotes.length)]
}
