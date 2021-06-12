@echo off
for /f "tokens=2 delims==" %%a in ('wmic OS Get localdatetime /value') do set "dt=%%a"

cd "\Users\paulc\Code\Java\Code Bot"
CALL heroku ps:scale web=0 -a codebot123
CALL heroku ps:scale web=0 -a codebot122
CALL heroku container:push web -a codebot122
CALL heroku container:push web -a codebot123
CALL heroku container:release web -a codebot122
CALL heroku container:release web -a codebot123
cd "\Users\paulc\Code\Java\Code Bot\herokuAccountSwitch"

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
