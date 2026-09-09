-- =====================================================================
-- WordFlow 示例词库（40 个 CET4 词，供开发调试）
-- 说明：正式上线前请替换/扩充为完整词库；每个单词都自带例句，
--       学习流程中的「英文短句展示」步骤会优先使用 AI 生成的句子，
--       无 AI Key 时（mock 模式）将回退到这里的例句。
-- =====================================================================
USE wordflow;

SET NAMES utf8mb4;

INSERT INTO learn_word (word, phonetic, chinese, part_of_speech, example_en, example_zh, difficulty, level) VALUES
('abandon', '/əˈbændən/', '放弃；抛弃', 'v.', 'Never abandon your dream even when times are hard.', '即使在困难时期也永远不要放弃你的梦想。', 3, 'CET4'),
('ability', '/əˈbɪləti/', '能力；才能', 'n.', 'She has the ability to solve complex problems quickly.', '她有快速解决复杂问题的能力。', 2, 'CET4'),
('absorb', '/əbˈzɔːrb/', '吸收；使专心', 'v.', 'Plants absorb water and nutrients from the soil.', '植物从土壤中吸收水分和养分。', 3, 'CET4'),
('academic', '/ˌækəˈdemɪk/', '学术的；学业的', 'adj.', 'His academic performance improved greatly this semester.', '这学期他的学业成绩有了很大提高。', 3, 'CET4'),
('access', '/ˈækses/', '通道；使用权；进入', 'n./v.', 'Students have free access to the online library.', '学生可以免费使用在线图书馆。', 2, 'CET4'),
('accident', '/ˈæksɪdənt/', '事故；意外', 'n.', 'The traffic accident was caused by careless driving.', '这起交通事故是由粗心驾驶引起的。', 2, 'CET4'),
('accompany', '/əˈkʌmpəni/', '陪伴；伴随', 'v.', 'Parents should accompany their children while they grow up.', '父母应该在孩子成长过程中陪伴他们。', 3, 'CET4'),
('accomplish', '/əˈkɑːmplɪʃ/', '完成；实现', 'v.', 'You can accomplish anything if you set your mind to it.', '只要下定决心，你就能完成任何事。', 4, 'CET4'),
('accurate', '/ˈækjərət/', '准确的；精确的', 'adj.', 'The weather forecast is not always accurate.', '天气预报并不总是准确的。', 3, 'CET4'),
('achieve', '/əˈtʃiːv/', '达到；取得；实现', 'v.', 'Hard work helped him achieve his goal.', '努力工作帮助他实现了目标。', 2, 'CET4'),
('adapt', '/əˈdæpt/', '适应；改编', 'v.', 'It takes time to adapt to a new environment.', '适应新环境需要时间。', 3, 'CET4'),
('adequate', '/ˈædɪkwət/', '足够的；适当的', 'adj.', 'Make sure you get adequate sleep before the exam.', '考试前要确保有充足的睡眠。', 4, 'CET4'),
('adjust', '/əˈdʒʌst/', '调整；适应', 'v.', 'You need to adjust your study plan according to the schedule.', '你需要根据时间表调整学习计划。', 3, 'CET4'),
('admire', '/ədˈmaɪər/', '钦佩；欣赏', 'v.', 'I admire her courage to speak in public.', '我钦佩她当众发言的勇气。', 2, 'CET4'),
('advantage', '/ədˈvæntɪdʒ/', '优势；有利条件', 'n.', 'Speaking two languages gives you an advantage in job hunting.', '会说两种语言让你在求职中占优势。', 3, 'CET4'),
('adventure', '/ədˈventʃər/', '冒险；奇遇', 'n.', 'The trip to the mountains was a great adventure.', '这次山区之旅是一次很棒的冒险。', 2, 'CET4'),
('affect', '/əˈfekt/', '影响；感动', 'v.', 'Lack of sleep can seriously affect your health.', '睡眠不足会严重影响你的健康。', 3, 'CET4'),
('ambition', '/æmˈbɪʃn/', '雄心；抱负', 'n.', 'His ambition is to become a great scientist.', '他的抱负是成为一名伟大的科学家。', 3, 'CET4'),
('ancient', '/ˈeɪnʃənt/', '古代的；古老的', 'adj.', 'We visited an ancient temple on the hill.', '我们参观了山上一座古老的寺庙。', 2, 'CET4'),
('anxious', '/ˈæŋkʃəs/', '焦虑的；渴望的', 'adj.', 'She was anxious about the result of the interview.', '她对面试结果感到焦虑。', 3, 'CET4'),
('apologize', '/əˈpɑːlədʒaɪz/', '道歉', 'v.', 'You should apologize for being late again.', '你应该为再次迟到道歉。', 2, 'CET4'),
('apparent', '/əˈpærənt/', '明显的；表面上的', 'adj.', 'It was apparent that he had not prepared for the speech.', '很明显他没有为演讲做准备。', 4, 'CET4'),
('appeal', '/əˈpiːl/', '呼吁；吸引；上诉', 'v./n.', 'The charity appeals to people to donate money.', '这家慈善机构呼吁人们捐款。', 4, 'CET4'),
('appreciate', '/əˈpriːʃieɪt/', '欣赏；感激', 'v.', 'I really appreciate your timely help.', '我非常感激你及时的帮助。', 3, 'CET4'),
('approach', '/əˈproʊtʃ/', '方法；接近', 'n./v.', 'We need a new approach to solve this problem.', '我们需要一种新方法来解决这个问题。', 3, 'CET4'),
('appropriate', '/əˈproʊpriət/', '适当的；恰当的', 'adj.', 'Wear appropriate clothes for the job interview.', '面试时要穿合适的衣服。', 4, 'CET4'),
('approve', '/əˈpruːv/', '批准；赞成', 'v.', 'The committee approved the new plan yesterday.', '委员会昨天批准了新计划。', 3, 'CET4'),
('argue', '/ˈɑːrɡjuː/', '争论；主张', 'v.', 'It is useless to argue with him about small things.', '为小事和他争论没有意义。', 2, 'CET4'),
('arrange', '/əˈreɪndʒ/', '安排；整理', 'v.', 'Please arrange a meeting for Friday morning.', '请为周五上午安排一次会议。', 3, 'CET4'),
('assess', '/əˈses/', '评估；评定', 'v.', 'Teachers assess students based on their daily performance.', '老师根据学生的日常表现进行评估。', 4, 'CET4'),
('assume', '/əˈsuːm/', '假定；承担', 'v.', 'We cannot assume that everyone has the same opinion.', '我们不能假定每个人都有相同的看法。', 4, 'CET4'),
('attach', '/əˈtætʃ/', '附上；使依恋', 'v.', 'Please attach your resume to the email.', '请把简历附在邮件里。', 3, 'CET4'),
('attempt', '/əˈtempt/', '尝试；企图', 'v./n.', 'She made another attempt to pass the driving test.', '她再次尝试通过驾照考试。', 3, 'CET4'),
('attitude', '/ˈætɪtuːd/', '态度；看法', 'n.', 'A positive attitude helps you face difficulties.', '积极的态度能帮助你面对困难。', 2, 'CET4'),
('attract', '/əˈtrækt/', '吸引', 'v.', 'The new museum attracts thousands of visitors every day.', '新博物馆每天吸引成千上万的游客。', 2, 'CET4'),
('available', '/əˈveɪləbl/', '可获得的；有空的', 'adj.', 'The tickets are available online now.', '现在可以在网上买到票。', 3, 'CET4'),
('avoid', '/əˈvɔɪd/', '避免；回避', 'v.', 'You should avoid eating too much fast food.', '你应该避免吃太多快餐。', 2, 'CET4'),
('aware', '/əˈwer/', '意识到的；知道的', 'adj.', 'Be aware of the risks before you make a decision.', '做决定之前要意识到风险。', 3, 'CET4'),
('balance', '/ˈbæləns/', '平衡；余额', 'n./v.', 'Keep a balance between work and rest.', '保持工作和休息之间的平衡。', 2, 'CET4'),
('benefit', '/ˈbenɪfɪt/', '利益；受益', 'n./v.', 'Regular exercise benefits both body and mind.', '经常锻炼对身心都有好处。', 2, 'CET4');
