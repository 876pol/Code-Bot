@echo off
CALL heroku dyno:kill web -a codebot123
CALL heroku ps:scale web=1 -a codebot123