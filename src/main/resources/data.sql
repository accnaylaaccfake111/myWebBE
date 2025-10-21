-- Insert sample lyrics templates
INSERT INTO lyrics_templates (theme, mood, style, template, example, rhyme_pattern, line_count, popularity, usage_count, is_active, author, source) VALUES
-- Mùa xuân templates
('Mùa xuân', 'Vui vẻ', 'lục bát', 
'Xuân về rộn rã muôn nơi
Hoa đào nở rộ, người người vui mừng
Gió xuân thổi nhẹ vô cùng
Mang theo hạnh phúc trong lòng mọi người

Cành mai vàng rực sân nhà
Tiếng cười trẻ thơ vang xa phố phường
Xuân này thật đẹp vô thường
Niềm vui chan chứa con đường ta đi', 
'Xuân về rộn rã muôn nơi...', '6-8-6-8', 8, 90, 0, true, 'Traditional', 'traditional'),

('Mùa xuân', 'Lãng mạn', 'song thất', 
'Xuân về trong gió heo may
Hoa tình nở rộ đắm say bao người
Dưới trời xuân ấm nụ cười
Em như cánh bướm tươi vui bay xa

Anh ngồi đếm cánh hoa sa
Nhớ em da diết như hoa mùa xuân
Tình yêu như nắng xuân vần
Sưởi ấm trái tim bao lần yêu thương', 
'Xuân về trong gió heo may...', '7-7-7-7', 8, 85, 0, true, 'Modern', 'modern'),

-- Học đường templates  
('Học đường', 'Hoài niệm', 'tự do',
'Sân trường xưa bóng phượng hồng
Tiếng ve sầu hát mùa hè nắng vàng
Ghế đá vẫn đó hàng cây
Nhớ thời học trò những ngày thơ ngây

Bạn bè quây quần bên nhau
Sẻ chia từng nỗi niềm sầu niềm vui
Thầy cô tận tụy dạy bảo
Cho ta hành trang vào đời mai sau', 
'Sân trường xưa bóng phượng hồng...', 'free', 8, 95, 0, true, 'Traditional', 'traditional'),

('Học đường', 'Vui vẻ', 'lục bát',
'Sáng nay đến trường thật vui
Bạn bè sum họp, cười tươi rạng rỡ
Thầy cô giảng bài say sưa
Con chữ mở ra cánh cửa tương lai

Giờ ra chơi đã đến rồi
Sân trường rộn rã tiếng cười vang lên
Tuổi thơ trong sáng dịu hiền
Mãi là kỷ niệm không quên trong lòng',
'Sáng nay đến trường thật vui...', '6-8-6-8', 8, 88, 0, true, 'Modern', 'modern'),

-- Thầy cô templates
('Thầy cô', 'Truyền cảm hứng', 'song thất',
'Thầy cô như ánh mặt trời
Soi đường dẫn lối cho đời học trò
Bao nhiêu khó nhọc chẳng lo
Miễn sao học trò nên người mai sau

Công ơn sâu nặng biết bao
Dù cho năm tháng trôi mau vẫn nhớ
Lời thầy cô dạy năm xưa
Là kim chỉ nam cho ta suốt đời',
'Thầy cô như ánh mặt trời...', '7-7-7-7', 8, 92, 0, true, 'Traditional', 'traditional'),

('Thầy cô', 'Biết ơn', 'lục bát',
'Công thầy như núi Thái Sơn
Nghĩa cô như nước trong nguồn chảy ra
Bao năm vun đắp tài hoa
Cho con nên người, cho ta nên người

Dù mai có cách xa rồi
Lòng con ghi nhớ suốt đời ơn thầy
Những bài học quý giá này
Theo con mãi mãi những ngày về sau',
'Công thầy như núi Thái Sơn...', '6-8-6-8', 8, 96, 0, true, 'Traditional', 'traditional'),

-- Bạn bè templates
('Bạn bè', 'Vui vẻ', 'tự do',
'Chúng mình là bạn thân nhau
Cùng vui cùng khóc bao năm qua rồi
Dù cho khó khăn ngập trời
Có nhau vượt qua, nụ cười vẫn tươi

Kỷ niệm đẹp mãi không phai
Tình bạn chân thành như mai vẫn xanh
Cùng nhau ta tiến về đích
Bạn ơi hãy nhớ tình mình mãi thôi',
'Chúng mình là bạn thân nhau...', 'free', 8, 87, 0, true, 'Modern', 'modern'),

('Bạn bè', 'Hoài niệm', 'song thất',
'Nhớ thuở còn bé thơ ngây
Cùng nhau nô đùa những ngày hồn nhiên
Tình bạn trong sáng như tiên
Không màng tính toán, chỉ riêng chân tình

Giờ đây mỗi đứa một miền
Nhưng tình bạn cũ vẫn nguyên như xưa
Dù cho năm tháng qua mau
Trong tim vẫn nhớ những câu ca dao',
'Nhớ thuở còn bé thơ ngây...', '7-7-7-7', 8, 89, 0, true, 'Traditional', 'traditional'),

-- Quê hương templates
('Quê hương', 'Hoài niệm', 'lục bát',
'Quê hương là chùm khế ngọt
Cho con trèo hái mỗi ngày
Quê hương là đường đi học
Con về trên lưng trâu già

Quê hương mỗi độ hoa cau
Mẹ về thăm ngoại trên đầu yến bay
Quê hương có nắng mưa ngày
Cho con lớn lên thành người mai sau',
'Quê hương là chùm khế ngọt...', '6-8-6-8', 8, 98, 0, true, 'Traditional', 'traditional'),

('Quê hương', 'Tự hào', 'song thất',
'Quê hương tôi đẹp giàu sang
Ruộng vàng bát ngát, sông ngàn cá tôm
Làng xóm yên ả êm đềm
Tình người ấm áp như niềm tin yêu

Dù đi muôn dặm xa xôi
Lòng tôi vẫn nhớ nơi tôi sinh ra
Quê hương mãi mãi trong ta
Là nguồn sức mạnh vượt qua gian nan',
'Quê hương tôi đẹp giàu sang...', '7-7-7-7', 8, 91, 0, true, 'Modern', 'modern');