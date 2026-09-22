@echo off
setlocal
set MAVEN_OPTS=-Dmaven.repo.local=D:\Pogame Files(x86)\JavaCode\.m2-local
mvn %*
endlocal
