# README

This project is a drawing-based communication application using a machine learning model. It includes a JavaFX desktop app, a FastAPI backend, a PostgreSQL database, and a web frontend for mobile use.

## Run JavaFX (Desktop)

mvn clean javafx:run

## Run Backend (FastAPI)

- cd backend-ml
- .\.venv312\Scripts\Activate.ps1
- uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload

## Run Web Frontend

- cd web
- python -m http.server 5500

## Access on Mobile

1. Connect phone and laptop to the same network (recommended: use phone hotspot)

2. Find your IP address:

ipconfig

Look for your IPv4 Address (example: 172.20.10.2)

3. In web/app.js, set:

const API_BASE = "http://<YOUR-IP>:8000";

Example:

const API_BASE = "http://172.20.10.2:8000";

4. Open on your phone:

http://<YOUR-IP>:5500

5. Test backend connection:

http://<YOUR-IP>:8000/health

## Notes

- JavaFX uses: http://127.0.0.1:8000
- Web frontend uses your IP: http://<YOUR-IP>:8000
- Both use the same backend and PostgreSQL database