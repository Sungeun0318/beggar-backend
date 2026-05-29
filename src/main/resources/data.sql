INSERT INTO users (user_name, password_hash, profile_image_url, uemail, role, created_at, updated_at, gender, age) VALUES
                                                                                                                       ('관리자', 'hash_admin123', 'http://example.com/admin.png', 'user01@kakao.com', 'ADMIN', NOW(), NOW(), 1, 30),
                                                                                                                       ('거지진감', 'hash_xyz123', 'http://example.com/p1.png', 'test.account02@kakao.com', 'USER', NOW(), NOW(), 1, 24),
                                                                                                                       ('거지판다', 'hash_abc456', 'http://example.com/p2.png', 'flutter_dev03@kakao.com', 'USER', NOW(), NOW(), 2, 27),
                                                                                                                       ('거지로봇', 'hash_qwer789', 'http://example.com/p3.png', 'mock.data04@kakao.com', 'USER', NOW(), NOW(), 1, 35),
                                                                                                                       ('거거', 'hash_asdf000', 'http://example.com/p4.png', 'frontend_tester05@kakao.com', 'USER', NOW(), NOW(), 2, 22);

INSERT INTO rooms (user_no, room_name, room_code, total_budget, room_created, is_friends) VALUES
                                                                                              (1, '강남 거지방', 'abc001', 60000, NOW(), TRUE),
                                                                                              (2, '홍대 맛집 탐방방', 'abc002', 80000, NOW(), FALSE),
                                                                                              (3, '자취생 정보공유', 'abc003', 45000, NOW(), FALSE),
                                                                                              (4, '배달비 아끼기방', 'abc004', 50000, NOW(), FALSE),
                                                                                              (5, '대학생 무지출챌린지', 'abc005', 30000, NOW(), FALSE),
                                                                                              (1, '직장인 점심 절약방', 'abc006', 70000, NOW(), FALSE),
                                                                                              (2, '신림동 룸메이트방', 'abc007', 55000, NOW(), TRUE),
                                                                                              (3, '편의점 꿀조합 공유', 'abc008', 20000, NOW(), FALSE),
                                                                                              (4, '주말 치팅데이 방', 'abc009', 90000, NOW(), FALSE),
                                                                                              (5, '고등학교 동창회방', 'abc010', 150000, NOW(), TRUE);