[global]
repo-path={{ .Env.BACKUP_DIR }}

[[=DB_DATABASE]
pg1-path={{ .Env.DATA_DIR }}
pg1-socket-path=/rds/socket
