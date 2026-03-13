INSERT INTO categories.categories
(category_id, category_name, category_icon, priority, left_over_options, user_id, is_deleted)
VALUES
    (gen_random_uuid(), 'Food', 'FOOD', 1, 'RESET_TO_ORIGINAL_AMOUNT', NULL, false),
    (gen_random_uuid(), 'Transport', 'TRANSPORT', 2, 'RESET_TO_ORIGINAL_AMOUNT', NULL, false),
    (gen_random_uuid(), 'Shopping', 'SHOPPING', 4, 'RESET_TO_ORIGINAL_AMOUNT', NULL, false),
    (gen_random_uuid(), 'HealthCare', 'HEALTHCARE', 3, 'RESET_TO_ORIGINAL_AMOUNT', NULL, false),
    (gen_random_uuid(), 'Entertainment', 'ENTERTAINMENT', 5, 'RESET_TO_ORIGINAL_AMOUNT', NULL, false);