CREATE TABLE users
(
	id SERIAL PRIMARY KEY,
	username TEXT,
	hashed_password TEXT
);

CREATE TABLE mbr_applications
(
	id SERIAL PRIMARY KEY,
	user_id INT NOT NULL REFERENCES mbr_users(id),
	name TEXT,
	address TEXT,
	phone_number TEXT,
	employer TEXT,
	life_insurance TEXT,
	job_title TEXT,
	job_salary NUMERIC,
	job_years INT,
	life_policy_id TEXT,
	life_policy_value NUMERIC
);
