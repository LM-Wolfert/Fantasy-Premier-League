CREATE TABLE IF NOT EXISTS players
(
	id		INT		NOT NULL,
	name		TEXT		NOT NULL,
	playChance_this	INT		NOT NULL,
	position	INT		NOT NULL,
	form		DOUBLE		NOT NULL,
	value_form	DOUBLE		NOT NULL,
	value_season	DOUBLE		NOT NULL,
	team_code	INT		NOT NULL,
	total_points	INT		NOT NULL,
	point_per_game	DOUBLE		NOT NULL,
	ep_this		DOUBLE		NOT NULL,
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
	last_update	BIGINT		NOT NULL,
	PRIMARY KEY(last_update)
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
	PRIMARY KEY(position)
);