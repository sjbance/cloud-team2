CREATE TABLE appraisals
(
	id SERIAL PRIMARY KEY,
	user_id INT NOT NULL,
	house_id TEXT,
	mort_id INT,
	appraised_value NUMERIC,
	ins_sent BOOLEAN,
	mun_sent BOOLEAN
);
