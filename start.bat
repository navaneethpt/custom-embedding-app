@echo off
REM Custom Embedding App - Windows Start Script

echo ========================================
echo Custom Domain-Specific Embedding System
echo ========================================
echo.

REM Check Java
where java >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Java not found. Please install Java 17 or higher
    pause
    exit /b 1
)
echo [OK] Java found

REM Check Maven
where mvn >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Maven not found. Please install Maven 3.6+
    pause
    exit /b 1
)
echo [OK] Maven found

REM Check Node
where node >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Node.js not found. Please install Node.js 16+
    pause
    exit /b 1
)
echo [OK] Node.js found
echo.

REM Setup Backend
echo ========================================
echo Setting Up Backend
echo ========================================
cd backend
call mvn clean install -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Backend setup failed
    pause
    exit /b 1
)
echo [OK] Backend setup complete
cd ..
echo.

REM Setup Frontend
echo ========================================
echo Setting Up Frontend
echo ========================================
cd frontend
call npm install
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Frontend setup failed
    pause
    exit /b 1
)
echo [OK] Frontend setup complete
cd ..
echo.

REM Start Backend
echo ========================================
echo Starting Backend Server
echo ========================================
cd backend
start "Backend Server" cmd /c "mvn spring-boot:run"
echo Backend starting on http://localhost:8080
cd ..
echo.

REM Wait a bit for backend to start
echo Waiting for backend to initialize...
timeout /t 10 /nobreak
echo.

REM Start Frontend
echo ========================================
echo Starting Frontend Server
echo ========================================
cd frontend
start "Frontend Server" cmd /c "npm start"
echo Frontend starting on http://localhost:3000
cd ..
echo.

echo ========================================
echo Setup Complete!
echo ========================================
echo.
echo Both servers are running:
echo   Backend:  http://localhost:8080
echo   Frontend: http://localhost:3000
echo.
echo The frontend will open in your browser automatically.
echo.
echo To stop the servers, close the command windows
echo or run: stop.bat
echo.
pause
