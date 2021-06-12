@echo off
for /f "tokens=2 delims==" %%a in ('wmic OS Get localdatetime /value') do set "dt=%%a"
if %dt:~6,2% LEQ 15 (
    CALL heroku dyno:kill web -a codebot122
    CALL heroku ps:scale web=0 -a codebot122
    CALL heroku dyno:kill web -a codebot123
    CALL heroku ps:scale web=1 -a codebot123
) else (
    CALL heroku dyno:kill web -a codebot123
    CALL heroku ps:scale web=0 -a codebot123
    CALL heroku dyno:kill web -a codebot122
    CALL heroku ps:scale web=1 -a codebot122
)
