CREATE TABLE IF NOT EXISTS players
(
	id		INT		NOT NULL,
	name		TEXT		NOT NULL,
	playChance_next	INT		NOT NULL,
	position	INT		NOT NULL,
	form		DOUBLE		NOT NULL,
	value_form	DOUBLE		NOT NULL,
	value_season	DOUBLE		NOT NULL,
	team_code	INT		NOT NULL,
	total_points	INT		NOT NULL,
	point_per_game	DOUBLE		NOT NULL,
	ep_next		DOUBLE		NOT NULL,
	cost		INT		NOT NULL,
	ict_index_rank	INT		NOT NULL,
	ict_index	DOUBLE		NOT NULL,
	minutes		INT		NOT NULL,
	PRIMARY KEY(id)
);

CREATE TABLE IF NOT EXISTS teams
(
	code		INT		NOT NULL,
	id		INT		NOT NULL,
	name		TEXT		NOT NULL,
	strength	INT		NOT NULL,
	PRIMARY KEY(code)
);

CREATE TABLE IF NOT EXISTS gameweeks
(
	id		INT		NOT NULL,
	deadline	BIGINT		NOT NULL,
	is_current	TINYINT(1)	NOT NULL,
	is_next		TINYINT(1)	NOT NULL,
	PRIMARY KEY(id)
);

CREATE TABLE IF NOT EXISTS fixtures
(
	event		INT		NOT NULL,
	id		INT		NOT NULL,
	away		INT		NOT NULL,
	home		INT		NOT NULL,
	PRIMARY KEY(id)
);

CREATE TABLE IF NOT EXISTS user_data
(
	data		VARCHAR(64)	NOT NULL,
	value		BIGINT		NOT NULL,
	PRIMARY KEY(data)
);

CREATE TABLE IF NOT EXISTS player_history
(
	id		INT		NOT NULL,
	start_cost	DOUBLE		NOT NULL,
	end_cost	DOUBLE		NOT NULL,
	total_points	INT		NOT NULL,
	minutes		INT		NOT NULL,
	ict_index	DOUBLE		NOT NULL,
	PRIMARY KEY(id)
);

CREATE TABLE IF NOT EXISTS squad_data
(
	position	INT		NOT NULL,
	id		INT		NOT NULL,
	name		VARCHAR(64)	NOT NULL,
	purchase_price	DOUBLE		NOT NULL,
	PRIMARY KEY(position)
);

CREATE TABLE IF NOT EXISTS past_fixtures
(
	id		INT		NOT NULL,
	fixture		INT		NOT NULL,
	opponent	INT		NOT NULL,
	points		INT		NOT NULL,
	round		INT		NOT NULL,
	minutes		INT		NOT NULL,
	PRIMARY KEY(id,fixture)
);