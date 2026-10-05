-- The race stats each run was raced with (iteration 9: training and form).
-- The server replays the log with them; ghosts need them to replay exactly.
ALTER TABLE runs ADD COLUMN stat_speed INT NOT NULL DEFAULT 0;
ALTER TABLE runs ADD COLUMN stat_stamina INT NOT NULL DEFAULT 0;
ALTER TABLE runs ADD COLUMN stat_agility INT NOT NULL DEFAULT 0;
ALTER TABLE runs ADD COLUMN stat_jump INT NOT NULL DEFAULT 0;
