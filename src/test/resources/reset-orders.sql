TRUNCATE orders RESTART IDENTITY;

-- Orders for user 1: coherent portfolio
INSERT INTO orders (instrumentId, userId, size, price, type, side, status, datetime) VALUES
  (3, 1, 100000, 1.00, 'MARKET', 'CASH_IN', 'FILLED', '2023-07-12 10:00:00');

INSERT INTO orders (instrumentId, userId, size, price, type, side, status, datetime) VALUES
  (1, 1, 10, 1000.00, 'MARKET', 'BUY', 'FILLED', '2023-07-12 11:00:00');

INSERT INTO orders (instrumentId, userId, size, price, type, side, status, datetime) VALUES
  (2, 1, 5, 2000.00, 'MARKET', 'BUY', 'FILLED', '2023-07-12 12:00:00');

INSERT INTO orders (instrumentId, userId, size, price, type, side, status, datetime) VALUES
  (1, 1, 5, 900.00, 'LIMIT', 'BUY', 'NEW', '2023-07-12 13:00:00');

INSERT INTO orders (instrumentId, userId, size, price, type, side, status, datetime) VALUES
  (1, 1, 100, 1200.00, 'MARKET', 'SELL', 'REJECTED', '2023-07-12 14:00:00');

INSERT INTO orders (instrumentId, userId, size, price, type, side, status, datetime) VALUES
  (2, 1, 2, 1900.00, 'LIMIT', 'BUY', 'CANCELLED', '2023-07-12 15:00:00');

-- Orders for user 2: minimal, only cash
INSERT INTO orders (instrumentId, userId, size, price, type, side, status, datetime) VALUES
  (3, 2, 50000, 1.00, 'MARKET', 'CASH_IN', 'FILLED', '2023-07-12 10:00:00');
