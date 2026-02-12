@echo off
REM Custom Embedding App - Windows Stop Script

echo Stopping Custom Embedding App servers...
echo.

REM Kill processes on port 8080 (backend)
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :8080') do (
    taskkill /F /PID %%a >nul 2>&1
    if %ERRORLEVEL% EQU 0 (
        echo [OK] Backend stopped
    )
)

REM Kill processes on port 3000 (frontend)
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :3000') do (
    taskkill /F /PID %%a >nul 2>&1
    if %ERRORLEVEL% EQU 0 (
        echo [OK] Frontend stopped
    )
)

echo.
echo All servers stopped successfully
pause
