-- Initial Seed Data for VMS Personnel

INSERT INTO personnel (id, full_name, department, title, email) VALUES (1, 'Ayşe Demir', 'Yazılım', 'Kıdemli Uzman', 'ayse.demir@firma.com');
INSERT INTO personnel (id, full_name, department, title, email) VALUES (2, 'Mehmet Yılmaz', 'İnsan Kaynakları', 'İK Müdürü', 'mehmet.yilmaz@firma.com');

-- Reset PostgreSQL IDENTITY Sequences to MAX(id)
SELECT setval('personnel_id_seq', (SELECT COALESCE(MAX(id), 1) FROM personnel));
SELECT setval('users_id_seq', (SELECT COALESCE(MAX(id), 1) FROM users));
SELECT setval('visitor_id_seq', (SELECT COALESCE(MAX(id), 1) FROM visitor));
