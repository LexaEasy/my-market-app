MERGE INTO items (id, title, description, img_path, price) KEY (id) VALUES
    (1, 'Футбольный мяч', 'Классический мяч для игры на открытом поле.', 'images/ball.svg', 1490),
    (2, 'Спортивная бутылка', 'Лёгкая бутылка для тренировок и прогулок.', 'images/bottle.svg', 690),
    (3, 'Рюкзак городской', 'Компактный рюкзак с отделением для ноутбука.', 'images/backpack.svg', 3490),
    (4, 'Кружка керамическая', 'Кружка для горячих напитков объёмом 350 мл.', 'images/mug.svg', 540),
    (5, 'Настольная лампа', 'Лампа с регулируемым углом наклона.', 'images/lamp.svg', 2190);

ALTER TABLE items ALTER COLUMN id RESTART WITH 6;
