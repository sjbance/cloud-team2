INSERT INTO mbr_users(username, hashed_password)
VALUES ('bob', '$2a$04$Mw3aV/aCvehbPQP0jKbBPuIxWk9XCHyMLgNrzWu3hYcC5QNRnuBmK');

INSERT INTO lii_users(username, hashed_password, policy_id, policy_value)
VALUES ('bob', '$2a$04$Mw3aV/aCvehbPQP0jKbBPuIxWk9XCHyMLgNrzWu3hYcC5QNRnuBmK', '1234', 100000);

INSERT INTO wfc_users (employee_id, hashed_password, employee_name, job_title, years_of_service, yearly_salary)
VALUES ('999', '$2a$04$Mw3aV/aCvehbPQP0jKbBPuIxWk9XCHyMLgNrzWu3hYcC5QNRnuBmK', 'Bob Joe', 'Head Widget Coordinator', 15, 50000);