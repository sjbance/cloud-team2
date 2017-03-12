CREATE TABLE users
(
	id SERIAL PRIMARY KEY,
	username TEXT,
	hashed_password TEXT
);

CREATE TABLE applications
(
	id SERIAL PRIMARY KEY,
	user_id INT NOT NULL REFERENCES users(id),
	name TEXT,
	mortgage_value NUMERIC,
	house_id TEXT,
	salary NUMERIC,
	start_of_employment TEXT,
	insured_value NUMERIC,
	deductible NUMERIC
);
