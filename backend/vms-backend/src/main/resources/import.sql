-- Initial Seed Data for VMS Personnel

INSERT INTO personnel (id, full_name, department, title, email) VALUES (1, 'Ayşe Demir', 'Yazılım', 'Kıdemli Uzman', 'ayse.demir@firma.com');
INSERT INTO personnel (id, full_name, department, title, email) VALUES (2, 'Mehmet Yılmaz', 'İnsan Kaynakları', 'İK Müdürü', 'mehmet.yilmaz@firma.com');

-- Reset Sequence
ALTER SEQUENCE personnel_SEQ RESTART WITH 10;
