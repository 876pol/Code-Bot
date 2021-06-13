@echo off
cd "\Users\paulc\Code\Java\Code Bot"
CALL heroku ps:scale web=0 -a codebot123
CALL heroku container:push web -a codebot123
CALL heroku container:release web -a codebot123
cd "\Users\paulc\Code\Java\Code Bot\herokuAccountSwitch"
CALL heroku dyno:kill web -a codebot123
CALL heroku ps:scale web=1 -a codebot123