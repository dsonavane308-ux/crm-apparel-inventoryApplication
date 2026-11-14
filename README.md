# crm-apparel-inventoryApplication

Steps to Start Project Work : 
Start Docker Desktop: Launch Docker Desktop on Windows and wait until the system tray icon turns green and indicates the engine is running.

Start Jenkins Container: Open Git Bash and start your local Jenkins server using:
docker start jenkins
Verification: Open http://localhost:8081 in your browser to confirm the dashboard loads.

Start Application Containers (Docker Compose): Navigate to your project directory and spin up the local development stack using:
cd "/d/Project Work/Projects/crmApplication"
docker compose up -d
Verification: Run docker ps to verify that your app, UI, and database containers are listed as Up.

Start Ngrok (For Webhooks): Open a separate terminal window and start your tunnel if you are testing GitHub webhooks using:
ngrok http 8081
Verification: Ngrok displays an active public forwarding URL.

Steps to Close and Shutdown Laptop :
Stop Ngrok: Go to the terminal running ngrok and press Ctrl + C to shut down the public tunnel.Verification: The ngrok session terminates and returns to the command prompt.

Stop Application Stack: Navigate to your project directory and stop your Docker Compose containers to free up system resources using:
cd "/d/Project Work/Projects/crmApplication"
docker compose down
Verification: Run docker ps to ensure your project containers are no longer running.

Stop Jenkins Container: Stop Jenkins to conserve RAM while your laptop is off using:
docker stop jenkins
Verification: Run docker ps -a and confirm Jenkins shows an Exited status.

Quit Docker Desktop: Completely exit Docker Desktop from your Windows system tray.

Shutdown Laptop: Safely shut down or sleep your machine.