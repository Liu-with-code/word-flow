@echo off
rem ============================================================
rem WordFlow 数据库一键初始化
rem 默认使用 root / root，与项目 application.yml 保持一致；
rem 如果密码不同，请同时修改本文件与 backend application.yml。
rem 注意：本脚本会 DROP 并重建 wordflow 库，请谨慎执行。
rem ============================================================
set MYSQL_PWD=root
mysql -uroot --default-character-set=utf8mb4 -e "DROP DATABASE IF EXISTS wordflow;" 2>nul
mysql -uroot --default-character-set=utf8mb4 < "%~dp0schema.sql"
mysql -uroot --default-character-set=utf8mb4 < "%~dp0seed_words.sql"
echo [OK] wordflow database initialized.
pause

