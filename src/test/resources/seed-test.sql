-- Users
INSERT INTO users (id, email, accountNumber) VALUES
  (1, 'test@example.com', 'TEST001'),
  (2, 'other@example.com', 'TEST002');

-- Instruments
INSERT INTO instruments (id, ticker, name, type) VALUES
  (1, 'TEST', 'Test Company S.A.', 'ACCIONES'),
  (2, 'OTHR', 'Other Company S.A.', 'ACCIONES'),
  (3, 'ARS', 'PESOS', 'MONEDA'),
  (4, 'NOMD', 'No Market Data S.A.', 'ACCIONES');

-- Market data
INSERT INTO marketdata (instrumentId, date, open, high, low, close, previousClose) VALUES
  (1, '2023-07-13', 980.00, 1120.00, 970.00, 1100.00, 1050.00),
  (2, '2023-07-13', 1950.00, 2150.00, 1900.00, 2100.00, 2000.00);

-- Orders for user 1: coherent portfolio
-- Cash in 100,000 ARS
INSERT INTO orders (instrumentId, userId, size, price, type, side, status, datetime) VALUES
  (3, 1, 100000, 1.00, 'MARKET', 'CASH_IN', 'FILLED', '2023-07-12 10:00:00');

-- BUY 10 TEST @ 1000 = 10,000 ARS
INSERT INTO orders (instrumentId, userId, size, price, type, side, status, datetime) VALUES
  (1, 1, 10, 1000.00, 'MARKET', 'BUY', 'FILLED', '2023-07-12 11:00:00');

-- BUY 5 OTHR @ 2000 = 10,000 ARS
INSERT INTO orders (instrumentId, userId, size, price, type, side, status, datetime) VALUES
  (2, 1, 5, 2000.00, 'MARKET', 'BUY', 'FILLED', '2023-07-12 12:00:00');

-- NEW BUY LIMIT: 5 TEST @ 900 (does not reserve under D-1)
INSERT INTO orders (instrumentId, userId, size, price, type, side, status, datetime) VALUES
  (1, 1, 5, 900.00, 'LIMIT', 'BUY', 'NEW', '2023-07-12 13:00:00');

-- REJECTED SELL: trying to sell 100 TEST when only 10 available
INSERT INTO orders (instrumentId, userId, size, price, type, side, status, datetime) VALUES
  (1, 1, 100, 1200.00, 'MARKET', 'SELL', 'REJECTED', '2023-07-12 14:00:00');

-- CANCELLED BUY LIMIT
INSERT INTO orders (instrumentId, userId, size, price, type, side, status, datetime) VALUES
  (2, 1, 2, 1900.00, 'LIMIT', 'BUY', 'CANCELLED', '2023-07-12 15:00:00');

-- Orders for user 2: minimal, only cash
INSERT INTO orders (instrumentId, userId, size, price, type, side, status, datetime) VALUES
  (3, 2, 50000, 1.00, 'MARKET', 'CASH_IN', 'FILLED', '2023-07-12 10:00:00');
