-- ================================================================= --
-- PRODUTOS
-- ================================================================= --

INSERT INTO products (name, normalized_name, description, price, stock, created_at, updated_at, deleted_at) VALUES
('Cafeteira Elétrica Mondial', 'cafeteira eletrica mondial', 'Prepara até 20 xícaras de café.', 199.90, 50, NOW(), NULL, NULL),
('Filtro de Papel Melitta 103', 'filtro de papel melitta 103', 'Caixa com 30 unidades.', 5.50, 200, NOW(), NULL, NULL),
('Máquina de Espresso Profissional', 'maquina de espresso profissional', 'Com moedor de grãos integrado.', 2500.00, 10, NOW(), NULL, NULL),
('Cápsulas de Café Intenso', 'capsulas de cafe intenso', 'Compatível com máquinas Nespresso.', 45.80, 150, NOW(), NULL, NULL),
('Chaleira Elétrica Inox', 'chaleira eletrica inox', 'Capacidade de 1.7 litros, desligamento automático.', 149.99, 30, NOW(), NULL, NULL),
('Moedor de Café Manual', 'moedor de cafe manual', 'Lâminas de cerâmica para uma moagem precisa.', 89.90, 40, NOW(), NULL, NULL),
('Prensa Francesa 600ml', 'prensa francesa 600ml', 'Vidro de borossilicato e estrutura de aço inox.', 110.00, 25, NOW(), NULL, NULL),
('Bule Térmico 1L - Vermelho', 'bule termico 1l vermelho', 'Mantém a temperatura por até 12 horas.', 75.40, 0, NOW(), NULL, NULL),
('Xícara de Porcelana Branca', 'xicara de porcelana branca', 'Design clássico e elegante.', 12.00, 120, NOW(), NULL, NOW()),
('Leiteira de Alumínio 1.5L', 'leiteira de aluminio 1.5l', 'Ferve o leite de forma rápida e segura.', 35.00, 60, NOW(), DATEADD('DAY', -1, NOW()), NULL); -- Corrigido aqui


-- ================================================================= --
-- CUPONS
-- ================================================================= --

-- Cupons de Porcentagem Válidos
INSERT INTO coupons (code, type, coupon_value, one_shot, max_uses, uses_count, valid_from, valid_until, created_at, deleted_at) VALUES
('promo10', 'PERCENT', 10, false, 100, 10, DATEADD('DAY', -1, NOW()), DATEADD('DAY', 30, NOW()), NOW(), NULL),
('desconto25', 'PERCENT', 25, false, 50, 5, DATEADD('DAY', -10, NOW()), DATEADD('DAY', 20, NOW()), NOW(), NULL),
('superoferta', 'PERCENT', 50, false, 20, 1, NOW(), DATEADD('DAY', 7, NOW()), NOW(), NULL),
('relampago5', 'PERCENT', 5, false, NULL, 90, DATEADD('HOUR', -1, NOW()), DATEADD('HOUR', 1, NOW()), NOW(), NULL),

-- Cupons de Valor Fixo Válidos
('vale10', 'FIXED', 10.00, false, 200, 30, DATEADD('DAY', -15, NOW()), DATEADD('DAY', 15, NOW()), NOW(), NULL),
('vale50', 'FIXED', 50.00, false, 25, 2, NOW(), DATEADD('DAY', 60, NOW()), NOW(), NULL),
('descontao100', 'FIXED', 100.00, false, 10, 0, NOW(), DATEADD('DAY', 90, NOW()), NOW(), NULL),

-- Cupons com Regras Especiais
('primeiracompra', 'PERCENT', 15, true, NULL, 0, NOW(), DATEADD('YEAR', 1, NOW()), NOW(), NULL),
('limite10', 'FIXED', 5.00, false, 10, 10, DATEADD('DAY', -5, NOW()), DATEADD('DAY', 5, NOW()), NOW(), NULL),
('jaera', 'PERCENT', 20, false, 100, 50, DATEADD('DAY', -60, NOW()), DATEADD('DAY', -30, NOW()), NOW(), NULL),
('embreve', 'PERCENT', 30, false, 100, 0, DATEADD('DAY', 10, NOW()), DATEADD('DAY', 40, NOW()), NOW(), NULL),
('inativo', 'FIXED', 15.00, false, 50, 10, DATEADD('DAY', -1, NOW()), DATEADD('DAY', 30, NOW()), NOW(), NOW()),
('oneshotusado', 'FIXED', 7.50, true, NULL, 1, DATEADD('DAY', -10, NOW()), DATEADD('DAY', 20, NOW()), NOW(), NULL),

-- Cupons genéricos para teste de volume
('teste01', 'PERCENT', 5, false, NULL, 0, NOW(), DATEADD('YEAR', 1, NOW()), NOW(), NULL),
('teste02', 'FIXED', 2.50, false, 500, 0, NOW(), DATEADD('YEAR', 1, NOW()), NOW(), NULL);